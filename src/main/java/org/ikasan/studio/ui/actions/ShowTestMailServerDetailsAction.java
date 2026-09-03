package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Redisplays the connection details for the running local test mail server. */
public final class ShowTestMailServerDetailsAction implements ActionListener {
    private final Project project;
    private final BasicElement element;

    public ShowTestMailServerDetailsAction(Project project, BasicElement element) {
        this.project = project;
        this.element = element;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (!(element instanceof FlowElement flowElement)
                || !flowElement.getComponentMeta().supportsTestMailServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TestMailServerCanOnlyBeUsedOnEmailProducer"));
            return;
        }

        String smtpHost = TestMailServerLinks.resolveSmtpHost(flowElement);
        int smtpPort = TestMailServerLinks.resolveSmtpPort(flowElement);
        if (!project.getService(TestMailServerSessionService.class).isListening(smtpHost, smtpPort)) {
            StudioUIUtils.displayIdeaInfoMessage(project,
                    StudioBundle.message("message.TestMailServerNotRunning"));
            return;
        }

        String webInbox = "http://" + TestMailServerSupport.UI_HOST + ":" + TestMailServerSupport.UI_PORT;
        StudioUIUtils.displayIdeaInfoMessage(project,
                StudioBundle.message("message.TestMailServerDetails", smtpHost, smtpPort, webInbox));
    }
}
