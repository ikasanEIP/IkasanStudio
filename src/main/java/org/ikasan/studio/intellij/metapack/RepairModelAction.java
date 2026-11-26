package org.ikasan.studio.intellij.metapack;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ModelPackRepair.Plan;
import org.ikasan.studio.core.metapack.ModelPackRepair;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

/** Review a validated model repair from Tools or the project recovery screen. */
public final class RepairModelAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed()); }
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        review(event.getProject());
    }

    public static void review(Project project) {
        if (project == null || project.isDisposed()) return;
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false;
        boolean applied = false;
        String title = StudioBundle.message("modelRepair.Title");
        try {
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            if (project.isDisposed()) return;
            if (!project.getService(IkasanDebugSessionService.class).isModuleStopped())
                throw new IllegalStateException(StudioBundle.message("modelRepair.StopModule"));
            AdoptMetaPackRevisionAction.checkUnsaved(project);
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess())
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            Path model = StudioProjectFiles.getModelJsonPath(project);
            String basePath = StudioProjectFiles.getApplicationBasePath(project);
            if (model == null || basePath == null) throw new IllegalStateException(StudioBundle.message("message.OpenAnIkasanProjectFirst"));
            Plan plan = MetaPackUiTasks.run(project, title, () -> {
                Plan candidate = ModelPackRepair.preview(Files.readString(model));
                Module restored = ComponentIO.validatePersistedModuleJson(
                        candidate.targetJson(), "repair rendering preview", false);
                ProjectArtifactRenderer.render(restored,
                        Files.readString(Path.of(basePath, StudioProjectFiles.POM_XML)));
                return candidate;
            });
            boolean revisionUpdate = plan.isRevisionUpdate();
            if (revisionUpdate) title = StudioBundle.message("modelUpdate.Title");
            String confirmation = revisionUpdate
                    ? StudioBundle.message("modelUpdate.Confirm", plan.targetPack(), plan.previousRevision(),
                            plan.targetRevision(), plan.targetRuntime(), plan.javaVersion())
                    : StudioBundle.message("modelRepair.Confirm", plan.previousPack(), plan.targetPack(),
                            plan.previousRevision(), plan.targetRevision(), plan.previousRuntime(),
                            plan.targetRuntime(), plan.javaVersion());
            if (Messages.showOkCancelDialog(project, confirmation,
                    title, StudioBundle.message(revisionUpdate ? "modelUpdate.Apply" : "modelRepair.Apply"), CommonBundle.getCancelButtonText(),
                    Messages.getQuestionIcon()) != Messages.OK) return;
            if (project.isDisposed()) return;
            if (!project.getService(IkasanDebugSessionService.class).isModuleStopped())
                throw new IllegalStateException(StudioBundle.message("modelRepair.StopModule"));
            AdoptMetaPackRevisionAction.checkUnsaved(project);
            MetaPackUiTasks.run(project, title, () -> { ModelPackRepair.apply(model, plan); return null; });
            applied = true;
            context.blockModelPersistence(StudioBundle.message("modelRepair.Reloading"));
            VirtualFile file = MetaPackUiTasks.run(project, title, () -> {
                VirtualFile refreshed = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(model);
                if (refreshed != null) refreshed.refresh(false, false);
                return refreshed;
            });
            if (file != null) {
                ModelDocumentReload.reload(project, file);
            }
            // Reopening starts the normal background model-loading lifecycle, including pack validation.
            project.getService(StudioProjectInitialisationService.class).reloadModel();
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message(revisionUpdate ? "modelUpdate.Applied" : "modelRepair.Applied", plan.targetPack(), plan.targetRevision()));
        } catch (ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception e) { Messages.showErrorDialog(project, StudioBundle.message(applied ? "modelRepair.PostApplyFailed" : "modelRepair.Failed", e.getMessage()), title); }
        finally { if (acquired) context.endMigration(); }
    }
}
