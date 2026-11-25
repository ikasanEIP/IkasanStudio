package org.ikasan.studio.intellij.project;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.util.messages.Topic;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.Getter;
import org.ikasan.studio.ui.UiContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

/**
 * Project-scoped owner of the Ikasan Studio initialization lifecycle.
 */
@Service(Service.Level.PROJECT)
public final class StudioProjectInitialisationService implements Disposable {
    private static final Logger LOG = Logger.getInstance(StudioProjectInitialisationService.class);

    public enum State {
        NOT_STARTED,
        WAITING_FOR_PROJECT_IMPORT,
        WAITING_FOR_INDEXES,
        READING_PROJECT,
        LOADING_COMPONENTS,
        READY,
        UPDATE_AVAILABLE,
        FAILED
    }

    public interface Listener {
        Topic<Listener> TOPIC = Topic.create("Ikasan Studio initialization", Listener.class);

        void stateChanged(State state, String detail);
    }

    record ImportReadiness(boolean imported, String detail) { }

    private final Project project;
    private final com.intellij.util.Alarm importRecheck;
    private final java.util.function.Supplier<ImportReadiness> importReadiness;
    private final int importRecheckDelay;
    private static final int IMPORT_RECHECK_DELAY_MS = 1000;
    private final AtomicBoolean inProgress = new AtomicBoolean();
    @Getter
    private volatile State state = State.NOT_STARTED;
    @Getter
    private volatile String detail;

    private volatile List<Integer> validModelBackupIndexes = List.of();
    private volatile boolean disposed;
    private boolean packDiagnosticsShown;
    @Getter
    private volatile List<String> availableMetaPacks = List.of();
    @Getter
    private volatile java.util.Set<String> snapshotMetaPacks = java.util.Set.of();

    public List<Integer> getValidModelBackupIndexes() {
        return validModelBackupIndexes;
    }

    public StudioProjectInitialisationService(Project project) {
        this(project, () -> inspectImportReadiness(project), IMPORT_RECHECK_DELAY_MS);
    }

