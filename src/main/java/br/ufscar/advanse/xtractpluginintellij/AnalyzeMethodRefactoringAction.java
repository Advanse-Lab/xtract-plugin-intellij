package br.ufscar.advanse.xtractpluginintellij;

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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

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

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest
                        .newBuilder(URI.create(baseUrl + "/health"))
                        .GET()
                        .setHeader("Content-Type", "application/json")
                        .build();

                HttpResponse<String> apiResponse;
                try {
                    apiResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                int statusCode = apiResponse.statusCode();
                String responseBody = apiResponse.body();

                System.out.println("HTTP status = " + statusCode);
                System.out.println("apiResponse = " + apiResponse);
                System.out.println("responseBody = " + responseBody);
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
