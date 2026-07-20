package br.ufscar.advanse.xtractpluginintellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class AnalyzeMethodRefactoringAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        String messagePopUp = "Plugin called successfully!";

        Messages.showMessageDialog(anActionEvent.getProject(), messagePopUp, "Extract Method", Messages.getInformationIcon());

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
    }
}
