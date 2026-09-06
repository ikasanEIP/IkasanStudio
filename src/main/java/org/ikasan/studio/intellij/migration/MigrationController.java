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
        String title = restore ? "Restore previous Ikasan migration" : "Migrate Ikasan version";
        boolean acquired = false;
        Path appliedSnapshot = null;
        try {
            if (project.getBasePath() == null) throw new IllegalStateException("Open an Ikasan project first.");
            if (DumbService.isDumb(project)) throw new IllegalStateException("Wait for project indexing to finish before migrating.");
            if (!restore && context.isModelPersistenceBlocked()) throw new IllegalStateException("Load a valid Studio model before migrating.");
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess()) {
                throw new IllegalStateException("Apply or discard the pending property edits before migrating.");
            }
            checkUnsaved(project);
            if (!context.tryBeginMigration()) throw new IllegalStateException("Wait for the current generation or migration to finish.");
            acquired = true;
            Path root = Path.of(project.getBasePath());
            String target = null;
            if (!restore) {
                Module live = context.getIkasanModule();
                if (live == null || !live.isInitialised()) throw new IllegalStateException("Configure and save the module first.");
                String current = live.getMetaVersion();
                if (!ModelMigration.SUPPORTED_VERSIONS.contains(current)) throw new IllegalStateException("Supported migration paths are V3.3.9 ↔ V4.1.6.");
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
                    return new Preview("Restore snapshot " + snapshot.id() + "\n\nThis restores the model, build configuration and generated files touched by that migration.\n"
                            + "Later edits to those files will be replaced by the previewed contents. Current contents are saved in a new recovery snapshot.\n"
                            + "Other files, including user/, are preserved.\n\n" + snapshot.report(), changes, module, true);
                }
                String source = Files.readString(root.resolve(MigrationArtifacts.MODEL));
                Module persisted = ComponentIO.validatePersistedModuleJson(source, "migration source", false);
                if (!StudioJson.newObjectMapper().readTree(ComponentIO.toJson(persisted)).equals(
                        StudioJson.newObjectMapper().readTree(ComponentIO.toJson(context.getIkasanModule())))) {
                    throw new IllegalStateException("The canvas and saved model differ. Regenerate or reload the model before migrating.");
                }
                var plan = ModelMigration.analyse(source, selectedTarget);
                if (!plan.canApply()) return new Preview(plan.report(), List.of(), null, false);
                String sourcePom = Files.readString(root.resolve("pom.xml"));
                var artifacts = MigrationArtifacts.render(plan, sourcePom);
                if (!Files.exists(root.resolve("AGENTS.md"))) artifacts.put("AGENTS.md", AiProjectContractGenerator.agentsGuide());
                var changes = MigrationWorkspace.prepare(root, artifacts);
                if (changes.stream().anyMatch(c -> c.path().equals("AGENTS.md") && c.before() != null)) {
                    throw new IllegalStateException("Project instructions were added while preparing migration. Preview again.");
                }
                // Preparation must not accept a source file which changed while templates were rendering.
                if (!changes.stream().filter(c -> c.path().equals(MigrationArtifacts.MODEL)).findFirst().orElseThrow().beforeText().equals(source)) {
                    throw new IllegalStateException("The model changed while preparing migration. Preview again.");
                }
                if (!changes.stream().filter(c -> c.path().equals("pom.xml")).findFirst().orElseThrow().beforeText().equals(sourcePom)) {
                    throw new IllegalStateException("The POM changed while preparing migration. Preview again.");
                }
                return new Preview(plan.report(), changes, ComponentIO.validatePersistedModuleJson(plan.targetJson(), "target", false), true);
            });
            if (project.isDisposed()) return;
            var dialog = new MigrationPreviewDialog(project, title, preview.report(), preview.changes(), preview.canApply());
            if (!dialog.showAndGet()) return;
            if (project.isDisposed()) return;
            checkUnsaved(project);
            Path snapshot = background(project, "Applying Ikasan migration", () ->
                    MigrationWorkspace.commit(root, preview.changes(), preview.report()));
            appliedSnapshot = snapshot;
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
                                    String result = aborted ? "Build cancelled" : errors == 0 ? "Build passed" : "Build failed with " + errors + " errors";
                                    com.intellij.notification.NotificationGroupManager.getInstance().getNotificationGroup("Ikasan Studio")
                                            .createNotification("Ikasan migration: " + result,
                                                    "Recovery snapshot: " + snapshot + ". Run application tests before deployment.",
                                                    errors == 0 && !aborted ? com.intellij.notification.NotificationType.INFORMATION : com.intellij.notification.NotificationType.WARNING)
                                            .notify(project);
                                });
                            });
                        })).onError(error -> notifyImportFailure(project, snapshot))
                ).onError(error -> notifyImportFailure(project, snapshot));
            } else if (maven != null) maven.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            Messages.showInfoMessage(project, "Migration applied. Recovery snapshot:\n" + snapshot
                    + (dialog.shouldCompile() ? "\n\nA build will run after Maven import. The target JDK must be installed and selected in Project Structure." : ""), title);
        } catch (Exception failure) {
            LOG.warn("Ikasan migration could not complete", failure);
            if (failure.getMessage() != null && failure.getMessage().contains("Automatic restoration was incomplete")) {
                context.blockModelPersistence(failure.getMessage());
            }
            if (project.isDisposed()) return;
            String detail = failure.getMessage() == null ? failure.toString() : failure.getMessage();
            if (appliedSnapshot != null) detail = "The files were migrated, but a follow-up step failed. Reload the model before continuing.\n"
                    + detail + "\nRecovery snapshot: " + appliedSnapshot;
            Messages.showWarningDialog(project, detail, title);
        } finally {
            if (acquired) context.endMigration();
        }
    }

    private static void notifyImportFailure(Project project, Path snapshot) {
        if (project.isDisposed()) return;
        com.intellij.notification.NotificationGroupManager.getInstance().getNotificationGroup("Ikasan Studio")
                .createNotification("Ikasan migration: Maven import failed",
                        "Files were migrated, but compilation could not start. Recovery snapshot: " + snapshot,
                        com.intellij.notification.NotificationType.WARNING).notify(project);
    }

    private static void checkUnsaved(Project project) {
        var manager = FileDocumentManager.getInstance();
        for (var document : manager.getUnsavedDocuments()) {
            var file = manager.getFile(document);
            if (file != null && file.getPath().startsWith(project.getBasePath() + "/")) {
                throw new IllegalStateException("Save the project's open files before migration: " + file.getName());
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
