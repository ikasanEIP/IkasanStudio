package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

/** Shares the existing project-generation integration boundary. */
public final class GenerateVerificationAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    @Override public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Module module = project == null ? null : project.getService(UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(module != null && module.isInitialised());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) GenerateFlowTestAction.openVerification(event.getProject());
    }
}
