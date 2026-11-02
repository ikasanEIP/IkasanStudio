package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

/** Resume source import independently of runtime acquisition. */
public final class ImportSourceModuleAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        var project = event.getProject();
        var module = project == null || project.isDisposed() ? null : project.getService(UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(project != null && project.getBasePath() != null && module != null && module.isInitialised());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        if (project != null && !project.isDisposed()) new ImportSourceModuleDialog(project).show();
    }
}
