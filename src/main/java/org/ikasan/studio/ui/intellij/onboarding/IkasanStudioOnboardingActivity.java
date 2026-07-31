package org.ikasan.studio.ui.intellij.onboarding;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.ikasan.studio.ui.intellij.toolWindow.DesignerToolWindowFactory;

import java.nio.file.Path;

/**
 * Opens Ikasan Studio on the first project launch for projects created by the Studio archetype.
 */
public final class IkasanStudioOnboardingActivity implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(IkasanStudioOnboardingActivity.class);
    private static final String ONBOARDING_VERSION_PROPERTY = "ikasan.studio.onboarding.version";
    private static final int CURRENT_ONBOARDING_VERSION = 1;

    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation) {
        if (project.isDisposed() || hasCompletedOnboarding(project)) {
            return Unit.INSTANCE;
        }

        String basePath = project.getBasePath();
        if (basePath == null || !IkasanStudioProjectDetector.isIkasanStudioProject(Path.of(basePath))) {
            return Unit.INSTANCE;
        }

        ApplicationManager.getApplication().invokeLater(() -> openStudioForOnboarding(project));
        return Unit.INSTANCE;
    }

    private void openStudioForOnboarding(Project project) {
        if (project.isDisposed() || hasCompletedOnboarding(project)) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(DesignerToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow == null) {
            LOG.warn("STUDIO: Ikasan Studio tool window was unavailable during first-run onboarding");
            return;
        }

        toolWindow.activate(() -> PropertiesComponent.getInstance(project)
                .setValue(ONBOARDING_VERSION_PROPERTY, CURRENT_ONBOARDING_VERSION, 0), true);
    }

    private boolean hasCompletedOnboarding(Project project) {
        return PropertiesComponent.getInstance(project)
                .getInt(ONBOARDING_VERSION_PROPERTY, 0) >= CURRENT_ONBOARDING_VERSION;
    }
}
