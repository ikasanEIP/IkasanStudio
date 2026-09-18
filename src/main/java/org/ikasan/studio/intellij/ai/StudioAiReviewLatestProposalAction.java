package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public final class StudioAiReviewLatestProposalAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && event.getProject().getBasePath() != null);
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) StudioAiImportProposalAction.openFile(event.getProject(), null);
    }
}
