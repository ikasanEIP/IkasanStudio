package org.ikasan.studio.ui.intellij.onboarding;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.ikasan.studio.ui.intellij.editor.IkasanStudioEditorService;

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
        if (project.isDisposed() || !isIkasanStudioProject(project)) {
            return Unit.INSTANCE;
        }

        IkasanStudioEditorService editorService = project.getService(IkasanStudioEditorService.class);
        if (!hasCompletedOnboarding(project)) {
            editorService.open();
            PropertiesComponent.getInstance(project)
                    .setValue(ONBOARDING_VERSION_PROPERTY, CURRENT_ONBOARDING_VERSION, 0);
        } else if (editorService.shouldRestore()) {
            editorService.open();
        }
        return Unit.INSTANCE;
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
