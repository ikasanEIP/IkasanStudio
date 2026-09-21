package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.NotNull;

public final class StudioAiImportProposalAction extends DumbAwareAction {
    /** A null path selects the most recently modified proposal in the project inbox. */
    static void openFile(Project project, java.nio.file.Path selected) {
        var inbox = project.getService(StudioAiProposalInboxService.class);
        String revision = inbox.pendingRevision();
        new Task.Backgroundable(project, StudioBundle.message("ai.ImportChecking"), false) {
            @Override public void run(@NotNull ProgressIndicator indicator) {
                try {
                    if (project.isDisposed()) return;
                    String basePath = project.getBasePath();
                    if (selected == null && basePath == null) throw new IllegalStateException(StudioBundle.message("ai.NotReady"));
                    var path = selected != null ? selected : StudioAiProposalInbox.latest(StudioAiProposalInbox.scan(
                            java.nio.file.Path.of(basePath, "ai-proposals")))
                            .orElseThrow(() -> new IllegalArgumentException(StudioBundle.message("ai.InboxEmpty")));
                    byte[] bytes;
                    try (var input = java.nio.file.Files.newInputStream(path)) { bytes = input.readNBytes(1_048_577); }
                    if (bytes.length > 1_048_576) throw new IllegalArgumentException(StudioBundle.message("ai.InboxTooLarge"));
                    if (!project.isDisposed()) {
                        project.getService(StudioAiService.class)
                                .importProposal(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), path);
                        ApplicationManager.getApplication().invokeLater(() -> inbox.dismiss(path, revision));
                    }
                } catch (ProcessCanceledException cancelled) { throw cancelled; }
                catch (Exception failure) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed()) showFailure(project, failure.getMessage());
                    });
                }
            }
        }.queue();
    }

    static void showFailure(Project project, String reason) {
        String feedback = StudioBundle.message("ai.ImportFailed", reason) + "\n\n"
                + StudioBundle.message("ai.ImportRecovery");
        int choice = com.intellij.openapi.ui.Messages.showDialog(project, feedback,
                StudioBundle.message("ai.ImportTitle"),
                new String[]{StudioBundle.message("ai.CopyFeedback"), StudioBundle.message("ai.FeedbackClose")},
                0, com.intellij.openapi.ui.Messages.getWarningIcon());
        if (choice == 0) com.intellij.openapi.ide.CopyPasteManager.getInstance()
                .setContents(new java.awt.datatransfer.StringSelection(feedback));
    }

    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null); }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) open(event.getProject());
    }

    static void open(Project project) {
        var chooser = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter(file -> "json".equalsIgnoreCase(file.getExtension()))
                .withTitle(StudioBundle.message("ai.ImportTitle"))
                .withDescription(StudioBundle.message("ai.ImportDescription"));
        StudioProjectFiles.chooseFileAndReadText(project, chooser, result -> {
            if (result.errorMessage() != null) {
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("ai.ImportFailed", result.errorMessage()));
                return;
            }
            new Task.Backgroundable(project, StudioBundle.message("ai.ImportChecking"), false) {
                @Override public void run(@NotNull ProgressIndicator indicator) {
                    try { project.getService(StudioAiService.class).importProposal(result.content(), java.nio.file.Path.of(result.path())); }
                    catch (ProcessCanceledException cancelled) { throw cancelled; }
                    catch (Exception failure) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!project.isDisposed()) showFailure(project, failure.getMessage());
                        });
                    }
                }
            }.queue();
        });
    }
}
