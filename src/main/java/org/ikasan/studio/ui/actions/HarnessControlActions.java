package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestFtpServerLinks;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Project-aware aggregate start/stop control for test harnesses referenced by the current model. Start and Stop
 * are independent, always-enabled actions - deliberately not one context-sensitive toggle that infers "already
 * running" and hides the option that doesn't currently apply. That inference has been wrong in practice (e.g. a
 * harness left running from an earlier session, or in another IDE window, that this project's own state tracking
 * doesn't know about), leaving the developer with no way to stop it because the toggle only ever offered Start.
 * Both underlying actions (StartTestFtpServerAction/StopTestFtpServerAction,
 * StartTestMailServerAction/StopTestMailServerAction) already handle being invoked in the "wrong" state
 * gracefully (e.g. an informational "already running" message rather than an error), so exposing both
 * unconditionally is safe.
 */
public final class HarnessControlActions {
    private final Project project;

    public HarnessControlActions(Project project) {
        this.project = project;
    }

    public ActionListener startAction() {
        return event -> {
            Module module = currentModule();
            if (module != null) {
                startHarnesses(module, event);
            }
        };
    }

    public ActionListener stopAction() {
        return event -> {
            Module module = currentModule();
            if (module != null) {
                stopHarnesses(module, event);
            }
        };
    }

    /** Whether the current model references any FTP or mail test harness at all - gates the group's visibility. */
    public boolean isAvailable() {
        return hasHarnesses(currentModule());
    }

    static boolean hasHarnesses(Module module) {
        return module != null && (!TestFtpServerLinks.findLinks(module).isEmpty()
                || !TestMailServerLinks.findLinks(module).isEmpty());
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
        if (!ftpLinks.isEmpty()) {
            FlowElement owner = firstFtpOwner(ftpLinks.get(0));
            if (owner != null) {
                new StopTestFtpServerAction(project, owner).actionPerformed(event);
            }
        }
        TestMailServerSessionService mailService = project.getService(TestMailServerSessionService.class);
        List<TestMailServerLinks.Link> mailLinks = TestMailServerLinks.findLinks(module);
        if (mailService.hasAnyOwned() && !mailLinks.isEmpty() && !mailLinks.get(0).producers().isEmpty()) {
            new StopTestMailServerAction(project, mailLinks.get(0).producers().get(0)).actionPerformed(event);
            return;
        }
        for (TestMailServerLinks.Link link : mailLinks) {
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
