package br.ufscar.advanse.xtractpluginintellij;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static br.ufscar.advanse.xtractpluginintellij.SliceAnalyzer.fragmentMetricExtractor;

public class AnalyzeMethodRefactoringAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        // Get required data keys
        Project project = anActionEvent.getProject();
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);

        if (project == null | editor == null) return;

        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();

        // Set visibility only in the case of
        // existing project editor, and selection
        anActionEvent.getPresentation().setEnabledAndVisible(
                selectionModel.hasSelection()
        );

        int startPosition = selectionModel.getSelectionStart();
        int endPosition = selectionModel.getSelectionEnd();

        if (selectionModel.getSelectionStartPosition() == null | selectionModel.getSelectionEndPosition() == null) {
            return;
        }

        int startPositionLine = selectionModel.getSelectionStartPosition().getLine();
        int startPositionColumn = selectionModel.getSelectionStartPosition().column;
        int endPositionColumn = selectionModel.getSelectionEndPosition().column;
        int endPositionLine = selectionModel.getSelectionEndPosition().line;

        String selectedCode = selectionModel.getSelectedText();

        // Remove selection from code
        selectionModel.removeSelection();

        System.out.println("startPosition = " + startPosition);
        System.out.println("endPosition = " + endPosition);
        System.out.println("startPositionLine = " + startPositionLine);
        System.out.println("startPositionColumn = " + startPositionColumn);
        System.out.println("endPositionColumn = " + endPositionColumn);
        System.out.println("endPositionLine = " + endPositionLine);

        System.out.println("begin selectedCode");
        System.out.println(selectedCode);
        System.out.println("end selectedCode");

        new Task.Backgroundable(project, "Calling API", true) {

            private String recommendationExplanationResponseBody;
            private String response;
            private String responseBody;
            private XtractApiOutput responseData;
            private XtractApiOutput recommendationExplanationResponseData;
            private boolean refactoringOportunity = true;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                Properties properties = new Properties();

                try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                    if (input == null) {
                        throw new FileNotFoundException("config.properties not found");
                    }
                    properties.load(input);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String baseUrl = properties.getProperty("XTRACT_BASE_URL", "http://localhost:8000");
                System.out.println("baseUrl = " + baseUrl);

                // Extracting metrics from fragments of method
                String projectPath = project.getBasePath();

                String pluginDirectoryPath = projectPath + "/plugin_data";
                final File pluginDirectory = new File(pluginDirectoryPath);

                pluginDirectory.mkdirs();

                String simplifiedSlicesFileName = pluginDirectoryPath + "/simplified_slices.json";
                String methodMetricsFileName = pluginDirectoryPath + "/method_metrics.csv";

                long startTime = System.nanoTime();
                // FME call
                fragmentMetricExtractor(
                        selectedCode,
                        simplifiedSlicesFileName,
                        methodMetricsFileName
                );
                long endTime = System.nanoTime();
                System.out.printf(">>>>> Elapsed time of %.5f seconds.\n", (endTime - startTime) * 1e-9);

                // Encode simplified slices file into base64
                BufferedReader br;
                try {
                    br = new BufferedReader(new FileReader(simplifiedSlicesFileName));
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                List<SliceAnalyzer.SimpleSlice> sample = new Gson().fromJson(br, List.class);

                Type listType = new TypeToken<List<SliceAnalyzer.SimpleSlice>>() {}.getType();
                String simplifiedSlicesString = new Gson().toJson(sample, listType);
                String simplifiedSlicesStringBase64Encoded = Base64.getEncoder().encodeToString(
                        simplifiedSlicesString.getBytes(StandardCharsets.UTF_8)
                );

                // Encode metrics file into base64
                Path methodMetricsPath = Paths.get(methodMetricsFileName);
                String methodMetricsString;
                String methodMetricsStringBase64Encoded;
                try {
                    methodMetricsString = Files.readString(methodMetricsPath, StandardCharsets.UTF_8);
                    methodMetricsStringBase64Encoded = Base64.getEncoder().encodeToString(
                            methodMetricsString.getBytes(StandardCharsets.UTF_8)
                    );
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                progressIndicator.setText("Calling the Refactoring Recommendation API");
                progressIndicator.setIndeterminate(true);

                try {
                    XtractApiInput requestData = new XtractApiInput(
                            simplifiedSlicesStringBase64Encoded,
                            methodMetricsStringBase64Encoded
                    );
                    String requestDataJson = new Gson().toJson(requestData);

                    // First API call, identify refactoring is needed and which code fragment should receive Extract Method
                    System.out.println("Calling the Refactoring Recommendation API");

                    HttpRequest.BodyPublisher methodAnalysisRequestBody = HttpRequest.BodyPublishers.ofString(
                            requestDataJson
                    );

                    HttpRequest methodAnalysisRequest = HttpRequest
                            .newBuilder(URI.create(baseUrl + "/evaluation"))
                            .POST(methodAnalysisRequestBody)
                            .setHeader("Content-Type", "application/json")
                            .build();

                    HttpClient httpClient = HttpClient.newHttpClient();
                    HttpResponse<String> apiResponse = httpClient.send(methodAnalysisRequest, HttpResponse.BodyHandlers.ofString());

                    // Treating API's response
                    int statusCode = apiResponse.statusCode();
                    responseBody = apiResponse.body();
                    responseData = new Gson().fromJson(responseBody, XtractApiOutput.class);

                    System.out.println("HTTP status: " + statusCode);
                    System.out.println(responseBody);
                    System.out.println(responseData.getForestPrediction());

                    System.out.println("Refactoring Recommendation API called successfully");

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed() || editor.isDisposed()) return;

                        // PopUp menu that informs user if refactoring of method is needed
                        Messages.showMessageDialog(anActionEvent.getProject(), getMessagePopUp(), "Extract Method", Messages.getInformationIcon());

                        System.out.println("responseData.isForestPredictionValid(): " + responseData.isForestPredictionValid());
                        System.out.println("responseData.getForestPrediction(): " + responseData.getForestPrediction());

                        if (!responseData.isForestPredictionValid() || responseData.getForestPrediction() <= 0.55) {
                            refactoringOportunity = false;
                            return;
                        }

                        System.out.println("Method is a candidate for refactoring.");

                        MarkupModel markupModel = editor.getMarkupModel();

                        TextAttributes attributes = new TextAttributes();
                        attributes.setBackgroundColor(new JBColor(
                                new Color(255, 255, 0, 100),
                                new Color(255, 255, 0, 50)
                        ));

                        int lineStartOffset = document.getLineStartOffset(startPositionLine + responseData.getBeginLine() + 2);
                        int startPositionOffset = lineStartOffset + responseData.getBeginColumn() - 1;

                        int lineEndOffset = document.getLineStartOffset(startPositionLine + responseData.getEndLine() + 2);
                        int endPositionOffset = lineEndOffset + responseData.getEndColumn();

                        System.out.println("startPosition = " + startPosition);
                        System.out.println("endPosition = " + endPosition);

                        System.out.println("startPositionLine = " + startPositionLine);
                        System.out.println("endPositionLine = " + endPositionLine);

                        System.out.println("responseData.getBeginLine() = " + responseData.getBeginLine());
                        System.out.println("responseData.getBeginColumn() = " + responseData.getBeginColumn());
                        System.out.println("responseData.getEndLine() = " + responseData.getEndLine());
                        System.out.println("responseData.getEndColumn() = " + responseData.getEndColumn());

                        System.out.println("lineStartOffset = " + lineStartOffset);
                        System.out.println("startPositionOffset = " + startPositionOffset);
                        System.out.println("lineEndOffset = " + lineEndOffset);
                        System.out.println("endPositionOffset = " + endPositionOffset);

//                        for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
//                            if (Boolean.TRUE.equals(highlighter.getUserData(RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY))) {
//                                markupModel.removeHighlighter(highlighter);
//                            }
//                        }
                        markupModel.removeAllHighlighters();

                        RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
                                startPositionOffset,
                                endPositionOffset,
                                HighlighterLayer.SELECTION - 1,
                                attributes,
                                HighlighterTargetArea.EXACT_RANGE
                        );
                        rangeHighlighter.putUserData(RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY, true);

                        CaretModel caretModel = editor.getCaretModel();
                        ScrollingModel scrollingModel = editor.getScrollingModel();

                        int targetLine = startPositionLine + responseData.getEndLine() + 2;
                        int targetColumn = responseData.getEndColumn();
                        LogicalPosition newPosition = new LogicalPosition(targetLine, targetColumn);

                        // Move cursor at the end of the highlighted code
                        caretModel.moveToLogicalPosition(newPosition);

                        // Focus the screen where the cursor is located
                        scrollingModel.scrollToCaret(ScrollType.CENTER_DOWN);
                    });

                    progressIndicator.setText("Calling the Explanation Recommendation API");
                    progressIndicator.setIndeterminate(true);

                    // Second API call, gets the explanation for the selected code for refactoring
                    System.out.println("Calling the Explanation Recommendation API");

                    XtractApiExplanationInput explanationInputData = new XtractApiExplanationInput(
                            responseData.getSourceCodeMethod(),
                            responseData.getFragment(),
                            responseData.getScore(),
                            responseData.getProbability(),
                            responseData.getRankingExplanation()
                    );

                    System.out.println(responseData.getRankingExplanation());
                    System.out.println(explanationInputData);

                    String explanationInputDataJson = new Gson().toJson(explanationInputData);

                    HttpRequest.BodyPublisher explanationRequestBody = HttpRequest.BodyPublishers.ofString(
                            explanationInputDataJson
                    );

                    HttpRequest methodExplanationRequest = HttpRequest
                            .newBuilder(URI.create(baseUrl + "/explanation"))
                            .POST(explanationRequestBody)
                            .setHeader("Content-Type", "application/json")
                            .build();

                    HttpResponse<String> recommendationExplanationResponse = httpClient.send(methodExplanationRequest, HttpResponse.BodyHandlers.ofString());

                    // Treating API's response
                    int methodExplanationStatusCode = recommendationExplanationResponse.statusCode();

                    recommendationExplanationResponseBody = recommendationExplanationResponse.body();
                    recommendationExplanationResponseData = new Gson().fromJson(recommendationExplanationResponseBody, XtractApiOutput.class);

                    System.out.println("HTTP status: " + methodExplanationStatusCode);
                    System.out.println("recommendationExplanationResponseBody: " + recommendationExplanationResponseBody);

                } catch (IOException ex) {
                    // Code run in case of error,
                    // treat here or save and show in UI later
                    response = "Error in request: " + ex.getMessage();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            private String getMessagePopUp() {
                if (!responseData.isForestPredictionValid()) return "The model identified that Extract Method is not necessary.";

                if (responseData.getForestPrediction() <= 0.55) return "The model identified that Extract Method is not necessary.";

                Float modelPrediction = responseData.getForestPrediction();
                return "The analyzed method was classified with a " + String.format("%.2f", modelPrediction * 100) + "% probability of requiring refactoring.";
            }

            @Override
            public void onSuccess() {
                System.out.println("Explanation Recommendation API called successfully");

                ToolWindow myToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Refactoring Recommendation");
                if (myToolWindow != null) {
                    // Activate the tool window if it is not visible
                    myToolWindow.activate(null);

                    // Access the first content item (tab) and interact with it.
                    Content content = myToolWindow.getContentManager().getContent(0);
                    if (content != null) {
                        System.out.println("refactoringOportunity: " + refactoringOportunity);

                        if (!refactoringOportunity) return;

                        System.out.println("RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY: " + RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY);
                        System.out.println("RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY: " + RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY);

                        // Retrieves the reference directly via UserData.
                        RefactoringRecommendationToolWindowFactory.HtmlPanel panel = content.getUserData(RefactoringRecommendationToolWindowFactory.REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY);

                        if (panel != null) {
                            String newContent = recommendationExplanationResponseData.getExplicacao();
                            panel.updateContent(newContent);
                        } else {
                            System.out.println("RefactoringRecommendationToolWindowFactory.HtmlPanel is null");
                        }
                    }
                }

                System.out.println("API called successfully!");
            }

            @Override
            public void onCancel() {
                // Called if the user clicks the cancel button on the progress bar
                System.out.println("Operation cancelled by user.");
            }
        }.queue();
    }
}
