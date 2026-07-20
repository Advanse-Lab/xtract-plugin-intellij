package br.ufscar.advanse.xtractpluginintellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class AnalyzeMethodRefactoringAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        String messagePopUp = "Plugin called successfully!";

        Messages.showMessageDialog(anActionEvent.getProject(), messagePopUp, "Extract Method", Messages.getInformationIcon());
    }
}
