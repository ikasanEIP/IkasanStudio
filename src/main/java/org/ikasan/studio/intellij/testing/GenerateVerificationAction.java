package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/** Shares the existing project-generation integration boundary. */
public final class GenerateVerificationAction extends com.intellij.openapi.project.DumbAwareAction {
    @Override public com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
        return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
    }
    @Override public void update(@NotNull AnActionEvent event) {
        var project = event.getProject();
        var module = project == null ? null : project.getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(module != null && module.isInitialised());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) GenerateFlowTestAction.openVerification(event.getProject());
    }
}
