package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestFtpServerLinks;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/** Project-aware aggregate start/stop control for test harnesses referenced by the current model. */
public final class ToggleTestHarnessesAction implements ActionListener {
    private final Project project;
    private final JButton button;

    public ToggleTestHarnessesAction(Project project, JButton button) {
        this.project = project;
        this.button = button;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Module module = currentModule();
        if (module == null) {
            return;
        }
        if (isAnyHarnessRunning(module)) {
            stopHarnesses(module, event);
        } else {
            startHarnesses(module, event);
        }
        refreshPresentation();
    }

    public void refreshPresentation() {
        Module module = currentModule();
        boolean available = hasHarnesses(module);
        boolean running = available && isAnyHarnessRunning(module);
        button.setEnabled(available);
        button.setIcon(running ? com.intellij.icons.AllIcons.Actions.Suspend : com.intellij.icons.AllIcons.Actions.Execute);
        button.setText(StudioBundle.message("button.Harnesses"));
        button.setToolTipText(running ? StudioBundle.message("tooltip.StopHarnesses") : StudioBundle.message("tooltip.StartHarnesses"));
        button.getAccessibleContext().setAccessibleName(running ? StudioBundle.message("accessible.StopHarnesses") : StudioBundle.message("accessible.StartHarnesses"));
    }

    static boolean hasHarnesses(Module module) {
        return module != null && (!TestFtpServerLinks.findLinks(module).isEmpty()
                || !TestMailServerLinks.findLinks(module).isEmpty());
    }

    private boolean isAnyHarnessRunning(Module module) {
        if (project.getService(TestFtpServerService.class).isRunning()) {
            return true;
        }
        TestMailServerSessionService mailService = project.getService(TestMailServerSessionService.class);
        return TestMailServerLinks.findLinks(module).stream()
                .anyMatch(link -> mailService.isListening(link.host(), link.port()));
    }

    private void startHarnesses(Module module, ActionEvent event) {
        List<TestFtpServerLinks.Link> ftpLinks = TestFtpServerLinks.findLinks(module);
        if (!ftpLinks.isEmpty()) {
            FlowElement owner = firstFtpOwner(ftpLinks.get(0));
            if (owner != null) {
                new StartTestFtpServerAction(project, owner).actionPerformed(event);
            }
        }
        List<TestMailServerLinks.Link> mailLinks = TestMailServerLinks.findLinks(module);
        if (!mailLinks.isEmpty() && !mailLinks.get(0).producers().isEmpty()) {
            new StartTestMailServerAction(project, mailLinks.get(0).producers().get(0)).actionPerformed(event);
        }
    }

    private void stopHarnesses(Module module, ActionEvent event) {
        List<TestFtpServerLinks.Link> ftpLinks = TestFtpServerLinks.findLinks(module);
        if (!ftpLinks.isEmpty() && project.getService(TestFtpServerService.class).isRunning()) {
            FlowElement owner = firstFtpOwner(ftpLinks.get(0));
            if (owner != null) {
                new StopTestFtpServerAction(project, owner).actionPerformed(event);
            }
        }
        TestMailServerSessionService mailService = project.getService(TestMailServerSessionService.class);
        for (TestMailServerLinks.Link link : TestMailServerLinks.findLinks(module)) {
            if (mailService.isListening(link.host(), link.port()) && !link.producers().isEmpty()) {
                new StopTestMailServerAction(project, link.producers().get(0)).actionPerformed(event);
                break;
            }
        }
    }

    private static FlowElement firstFtpOwner(TestFtpServerLinks.Link link) {
        if (!link.consumers().isEmpty()) {
            return link.consumers().get(0);
        }
        return link.producers().isEmpty() ? null : link.producers().get(0);
    }

    private Module currentModule() {
        return project.isDisposed() ? null : project.getService(UiContext.class).getIkasanModule();
    }
}
