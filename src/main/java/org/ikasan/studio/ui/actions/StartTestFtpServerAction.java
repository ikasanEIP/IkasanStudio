package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/** Starts Studio\x27s project-scoped embedded FTP server for an FTP Consumer. */
public final class StartTestFtpServerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance(StartTestFtpServerAction.class);
    private final Project project;
    private final BasicElement element;
    public StartTestFtpServerAction(Project project, BasicElement element) { this.project = project; this.element = element; }

    @Override public void actionPerformed(ActionEvent event) {
        if (!(element instanceof FlowElement flowElement) || !flowElement.getComponentMeta().supportsTestFtpServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestFtpServerCanOnlyBeUsedOnFtpConsumer"));
            return;
        }
        TestFtpServerConfiguration configuration = TestFtpServerConfiguration.from(flowElement);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Starting test FTP server") {
            @SuppressWarnings("NullableProblems")
            @Override public void run(ProgressIndicator indicator) {
                try {
                    TestFtpServerService service = project.getService(TestFtpServerService.class);
                    boolean alreadyRunning = service.isRunningAt(configuration);
                    Path root = service.start(configuration);
                    String statusMessage = alreadyRunning
                            ? StudioBundle.message("message.TestFtpServerAlreadyRunning", configuration.address(), root)
                            : StudioBundle.message("message.StartingTestFtpServer", configuration.address(), configuration.username(), root);
                    ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaInfoMessage(project,
                            statusMessage + "<br><br>" + StudioBundle.message("message.TestFtpServerOverwriteLimitation")));
                } catch (IllegalArgumentException e) {
                    ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("message.TestFtpServerRequiresLocalHost")));
                } catch (Exception e) {
                    String safeDetail = StopTestMailServerAction.redact(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    LOG.warn("STUDIO: Could not start test FTP server: " + safeDetail);
                    if (isPortConflict(e)) {
                        ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("message.TestFtpServerPortConflict", configuration.address())));
                        return;
                    }
                    ApplicationManager.getApplication().invokeLater(() -> StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("message.CouldNotStartTestFtpServer", safeDetail)));
                }
            }
        });
    }

    /**
     * Apache MINA's FtpServer wraps the real java.net.BindException ("Address already in use") in its own
     * generic FtpException with a "Failed to bind..." message - walking the cause chain for the actual
     * BindException is more robust than matching that wording, which could change between library versions.
     * Since {@link TestFtpServerService} is project-scoped (each project runs its own embedded server, with
     * no awareness of any other project's), while the actual TCP port is real OS-level state shared across
     * every project on the machine, this is far more often "a different project's test FTP server already
     * owns this exact host/port" than any other startup failure - worth a specific, actionable message.
     */
    private static boolean isPortConflict(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof java.net.BindException) {
                return true;
            }
        }
        return false;
    }
}
