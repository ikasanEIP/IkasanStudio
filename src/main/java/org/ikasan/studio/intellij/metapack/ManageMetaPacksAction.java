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
        if (event.getProject() == null || event.getProject().isDisposed()) return;
        var dialog = new MetaPackManagerDialog(event.getProject());
        dialog.reload();
        dialog.show();
    }
}
