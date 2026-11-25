package org.ikasan.studio.intellij.configuration;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;

public final class ImportEnvironmentPropertiesAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }

    @Override public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabled(project != null && !project.isDisposed() && project.getBasePath() != null);
    }

    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null && !project.isDisposed() && project.getBasePath() != null)
            new ImportEnvironmentPropertiesDialog(project).show();
    }
}
