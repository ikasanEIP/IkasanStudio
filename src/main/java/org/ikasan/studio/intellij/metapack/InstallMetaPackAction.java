package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.core.metapack.loading.MetaPackInstaller;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;

/** Explicit local installation; validation and copying run outside the EDT. */
public final class InstallMetaPackAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(InstallMetaPackAction.class);
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        if (project == null || project.isDisposed()) return;
        var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("jar");
        descriptor.setTitle(StudioBundle.message("metaPack.ChooseJar"));
        descriptor.setDescription(StudioBundle.message("metaPack.InstallDescription"));
        FileChooser.chooseFile(descriptor, project, null, chosen -> {
            var source = Path.of(chosen.getPath());
            ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("metaPack.Progress"), true) {
                private MetaPackManifest installed;
                @Override public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        installed = MetaPackInstaller.install(source,
                                new StudioMetaPackDirectoryProvider().directory(), indicator::checkCanceled);
                    } catch (ProcessCanceledException cancelled) {
                        throw cancelled;
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure.getMessage(), failure);
                    }
                }
                @Override public void onSuccess() {
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaInfoMessage(project,
                            StudioBundle.message("metaPack.Installed", installed.id(), installed.packVersion()));
                }
                @Override public void onThrowable(@NotNull Throwable failure) {
                    LOG.warn("Meta-pack installation failed", failure);
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("metaPack.Failed", failure.getMessage()));
                }
            });
        });
    }
}
