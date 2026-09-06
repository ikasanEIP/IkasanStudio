package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/** Selects the embedded FTP root in the IntelliJ Project view. */
public final class ShowTestFtpDirectoryAction implements ActionListener {
    private final Project project;
    private final BasicElement element;

    public ShowTestFtpDirectoryAction(Project project, BasicElement element) {
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
        if (!service.isRunning()) {
            StudioUIUtils.displayIdeaInfoMessage(project,
                    StudioBundle.message("message.TestFtpServerNotRunning"));
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> showDirectory(service));
    }

    private void showDirectory(TestFtpServerService service) {
        try {
            Path directory = service.getRootDirectory();
            if (!StudioNavigator.selectInProjectView(project, directory)) {
                showFailure(StudioBundle.message("message.TestFtpDirectoryCouldNotBeFound"));
            }
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            showFailure(StudioBundle.message("message.CouldNotShowTestFtpDirectory", detail));
        }
    }

    private void showFailure(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project, message);
        });
    }
}
