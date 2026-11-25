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
import java.util.ArrayList;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;

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
    private final java.util.function.Predicate<List<org.ikasan.studio.intellij.runtime.HarnessStopImpact.Endpoint>> confirmStop;

    public HarnessControlActions(Project project) {
        this(project, endpoints -> HarnessStopWarning.confirm(project, endpoints));
    }

    HarnessControlActions(Project project, java.util.function.Predicate<List<org.ikasan.studio.intellij.runtime.HarnessStopImpact.Endpoint>> confirmStop) {
        this.project = project; this.confirmStop = confirmStop;
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

    public record HarnessStatus(List<String> owned, List<String> shared) {
        public HarnessStatus { owned = List.copyOf(owned); shared = List.copyOf(shared); }
        public List<String> available() { return java.util.stream.Stream.concat(owned.stream(), shared.stream()).toList(); }
    }

    public List<String> runningHarnesses() { return status().available(); }

    /** Availability and ownership are separate; cached reads only on the EDT. */
    public HarnessStatus status() {
        if (project.isDisposed()) return new HarnessStatus(List.of(), List.of());
        ArrayList<String> running = new java.util.ArrayList<>();
        ArrayList<String> shared = new java.util.ArrayList<>();
        TestFtpServerService ftp = project.getService(org.ikasan.studio.intellij.runtime.TestFtpServerService.class);
        TestSftpServerService sftp = project.getService(org.ikasan.studio.intellij.runtime.TestSftpServerService.class);
        TestMailServerSessionService mail = project.getService(TestMailServerSessionService.class);
        if (ftp != null && ftp.isRunning()) running.add("FTP");
        if (sftp != null && sftp.isRunning()) running.add("SFTP");
        if (mail != null && mail.hasAnyOwnedListening()) running.add("SMTP");
        else if (mail != null) {
            UiContext context = project.getService(UiContext.class);
            Module module = context == null ? null : context.getIkasanModule();
            if (module != null && TestMailServerLinks.findLinks(module).stream().flatMap(link -> link.producers().stream())
                    .anyMatch(element -> mail.isListening(
                            org.ikasan.studio.intellij.runtime.HarnessEndpointSettings.effectiveMailHost(project, element),
                            org.ikasan.studio.intellij.runtime.HarnessEndpointSettings.effectiveMailPort(project, element))))
                shared.add(org.ikasan.studio.ui.StudioBundle.message("harness.smtp.sharedAvailable"));
        }
        return new HarnessStatus(running, shared);
    }

    static boolean hasHarnesses(Module module) {
        return module != null && (!TestFtpServerLinks.findLinks(module).isEmpty()
                || !TestMailServerLinks.findLinks(module).isEmpty()
                || org.ikasan.studio.intellij.runtime.TestSftpServerService.hasSftp(module));
    }

    private void startHarnesses(Module module, ActionEvent event) {
        HarnessOverrideService overrides = project.getService(org.ikasan.studio.intellij.execution.HarnessOverrideService.class);
        if (overrides != null && overrides.targets().stream().anyMatch(t -> "SFTP".equals(t.kind())
                && overrides.nextSelection().contains(t.id()))) SftpHarnessAction.start(project);
        List<TestFtpServerLinks.Link> ftpLinks = TestFtpServerLinks.findLinks(module);
        if (!ftpLinks.isEmpty()) {
            FlowElement owner = overrides == null ? firstFtpOwner(ftpLinks.get(0)) :
                    overrides.targets().stream().filter(t -> "FTP".equals(t.kind()) && overrides.nextSelection().contains(t.id()))
                            .map(org.ikasan.studio.core.model.analysis.HarnessOverrides.Target::element)
                            .findFirst().orElse(firstFtpOwner(ftpLinks.get(0)));
            if (owner != null) {
                new StartTestFtpServerAction(project, owner).actionPerformed(event);
            }
        }
        List<TestMailServerLinks.Link> mailLinks = TestMailServerLinks.findLinks(module);
        if (!mailLinks.isEmpty() && !mailLinks.get(0).producers().isEmpty()) {
            FlowElement owner = overrides == null ? mailLinks.get(0).producers().get(0) :
                    overrides.targets().stream().filter(t -> "Email".equals(t.kind()) && overrides.nextSelection().contains(t.id()))
                            .map(org.ikasan.studio.core.model.analysis.HarnessOverrides.Target::element)
                            .findFirst().orElse(mailLinks.get(0).producers().get(0));
            new StartTestMailServerAction(project, owner).actionPerformed(event);
        }
    }

    private void stopHarnesses(Module module, ActionEvent event) {
        if (!confirmStop.test(org.ikasan.studio.intellij.runtime.HarnessStopImpact.owned(project))) return;
        SftpHarnessAction.stop(project, true);
        List<TestFtpServerLinks.Link> ftpLinks = TestFtpServerLinks.findLinks(module);
        if (!ftpLinks.isEmpty()) {
            FlowElement owner = firstFtpOwner(ftpLinks.get(0));
            if (owner != null) {
                new StopTestFtpServerAction(project, owner, true).actionPerformed(event);
            }
        }
        TestMailServerSessionService mailService = project.getService(TestMailServerSessionService.class);
        List<TestMailServerLinks.Link> mailLinks = TestMailServerLinks.findLinks(module);
        if (mailService.hasAnyOwned() && !mailLinks.isEmpty() && !mailLinks.get(0).producers().isEmpty()) {
            new StopTestMailServerAction(project, mailLinks.get(0).producers().get(0), true).actionPerformed(event);
            return;
        }
        for (TestMailServerLinks.Link link : mailLinks) {
            if (mailService.isListening(link.host(), link.port()) && !link.producers().isEmpty()) {
                new StopTestMailServerAction(project, link.producers().get(0), false).actionPerformed(event);
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
