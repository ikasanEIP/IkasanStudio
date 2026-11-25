package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.ikasan.studio.core.metapack.MetaPackRevisionLock.Plan;
import org.ikasan.studio.core.metapack.MetaPackRevisionLock;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

/** Explicit recovery from a revision mismatch, with a reviewed model-only change and rotating backup. */
public final class AdoptMetaPackRevisionAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed()); }
    @Override public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) return;
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false;
        String title = StudioBundle.message("metaPack.Adopt");
        try {
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            checkUnsaved(project);
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess())
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            Path model = StudioProjectFiles.getModelJsonPath(project);
            if (model == null) throw new IllegalStateException(StudioBundle.message("message.OpenAnIkasanProjectFirst"));
            Plan plan = MetaPackUiTasks.run(project, title, () -> MetaPackRevisionLock.preview(Files.readString(model)));
            if (Messages.showYesNoDialog(project, StudioBundle.message("metaPack.ConfirmAdopt", plan.packId(), plan.previousRevision(), plan.availableRevision()),
                    title, Messages.getQuestionIcon()) != Messages.YES) return;
            checkUnsaved(project);
            MetaPackUiTasks.run(project, title, () -> { MetaPackRevisionLock.apply(model, plan); return null; });
            VirtualFile file = MetaPackUiTasks.run(project, title, () -> {
                VirtualFile refreshed = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(model);
                if (refreshed != null) refreshed.refresh(false, false);
                return refreshed;
            });
            if (file != null) {
                ModelDocumentReload.reload(project, file);
            }
            context.allowModelPersistence();
            // Reopening starts the normal background model-loading lifecycle, including pack validation.
            project.getService(StudioProjectInitialisationService.class).reloadModel();
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("metaPack.Adopted", plan.packId(), plan.availableRevision()));
        } catch (ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception e) { Messages.showErrorDialog(project, StudioBundle.message("metaPack.Failed", e.getMessage()), title); }
        finally { if (acquired) context.endMigration(); }
    }
    static void checkUnsaved(Project project) {
        for (Document document : FileDocumentManager.getInstance().getUnsavedDocuments()) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file != null && project.getBasePath() != null && file.getPath().startsWith(project.getBasePath() + "/"))
                throw new IllegalStateException(StudioBundle.message("message.SaveTheProjectsOpenFilesBeforeMigration", file.getName()));
        }
    }
}
