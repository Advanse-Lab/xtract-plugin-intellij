package br.ufscar.advanse.xtractpluginintellij;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

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

        String selected_text = selectionModel.getSelectedText();

        // Remove selection from code
        selectionModel.removeSelection();

        System.out.println("startPosition = " + startPosition);
        System.out.println("endPosition = " + endPosition);
        System.out.println("startPositionLine = " + startPositionLine);
        System.out.println("startPositionColumn = " + startPositionColumn);
        System.out.println("endPositionColumn = " + endPositionColumn);
        System.out.println("endPositionLine = " + endPositionLine);

        System.out.println("begin selected_text");
        System.out.println(selected_text);
        System.out.println("end selected_text");

        new Task.Backgroundable(project, "Calling API", true) {

            String response;
            String responseBody;
            XtractApiOutput responseData;

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
                        selected_text,
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

                    // Calling the refactoring method API
                    System.out.println("Calling the Refactoring Recommendation API");

                    HttpRequest.BodyPublisher methodAnalysisRequestBody = HttpRequest.BodyPublishers.ofString(
                            requestDataJson
                    );

                    HttpRequest methodAnalysisRequest = HttpRequest
                            .newBuilder(URI.create(baseUrl + "/time4"))
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

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSuccess() {
                Messages.showMessageDialog(anActionEvent.getProject(), "API called successfully!", "Extract Method", Messages.getInformationIcon());
            }

            @Override
            public void onCancel() {
                // Called if the user clicks the cancel button on the progress bar
                System.out.println("Operation cancelled by user.");
            }
        }.queue();
    }
}
