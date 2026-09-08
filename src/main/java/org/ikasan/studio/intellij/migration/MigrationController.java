package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/** Modal review keeps canvas edits out of a background migration; file preconditions cover external edits. */
public final class MigrationController {
    private static final Logger LOG = Logger.getInstance(MigrationController.class);
    private MigrationController() { }
    private record Preview(String report, List<MigrationWorkspace.Change> changes, Module module, boolean canApply) { }

    public static void open(Project project, boolean restore) {
        if (project.isDisposed()) return;
        UiContext context = project.getService(UiContext.class);
        String title = restore ? StudioBundle.message("dialog.RestorePreviousIkasanMigration") : StudioBundle.message("dialog.MigrateIkasanVersion");
        boolean acquired = false;
        Path appliedSnapshot = null;
        try {
            if (project.getBasePath() == null) throw new IllegalStateException(StudioBundle.message("message.OpenAnIkasanProjectFirst"));
            if (DumbService.isDumb(project)) throw new IllegalStateException(StudioBundle.message("message.WaitForProjectIndexingToFinishBeforeMigrating"));
            if (!restore && context.isModelPersistenceBlocked()) throw new IllegalStateException(StudioBundle.message("message.LoadAValidStudioModelBeforeMigrating"));
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess()) {
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            }
            checkUnsaved(project);
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            Path root = Path.of(project.getBasePath());
            String target = null;
            if (!restore) {
                Module live = context.getIkasanModule();
                if (live == null || !live.isInitialised()) throw new IllegalStateException(StudioBundle.message("message.ConfigureAndSaveTheModuleFirst"));
                String current = live.getMetaVersion();
                if (!ModelMigration.SUPPORTED_VERSIONS.contains(current)) throw new IllegalStateException(StudioBundle.message("message.SupportedMigrationPathsAreV339V416"));
                String[] choices = ModelMigration.SUPPORTED_VERSIONS.stream().filter(v -> !v.equals(current)).toArray(String[]::new);
                var chooser = new MigrationTargetDialog(project, current, choices);
                if (!chooser.showAndGet()) return;
                target = chooser.targetVersion();
            }
            String selectedTarget = target;
            Preview preview = background(project, "Preparing migration preview", () -> {
                if (restore) {
                    var snapshot = MigrationWorkspace.latest(root);
                    var changes = MigrationWorkspace.prepareRestore(root, snapshot);
                    String restored = changes.stream().filter(c -> c.path().equals(MigrationArtifacts.MODEL)).findFirst().orElseThrow().afterText();
                    Module module = ComponentIO.validatePersistedModuleJson(restored, "migration snapshot", false);
                    return new Preview(StudioBundle.message("message.RestoreSnapshotReport", snapshot.id(), snapshot.report()), changes, module, true);
                }
                String source = Files.readString(root.resolve(MigrationArtifacts.MODEL));
                Module persisted = ComponentIO.validatePersistedModuleJson(source, "migration source", false);
                if (!StudioJson.newObjectMapper().readTree(ComponentIO.toJson(persisted)).equals(
                        StudioJson.newObjectMapper().readTree(ComponentIO.toJson(context.getIkasanModule())))) {
                    throw new IllegalStateException(StudioBundle.message("message.TheCanvasAndSavedModelDiffer"));
                }
                var plan = ModelMigration.analyse(source, selectedTarget);
                if (!plan.canApply()) return new Preview(plan.report(), List.of(), null, false);
                String sourcePom = Files.readString(root.resolve("pom.xml"));
                var artifacts = MigrationArtifacts.render(plan, sourcePom);
                if (!Files.exists(root.resolve("AGENTS.md"))) artifacts.put("AGENTS.md", AiProjectContractGenerator.agentsGuide());
                var changes = MigrationWorkspace.prepare(root, artifacts);
                if (changes.stream().anyMatch(c -> c.path().equals("AGENTS.md") && c.before() != null)) {
                    throw new IllegalStateException(StudioBundle.message("message.ProjectInstructionsWereAddedWhilePreparingMigration"));
                }
                // Preparation must not accept a source file which changed while templates were rendering.
                if (!changes.stream().filter(c -> c.path().equals(MigrationArtifacts.MODEL)).findFirst().orElseThrow().beforeText().equals(source)) {
                    throw new IllegalStateException(StudioBundle.message("message.TheModelChangedWhilePreparingMigration"));
                }
                if (!changes.stream().filter(c -> c.path().equals("pom.xml")).findFirst().orElseThrow().beforeText().equals(sourcePom)) {
                    throw new IllegalStateException(StudioBundle.message("message.ThePomChangedWhilePreparingMigration"));
                }
                return new Preview(plan.report(), changes, ComponentIO.validatePersistedModuleJson(plan.targetJson(), "target", false), true);
            });
            if (project.isDisposed()) return;
            int requiredJava = preview.module() == null ? 0 : background(project, "Checking target Java version", () ->
                    Integer.parseInt(org.ikasan.studio.core.metapack.ComponentLibrary
                            .getMetaPackManifest(preview.module().getMetaVersion()).javaVersion()));
            var dialog = new MigrationPreviewDialog(project, title, preview.report(), preview.changes(), preview.canApply(), requiredJava);
            if (!dialog.showAndGet()) return;
            if (project.isDisposed()) return;
            checkUnsaved(project);
            var selectedJdk = dialog.selectedJdk();
            Path snapshot = background(project, "Applying Ikasan migration", () -> {
                if (selectedJdk == null || selectedJdk.getHomePath() == null
                        || !com.intellij.openapi.projectRoots.JavaSdk.getInstance().isValidSdkHome(selectedJdk.getHomePath())) {
                    throw new IllegalStateException(StudioBundle.message("message.TheSelectedJdkIsUnavailable", requiredJava));
                }
                return MigrationWorkspace.commit(root, preview.changes(), preview.report());
            });
            appliedSnapshot = snapshot;
            MigrationJdk.apply(project, dialog.selectedJdk(), requiredJava);
            context.setIkasanModule(preview.module());
            background(project, "Refreshing migrated project", () -> {
                var base = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
                if (base != null) base.refresh(false, true);
                return null;
            });
            context.setIkasanPomModel(null);
            context.setApplicationProperties(null);
            context.allowModelPersistence();
            if (context.getViewHandlerFactory() != null) context.getViewHandlerFactory().clear();
            context.resetSelectionAfterDeletion();
            if (context.getPalettePanel() != null) context.getPalettePanel().resetPallette();
            if (context.getDesignerCanvas() != null) {
                context.getDesignerCanvas().setInitialiseAllDimensions(true);
                context.getDesignerCanvas().repaint();
            }
            if (context.getCanvasPanel() != null) context.getCanvasPanel().disableH2Button(preview.module().getUseEmbeddedH2());
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (!project.isDisposed() && context.getIkasanModule() == preview.module()) {
                    try { new GeneratedProjectSynchronizer(project).initialisePsiFileHandles(); }
                    catch (RuntimeException ex) { LOG.warn("Could not refresh migration navigation targets", ex); }
                }
            });
            var maven = MavenProjectsManager.getInstance(project);
            if (maven != null && dialog.shouldCompile()) {
                maven.forceUpdateProjects(maven.getProjects()).onSuccess(ignored ->
                    maven.scheduleImportAndResolve().onSuccess(modules ->
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed()) return;
                            DumbService.getInstance(project).smartInvokeLater(() -> {
                                if (context.getIkasanModule() != preview.module()) return;
                                CompilerManager.getInstance(project).make((aborted, errors, warnings, compileContext) -> {
                                    String result = aborted ? StudioBundle.message("message.BuildCancelled")
                                            : errors == 0 ? StudioBundle.message("message.BuildPassed")
                                            : StudioBundle.message("message.BuildFailedWithNErrors", errors);
                                    com.intellij.notification.NotificationGroupManager.getInstance().getNotificationGroup("Ikasan Studio")
                                            .createNotification(StudioBundle.message("message.IkasanMigrationResult", result),
                                                    StudioBundle.message("message.RecoverySnapshotRunApplicationTests", snapshot),
                                                    errors == 0 && !aborted ? com.intellij.notification.NotificationType.INFORMATION : com.intellij.notification.NotificationType.WARNING)
                                            .notify(project);
                                });
                            });
                        })).onError(error -> notifyImportFailure(project, snapshot))
                ).onError(error -> notifyImportFailure(project, snapshot));
            } else if (maven != null) maven.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            Messages.showInfoMessage(project, StudioBundle.message("message.MigrationApplied", snapshot)
                    + (dialog.shouldCompile() ? StudioBundle.message("message.ABuildWillRunAfterMavenImport") : ""), title);
        } catch (Exception failure) {
            LOG.warn("Ikasan migration could not complete", failure);
            if (failure.getMessage() != null && failure.getMessage().contains("Automatic restoration was incomplete")) {
                context.blockModelPersistence(failure.getMessage());
            }
            if (project.isDisposed()) return;
            String detail = failure.getMessage() == null ? failure.toString() : failure.getMessage();
            if (appliedSnapshot != null) detail = StudioBundle.message("message.TheFilesWereMigratedButAFollowUpStepFailed", detail, appliedSnapshot);
            Messages.showWarningDialog(project, detail, title);
        } finally {
            if (acquired) context.endMigration();
        }
    }

    private static void notifyImportFailure(Project project, Path snapshot) {
        if (project.isDisposed()) return;
        com.intellij.notification.NotificationGroupManager.getInstance().getNotificationGroup("Ikasan Studio")
                .createNotification(StudioBundle.message("message.IkasanMigrationMavenImportFailed"),
                        StudioBundle.message("message.FilesWereMigratedButCompilationCouldNotStart", snapshot),
                        com.intellij.notification.NotificationType.WARNING).notify(project);
    }

    private static void checkUnsaved(Project project) {
        var manager = FileDocumentManager.getInstance();
        for (var document : manager.getUnsavedDocuments()) {
            var file = manager.getFile(document);
            if (file != null && file.getPath().startsWith(project.getBasePath() + "/")) {
                throw new IllegalStateException(StudioBundle.message("message.SaveTheProjectsOpenFilesBeforeMigration", file.getName()));
            }
        }
    }

    private static <T> T background(Project project, String title, Callable<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.call()); } catch (Exception ex) { failure.set(ex); }
        }, title, false, project);
        if (failure.get() != null) throw failure.get();
        return result.get();
    }
}
