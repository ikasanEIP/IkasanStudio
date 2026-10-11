package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.ui.StudioBundle;
import java.nio.file.Path;

/** Opens an existing test without generating or changing developer-owned files. */
public final class NavigateToFlowTestAction {
    private static final Logger LOG = Logger.getInstance(NavigateToFlowTestAction.class);
    private NavigateToFlowTestAction() { }

    public static void open(Project project, Flow flow) {
        if (project.isDisposed() || project.getBasePath() == null) return;
        // Capture mutable model state on the UI thread; refresh files in the background.
        Path path = Path.of(project.getBasePath()).resolve(FlowTestScaffold.testPath(flow));
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (project.isDisposed()) return;
            try {
                if (!StudioNavigator.openFileByNioPath(project, path))
                    showMessage(project, StudioBundle.message("flowTest.navigation.missing", path.toString()));
            } catch (ProcessCanceledException cancelled) {
                throw cancelled;
            } catch (Exception failure) {
                LOG.warn("Could not navigate to flow test: " + path, failure);
                showMessage(project, StudioBundle.message("flowTest.navigation.failed"));
            }
        });
    }

    private static void showMessage(Project project, String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) Messages.showInfoMessage(project, message,
                    StudioBundle.message("menu.JumpToFlowTest"));
        });
    }
}
