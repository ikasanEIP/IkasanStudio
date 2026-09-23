package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Path;

/** Local-only export; does not require an initialized Studio module or network access. */
public final class ExportOfflineToolsAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ExportOfflineToolsAction.class);
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject()!=null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        var project=event.getProject();
        if (project==null || project.isDisposed()) return;
        var descriptor=FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(StudioBundle.message("offlineTools.ChooseDirectory"));
        descriptor.setDescription(StudioBundle.message("offlineTools.DirectoryDescription"));
        FileChooser.chooseFile(descriptor, project, null, chosen -> {
            Path parent=Path.of(chosen.getPath());
            ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("offlineTools.Progress"), true) {
                private Path exported;
                @Override public void run(@NotNull ProgressIndicator indicator) {
                    try (var input=ExportOfflineToolsAction.class.getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
                        if (input==null) throw new IOException("Bundled offline tools archive is missing");
                        exported=OfflineToolsExporter.export(input, parent, indicator::checkCanceled);
                    } catch (IOException failure) { throw new IllegalStateException(failure.getMessage(), failure); }
                }
                @Override public void onSuccess() {
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaInfoMessage(project,
                            StudioBundle.message("offlineTools.Exported", exported.toString()));
                }
                @Override public void onThrowable(@NotNull Throwable failure) {
                    LOG.warn("Offline migration tools export failed", failure);
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("offlineTools.Failed", failure.getMessage()));
                }
            });
        });
    }
}
