package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.ui.UiContext;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;

public final class MigrateIkasanVersionAction extends DumbAwareAction {
    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) {
        Project project = event.getProject();
        Module module = project == null ? null : project.getService(UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(module != null && module.isInitialised());
    }
    @Override public void actionPerformed(AnActionEvent event) {
        if (event.getProject() != null) MigrationController.open(event.getProject(), false);
    }
}
