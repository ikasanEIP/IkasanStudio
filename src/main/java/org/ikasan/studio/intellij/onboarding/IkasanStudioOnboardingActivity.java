package org.ikasan.studio.intellij.onboarding;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import java.util.concurrent.atomic.AtomicBoolean;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.ikasan.studio.intellij.editor.IkasanStudioEditorService;

import java.nio.file.Path;

/**
 * Opens Ikasan Studio on first use and restores it on later project launches only when the user
 * left the Studio editor open.
 */
public final class IkasanStudioOnboardingActivity implements ProjectActivity {
    private static final String ONBOARDING_VERSION_PROPERTY = "ikasan.studio.onboarding.version";
    private static final int CURRENT_ONBOARDING_VERSION = 1;

    // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
    // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
    // uncaught plugin exception rather than failing gracefully.
    @SuppressWarnings("NullableProblems")
    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation) {
        if (project.isDisposed()) {
            return Unit.INSTANCE;
        }
        AtomicBoolean handled = new AtomicBoolean();
        Runnable tryOpen = () -> ReadAction.nonBlocking(() ->
                        !handled.get() && isIkasanStudioProject(project)
                                && StudioProjectFiles.hasGeneratedContentRoot(project))
                .expireWith(project)
                .finishOnUiThread(ModalityState.nonModal(), ready -> {
                    if (!ready || project.isDisposed() || handled.get()) return;
                    DumbService.getInstance(project).runWhenSmart(() ->
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (project.isDisposed() || !handled.compareAndSet(false, true)) return;
                                openAfterImport(project);
                            }, ModalityState.nonModal()));
                })
                .submit(AppExecutorUtil.getAppExecutorService());
        // Archetype generation may finish after startup. Retry when Maven adds its content roots.
        project.getMessageBus().connect(project).subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            @SuppressWarnings("NullableProblems")
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                if (!handled.get()) tryOpen.run();
            }
        });
        tryOpen.run();
        return Unit.INSTANCE;
    }

    private void openAfterImport(Project project) {
        IkasanStudioEditorService editorService = project.getService(IkasanStudioEditorService.class);
        if (!hasCompletedOnboarding(project)) {
            editorService.open();
            PropertiesComponent.getInstance(project)
                    .setValue(ONBOARDING_VERSION_PROPERTY, CURRENT_ONBOARDING_VERSION, 0);
        } else if (editorService.shouldRestore()) {
            editorService.open();
        }
    }

    private boolean isIkasanStudioProject(Project project) {
        String basePath = project.getBasePath();
        return basePath != null
                && IkasanStudioProjectDetector.isIkasanStudioProject(Path.of(basePath));
    }

    private boolean hasCompletedOnboarding(Project project) {
        return PropertiesComponent.getInstance(project)
                .getInt(ONBOARDING_VERSION_PROPERTY, 0) >= CURRENT_ONBOARDING_VERSION;
    }
}
