package org.ikasan.studio.ui.actions;

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

/** Redisplays the connection and filesystem details for the running embedded FTP server. */
public final class ShowTestFtpServerDetailsAction implements ActionListener {
    private final Project project;
    private final BasicElement element;

    public ShowTestFtpServerDetailsAction(Project project, BasicElement element) {
        this.project = project;
        this.element = element;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (!(element instanceof FlowElement flowElement)
                || !flowElement.getComponentMeta().supportsTestFtpServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TestFtpServerCanOnlyBeUsedOnFtpConsumer"));
            return;
        }
        TestFtpServerService service = project.getService(TestFtpServerService.class);
        TestFtpServerConfiguration configuration = TestFtpServerConfiguration.from(flowElement);
        Path rootDirectory = service.getRootDirectory();
        if (!service.isRunningAt(configuration) || rootDirectory == null) {
            StudioUIUtils.displayIdeaInfoMessage(project,
                    StudioBundle.message("message.TestFtpServerNotRunning"));
            return;
        }
        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestFtpServerDetails",
                configuration.host(), configuration.port(), configuration.username(), configuration.password(),
                rootDirectory));
    }
}
