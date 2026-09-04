package org.ikasan.studio.intellij.project;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.util.messages.Topic;
import lombok.Getter;
import org.ikasan.studio.ui.UiContext;

import java.util.concurrent.atomic.AtomicBoolean;

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
        FAILED
    }

    public interface Listener {
        Topic<Listener> TOPIC = Topic.create("Ikasan Studio initialization", Listener.class);

        void stateChanged(State state, String detail);
    }

    private final Project project;
    private final AtomicBoolean inProgress = new AtomicBoolean();
    @Getter
    private volatile State state = State.NOT_STARTED;
    @Getter
    private volatile String detail;

    public StudioProjectInitialisationService(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md)
            // because the IntelliJ Gradle plugin instruments it with a runtime assertion that
            // would surface as an uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                start();
            }
        });
    }

    public void start() {
        if (project.isDisposed() || state == State.READY || !inProgress.compareAndSet(false, true)) {
            return;
        }
        if (!StudioProjectFiles.hasGeneratedContentRoot(project)) {
            waitForProjectImport();
            return;
        }

        transition(State.WAITING_FOR_INDEXES, null);
        DumbService.getInstance(project).runWhenSmart(this::loadProjectWhenSmart);
    }

    public void markReady() {
        inProgress.set(false);
        transition(State.READY, null);
    }

    public void fail(String userMessage, Exception exception) {
        if (exception != null) {
            LOG.warn("STUDIO: Could not initialize Ikasan Studio", exception);
        }
        inProgress.set(false);
        transition(State.FAILED, userMessage);
    }

    private void loadProjectWhenSmart() {
        if (project.isDisposed()) {
            inProgress.set(false);
            return;
        }
        if (!StudioProjectFiles.hasGeneratedContentRoot(project)) {
            waitForProjectImport();
            return;
        }

        transition(State.READING_PROJECT, null);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                UiContext uiContext = project.getService(UiContext.class);
                StudioProjectFiles.synchGenerateModelInstanceFromJSON(project);
                if (uiContext.isModelPersistenceBlocked()) {
                    fail("Studio opened in recovery mode because model.json could not be loaded safely. " +
                            "The original file is preserved; restore or correct it and use Reload from Disk.", null);
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

    private void waitForProjectImport() {
        inProgress.set(false);
        transition(State.WAITING_FOR_PROJECT_IMPORT, null);
    }

    private void transition(State newState, String newDetail) {
        state = newState;
        detail = newDetail;
        if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(Listener.TOPIC).stateChanged(newState, newDetail);
        }
    }

    @Override
    public void dispose() {
        // Disposed automatically by the platform when the project closes; nothing to release here
        // beyond the message-bus connection expiring via connect(this).
    }
}
