package org.ikasan.studio.intellij.project;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.ikasan.studio.core.persistence.json.IkasanModelExporter;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/** Exports the live designer model under modal progress so it cannot change during serialization. */
public final class ExportIkasanModelAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ExportIkasanModelAction.class);

    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }

    @Override public void update(@NotNull AnActionEvent event) {
        var project = event.getProject();
        var module = project == null || project.isDisposed() ? null : project.getService(UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(module != null && module.isInitialised());
    }

    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        if (project == null || project.isDisposed()) return;
        var descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(StudioBundle.message("modelExport.ChooseDirectory"));
        descriptor.setDescription(StudioBundle.message("modelExport.DirectoryDescription"));
        FileChooser.chooseFile(descriptor, project, null, chosen -> {
            if (project.isDisposed()) return;
            var context = project.getService(UiContext.class);
            if (!context.tryBeginMigration()) {
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
                return;
            }
            try {
                var module = context.getIkasanModule();
                if (module == null || !module.isInitialised()) return;
                Path parent = Path.of(chosen.getPath());
                ProgressManager.getInstance().run(new Task.Modal(project, StudioBundle.message("modelExport.Progress"), true) {
                    private Path exported;
                    @Override public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            exported = IkasanModelExporter.export(module, parent, indicator::checkCanceled);
                        } catch (IOException failure) {
                            throw new IllegalStateException(failure.getMessage(), failure);
                        }
                    }
                    @Override public void onSuccess() {
                        if (!project.isDisposed()) StudioUIUtils.displayIdeaInfoMessage(project,
                                StudioBundle.message("modelExport.Exported", exported.toString()));
                    }
                    @Override public void onThrowable(@NotNull Throwable failure) {
                        LOG.warn("Ikasan model document export failed", failure);
                        if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("modelExport.Failed", failure.getMessage()));
                    }
                });
            } finally {
                context.endMigration();
            }
        });
    }
}