    StudioProjectInitialisationService(Project project, java.util.function.Supplier<ImportReadiness> readiness, int retryDelay) {
        this.project = project;
        this.importReadiness = readiness;
        this.importRecheckDelay = retryDelay;
        this.importRecheck = new com.intellij.util.Alarm(com.intellij.util.Alarm.ThreadToUse.SWING_THREAD, this);
        project.getMessageBus().connect(this).subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md)
            // because the IntelliJ Gradle plugin instruments it with a runtime assertion that
            // would surface as an uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                if (state == State.NOT_STARTED || state == State.WAITING_FOR_PROJECT_IMPORT) start();
            }
        });
    }

    public void start() {
        if (disposed || project.isDisposed() || state == State.READY || !inProgress.compareAndSet(false, true)) {
            return;
        }
        ReadAction.nonBlocking(importReadiness::get)
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), imported -> {
                    if (disposed || project.isDisposed()) {
                        inProgress.set(false);
                    } else if (!imported.imported()) {
                        waitForProjectImport(imported.detail());
                    } else {
                        transition(State.WAITING_FOR_INDEXES, null);
                        DumbService.getInstance(project).runWhenSmart(this::loadProjectWhenSmart);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService())
                .onError(failure -> {
                    inProgress.set(false);
                    if (!disposed && !project.isDisposed() && !(failure instanceof ProcessCanceledException)) {
                        fail(org.ikasan.studio.ui.StudioBundle.message("initialisation.ImportCheckFailed"),
                                failure instanceof Exception exception ? exception : new RuntimeException(failure));
                    }
                });
    }

    /** Ask Maven to rebuild its imported workspace model; Retry alone only observes readiness. */
    public void reloadMavenProject() {
        if (disposed || project.isDisposed() || state != State.WAITING_FOR_PROJECT_IMPORT) return;
        try {
            org.jetbrains.idea.maven.project.MavenProjectsManager.getInstance(project)
                    .forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            LOG.info("STUDIO: Requested Maven project reimport for " + project.getName());
            start();
        } catch (ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception failure) {
            LOG.warn("STUDIO: Could not request Maven project reimport", failure);
            org.ikasan.studio.ui.StudioUIUtils.displayIdeaWarnMessage(project,
                    org.ikasan.studio.ui.StudioBundle.message("initialisation.ReloadMavenFailed", failure.getMessage()));
        }
    }

    /** Restarts background loading after an explicit, validated revision-lock adoption. */
    public void reloadModel() {
        if (disposed || project.isDisposed() || inProgress.get()) return;
        state = State.NOT_STARTED;
        start();
    }

    public void markReady() {
        inProgress.set(false);
        transition(State.READY, null);
    }

    public void fail(String userMessage, Exception exception) {
        if (exception != null) {
            LOG.warn(org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent.format(
                    org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent.Event.CONFIGURATION_INVALID, exception, null, null, null));
        }
        inProgress.set(false);
        transition(State.FAILED, userMessage);
    }

    private void loadProjectWhenSmart() {
        if (disposed || project.isDisposed()) {
            inProgress.set(false);
            return;
        }
        transition(State.READING_PROJECT, null);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (disposed || project.isDisposed()) return;
                org.ikasan.studio.intellij.project.StudioProjectInitialisationService.ImportReadiness readiness = ReadAction.nonBlocking(importReadiness::get).expireWith(this).executeSynchronously();
                if (!readiness.imported()) {
                    waitForProjectImport(readiness.detail());
                    return;
                }
                if (!packDiagnosticsShown) {
                    Map<String,String> problems = org.ikasan.studio.core.metapack.ComponentLibrary.getUnavailableMetaPacks();
                    packDiagnosticsShown = true;
                    if (!problems.isEmpty()) {
                        String details = problems.entrySet().stream()
                                .map(entry -> entry.getKey() + ": " + entry.getValue())
                                .collect(java.util.stream.Collectors.joining("\n"));
                        org.ikasan.studio.intellij.metapack.MetaPackNotifications.showUnavailable(project, details);
                    }
                }
                availableMetaPacks = List.copyOf(org.ikasan.studio.core.metapack.ComponentLibrary.getMetapackList());
                List<String> released = org.ikasan.studio.core.metapack.ComponentLibrary.getMetapackList(false);
                snapshotMetaPacks = availableMetaPacks.stream().filter(id -> !released.contains(id))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                UiContext uiContext = project.getService(UiContext.class);
                validModelBackupIndexes = List.of();
                StudioProjectFiles.synchGenerateModelInstanceFromJSON(project);
                if (uiContext.isModelPersistenceBlocked()) {
                    // Only offer the normal update journey after validating the proposed model and templates.
                    Path modelPath = StudioProjectFiles.getModelJsonPath(project);
                    String basePath = project.getBasePath();
                    try {
                        if (modelPath != null && basePath != null) {
                            org.ikasan.studio.core.metapack.ModelPackRepair.Plan update =
                                    org.ikasan.studio.core.metapack.ModelPackRepair.preview(Files.readString(modelPath));
                            if (update.isRevisionUpdate()) {
                                org.ikasan.studio.core.model.ikasan.instance.Module proposed =
                                        org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(update.targetJson(), "revision update preview", false);
                                org.ikasan.studio.core.generator.ProjectArtifactRenderer.render(proposed,
                                        Files.readString(Path.of(basePath, StudioProjectFiles.POM_XML)));
                                inProgress.set(false);
                                transition(State.UPDATE_AVAILABLE, org.ikasan.studio.ui.StudioBundle.message(
                                        "modelUpdate.Detail", update.targetPack(), update.previousRevision(), update.targetRevision()));
                                return;
                            }
                        }
                    } catch (ProcessCanceledException cancelled) { throw cancelled; }
                    catch (Exception unavailable) {
                        LOG.debug("Model is not eligible for a validated revision update", unavailable);
                    }
                    validModelBackupIndexes = StudioProjectFiles.getValidModelBackupIndexes(project);
                    String recoveryGuidance = validModelBackupIndexes.isEmpty()
                            ? org.ikasan.studio.ui.StudioBundle.message("modelRepair.NoBackupGuidance")
                            : org.ikasan.studio.ui.StudioBundle.message("modelRepair.BackupGuidance");
                    fail("Studio opened in recovery mode because model.json could not be loaded safely. " +
                            "The original file is preserved. " + uiContext.getModelPersistenceBlockReason() + " " + recoveryGuidance, null);
                    return;
                }
                if (uiContext.getIkasanModule() == null) {
                    fail("The Ikasan project model is not available yet. Check that Maven import completed, then try again.", null);
                    return;
                }
                if (uiContext.getPipsiIkasanModel() == null) {
                    fail("Ikasan Studio could not create its project model. Close and reopen the Ikasan Studio editor tab, then try again.", null);
                    return;
                }
                uiContext.getPipsiIkasanModel().initialisePsiFileHandles();
                transition(State.LOADING_COMPONENTS, null);
            } catch (ProcessCanceledException e) {
                inProgress.set(false);
                throw e;
            } catch (Exception e) {
                fail("The project model could not be loaded. Check that Maven import completed, then try again.", e);
            }
        });
    }

    public void restoreModelBackup(int index) {
        if (project.isDisposed() || !validModelBackupIndexes.contains(index) || !inProgress.compareAndSet(false, true)) {
            return;
        }
        transition(State.READING_PROJECT, "Restoring and validating model.json.bak." + index);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                java.nio.file.Path rejected = StudioProjectFiles.restoreModelBackup(project, index);
                org.ikasan.studio.ui.StudioUIUtils.displayIdeaInfoMessage(project,
                        "Restored model.json.bak." + index + ". The rejected model was preserved as " +
                                (rejected == null ? "a diagnostic copy." : rejected.getFileName() + "."));
                validModelBackupIndexes = List.of();
                inProgress.set(false);
                state = State.NOT_STARTED;
                start();
            } catch (Exception e) {
                fail("The selected model backup could not be restored. The current model and backup were preserved.", e);
            }
        });
    }

    private static ImportReadiness inspectImportReadiness(Project project) {
        boolean imported = StudioProjectFiles.hasGeneratedContentRoot(project);
        if (imported) return new ImportReadiness(true, null);
        String roots = java.util.Arrays.stream(com.intellij.openapi.roots.ProjectRootManager.getInstance(project).getContentRoots())
                .filter(com.intellij.openapi.vfs.VirtualFile::isValid)
                .map(com.intellij.openapi.vfs.VirtualFile::getPath).sorted().collect(java.util.stream.Collectors.joining("\n"));
        String expected = project.getBasePath() + StudioProjectFiles.GENERATED_CONTENT_ROOT;
        return new ImportReadiness(false, org.ikasan.studio.ui.StudioBundle.message("initialisation.ImportRoots", expected,
                roots.isEmpty() ? org.ikasan.studio.ui.StudioBundle.message("initialisation.NoContentRoots") : roots));
    }

    private void waitForProjectImport(String reason) {
        inProgress.set(false);
        if (state != State.WAITING_FOR_PROJECT_IMPORT || !java.util.Objects.equals(detail, reason))
            LOG.info("STUDIO: Waiting for generated module registration. " + reason);
        transition(State.WAITING_FOR_PROJECT_IMPORT, reason);
        // A rootsChanged event can arrive while a readiness read is still in progress,
        // or before Maven's final workspace update becomes visible. Recheck independently.
        importRecheck.cancelAllRequests();
        if (!disposed && !project.isDisposed()) importRecheck.addRequest(() -> {
            if (state == State.WAITING_FOR_PROJECT_IMPORT) start();
        }, importRecheckDelay);
    }

    private void transition(State newState, String newDetail) {
        if (disposed) return;
        if (newState != State.WAITING_FOR_PROJECT_IMPORT) importRecheck.cancelAllRequests();
        state = newState;
        detail = newDetail;
        if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(Listener.TOPIC).stateChanged(newState, newDetail);
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        importRecheck.cancelAllRequests();
        inProgress.set(false);
    }
}
