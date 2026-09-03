package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Stops only the FTP server instance owned by this Studio project. */
public final class StopTestFtpServerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance(StopTestFtpServerAction.class);
    private final Project project;
    private final BasicElement element;
    public StopTestFtpServerAction(Project project, BasicElement element) { this.project = project; this.element = element; }

    @Override public void actionPerformed(ActionEvent event) {
        if (!(element instanceof FlowElement flowElement) || !flowElement.getComponentMeta().supportsTestFtpServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestFtpServerCanOnlyBeUsedOnFtpConsumer"));
            return;
        }
        TestFtpServerService service = project.getService(TestFtpServerService.class);
        if (!service.isRunning()) {
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestFtpServerNotRunning"));
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Stopping test FTP server") {
            @SuppressWarnings("NullableProblems")
            @Override public void run(ProgressIndicator indicator) {
                try {
                    service.stop();
                    ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaInfoMessage(project,
                            StudioBundle.message("message.TestFtpServerStopped")));
                } catch (Exception e) {
                    LOG.warn("STUDIO: Could not stop test FTP server", e);
                    String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("message.CouldNotStopTestFtpServer", detail)));
                }
            }
        });
    }
}
