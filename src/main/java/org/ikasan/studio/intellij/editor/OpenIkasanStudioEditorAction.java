package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class OpenIkasanStudioEditorAction extends DumbAwareAction {
    // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
    // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
    // uncaught plugin exception rather than failing gracefully.
    @SuppressWarnings("NullableProblems")
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null && !project.isDisposed()) {
            project.getService(IkasanStudioEditorService.class).open();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabled(project != null && !project.isDisposed());
    }
}
