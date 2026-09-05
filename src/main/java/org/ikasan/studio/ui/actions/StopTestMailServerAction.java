package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Stops only the mail harness registered as owned by this project. External listeners are never terminated. */
public class StopTestMailServerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#StopTestMailServerAction");
    private final Project project;
    private final BasicElement ikasanBasicElement;

    public StopTestMailServerAction(Project project, BasicElement ikasanBasicElement) {
        this.project = project;
        this.ikasanBasicElement = ikasanBasicElement;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (!(ikasanBasicElement instanceof FlowElement flowElement)
                || !flowElement.getComponentMeta().supportsTestMailServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TestMailServerCanOnlyBeUsedOnEmailProducer"));
            return;
        }
        String smtpHost = TestMailServerLinks.resolveSmtpHost(flowElement);
        int smtpPort = TestMailServerLinks.resolveSmtpPort(flowElement);
        String smtpAddress = smtpHost + ":" + smtpPort;
        TestMailServerSessionService service = project.getService(TestMailServerSessionService.class);
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                StudioBundle.message("message.StoppingTestMailServer")) {
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                boolean listening = TestMailServerSupport.isAlreadyListening(smtpHost, smtpPort);
                boolean owned = service.hasAnyOwned();
                ApplicationManager.getApplication().invokeLater(
                        () -> stopOnEdt(service, smtpAddress, listening, owned));
            }
        });
    }

    private void stopOnEdt(TestMailServerSessionService service, String smtpAddress,
                           boolean listening, boolean owned) {
        if (!listening && !owned) {
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerNotRunning"));
            return;
        }
        if (!owned) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TestMailServerExternallyOwnedCannotStop", smtpAddress));
            return;
        }
        try {
            if (service.stopAnyOwned()) {
                service.pollNow();
                StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerStopped"));
            }
        } catch (RuntimeException e) {
            String detail = redact(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            LOG.warn("STUDIO: Could not stop the owned test mail server: " + detail);
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.CouldNotStopTestMailServer", detail));
        }
    }
    static String redact(String text) {
        if (text == null || text.isBlank()) return "Unknown failure";
        return text.replaceAll("(?i)(password|passphrase|private[-_]?key(?:filename|path)?)(\\s*[=:]\\s*|\\s+)([^,;\\s]+)", "$1$2<redacted>");
    }
}
