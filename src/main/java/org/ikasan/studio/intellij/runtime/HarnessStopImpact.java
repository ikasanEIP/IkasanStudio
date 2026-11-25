package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import java.util.*;
import java.util.ArrayList;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;

/** Cached ownership/model inspection only: no socket probes, DNS or process discovery. */
public final class HarnessStopImpact {
    private HarnessStopImpact() { }
    public record Endpoint(String kind, String host, int port) {
        public boolean matches(Endpoint other) {
            return other != null && kind.equals(other.kind) && port > 0 && other.port > 0
                    && new TestFtpServerConfiguration(host, port, "", "").sameAddressAs(
                            new TestFtpServerConfiguration(other.host, other.port, "", ""));
        }
        public String label() { return kind + " " + host + ":" + port; }
    }
    public record Impact(String project, Endpoint endpoint) { }
    public static List<Endpoint> owned(Project project) {
        ArrayList<Endpoint> result = new ArrayList<>();
        Endpoint ftp = project.getService(TestFtpServerService.class).listeningEndpoint();
        Endpoint sftp = project.getService(TestSftpServerService.class).listeningEndpoint();
        if (ftp != null) result.add(ftp);
        if (sftp != null) result.add(sftp);
        result.addAll(project.getService(TestMailServerSessionService.class).ownedEndpoints());
        return List.copyOf(result);
    }
    public static List<Impact> affected(Project owner, List<Project> open, List<Endpoint> endpoints) {
        Set<Impact> result = new LinkedHashSet<>();
        for (Project other : open) {
            if (other == owner || other.isDisposed()) continue;
            UiContext context = other.getService(UiContext.class);
            Module module = context == null ? null : context.getIkasanModule();
            if (module == null || module.getFlows() == null) continue;
            for (Flow flow : module.getFlows()) for (FlowElement element : flow.getFlowElementsNoExternalEndPoints()) {
                Endpoint used = null;
                if (element.getComponentMeta().supportsTestMailServer())
                    used = new Endpoint("SMTP", HarnessEndpointSettings.effectiveMailHost(other, element), HarnessEndpointSettings.effectiveMailPort(other, element));
                else if (element.getComponentMeta().supportsTestFtpServer()) {
                    if (HarnessEndpointSettings.effective(other, element))
                        used = other.getService(TestFtpServerService.class).listeningEndpoint();
                    else {
                        TestFtpServerConfiguration ftp = TestFtpServerConfiguration.from(element);
                        used = new Endpoint("FTP", ftp.host(), ftp.port());
                    }
                } else if ("SFTP Endpoint".equals(element.getComponentMeta().getEndpointKey())) {
                    if (HarnessEndpointSettings.effective(other, element))
                        used = other.getService(TestSftpServerService.class).listeningEndpoint();
                    else {
                        String port = element.getPropertyValueAsString("remotePort");
                        try { used = new Endpoint("SFTP", element.getPropertyValueAsString("remoteHost"), Integer.parseInt(port)); }
                        catch (NumberFormatException ignored) { /* Unresolved values are not evidence of sharing. */ }
                    }
                }
                for (Endpoint stopped : endpoints) if (stopped.matches(used))
                    result.add(new Impact(other.getName(), stopped));
            }
        }
        return result.stream().sorted(Comparator.comparing(Impact::project).thenComparing(i -> i.endpoint().label())).toList();
    }
}
