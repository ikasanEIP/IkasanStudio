package org.ikasan.studio.intellij.diagnostics;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.intellij.project.RuntimeImportReportDialog;
import org.jetbrains.annotations.NotNull;

public final class ShowRuntimeImportReportAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null && !event.getProject().isDisposed())
            new RuntimeImportReportDialog(event.getProject(), null).show();
    }
}
