package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;

public final class RestoreIkasanMigrationAction extends DumbAwareAction {
    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null); }
    @Override public void actionPerformed(AnActionEvent event) {
        if (event.getProject() != null) MigrationController.open(event.getProject(), true);
    }
}
