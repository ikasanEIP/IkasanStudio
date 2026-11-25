package org.ikasan.studio.intellij.metapack;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;

public final class ManageMetaPacksAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(AnActionEvent event) {
        open(event.getProject());
    }

    /** Opens the same manager from the menu or a notification action, on the EDT. */
    public static void open(com.intellij.openapi.project.Project project) {
        if (project == null || project.isDisposed()) return;
        MetaPackManagerDialog dialog = new MetaPackManagerDialog(project);
        dialog.reload();
        dialog.show();
    }
}
