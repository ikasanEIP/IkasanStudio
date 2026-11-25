package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.NotNull;

/** Exports the saved project's required pack when configured; generic exports remain available. */
public final class ExportOfflineToolsAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ExportOfflineToolsAction.class);
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject()!=null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        Project project=event.getProject();
        if (project==null || project.isDisposed()) return;
        FileChooserDescriptor descriptor=FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(StudioBundle.message("offlineTools.ChooseDirectory"));
        descriptor.setDescription(StudioBundle.message("offlineTools.DirectoryDescription"));
        FileChooser.chooseFile(descriptor, project, null, chosen -> {
            Path parent=Path.of(chosen.getPath());
            ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("offlineTools.Progress"), true) {
                private Path exported;
                @Override public void run(@NotNull ProgressIndicator indicator) {
                    try (InputStream input=ExportOfflineToolsAction.class.getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
                        if (input==null) throw new IOException("Bundled offline tools archive is missing");
                        Path model = ReadAction.nonBlocking(() ->
                                StudioProjectFiles.getModelJsonPath(project))
                                .expireWith(project.getService(StudioProjectInitialisationService.class)).executeSynchronously();
                        String savedModel = model != null && Files.isRegularFile(model) ? Files.readString(model) : null;
                        exported=OfflineToolsExporter.export(input, parent, savedModel, indicator::checkCanceled);
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
