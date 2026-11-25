package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.intellij.runtime.*;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.intellij.runtime.HarnessStopImpact.Endpoint;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;

class HarnessStopWarningTest {
    Project project(String name, FlowElement element) throws Exception {
        Project project = mock(Project.class); when(project.getName()).thenReturn(name);
        UiContext context = mock(UiContext.class); when(project.getService(UiContext.class)).thenReturn(context);
        Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).build(); element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        if (element.getComponentMeta().isConsumer()) flow.setConsumer(element);
        else flow.getFlowRoute().getFlowElements().add(element);
        when(context.getIkasanModule()).thenReturn(TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow)));
        return project;
    }
    FlowElement email(int port) throws Exception {
        FlowElement element = FlowElement.flowElementBuilder().componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(TestFixtures.BASE_META_PACK,
                "Email Producer")).componentName("Mail").build();
        element.setPropertyValue("mailSmtpHost", "localhost"); element.setPropertyValue("mailSmtpPort", port); return element;
    }
    @Test void warnsAboutSharedMailAndLetsTheDeveloperCancel() throws Exception {
        Project owner = mock(Project.class); Project other = project("Project B", email(1025));
        List<Endpoint> stopped = List.of(new HarnessStopImpact.Endpoint("SMTP", "127.0.0.1", 1025));
        AtomicInteger calls = new AtomicInteger();
        assertThat(HarnessStopWarning.confirm(owner, stopped, List.of(owner, other), impact -> {
            calls.incrementAndGet(); assertThat(impact).containsExactly(new HarnessStopImpact.Impact("Project B", stopped.get(0))); return false;
        })).isFalse();
        assertThat(calls.get()).isOne();
        assertThat(HarnessStopWarning.confirm(owner, stopped, List.of(other), impact -> true)).isTrue();
    }
    @Test void ignoresOwnerDisposedProjectsAndDifferentPorts() throws Exception {
        Project owner = project("Owner", email(1025)); Project other = project("Independent", email(2525));
        Project closed = project("Closed", email(1025)); when(closed.isDisposed()).thenReturn(true);
        assertThat(HarnessStopWarning.confirm(owner, List.of(new HarnessStopImpact.Endpoint("SMTP", "localhost", 1025)),
                List.of(owner, other, closed), impact -> { throw new AssertionError("No sharing"); })).isTrue();
    }
    @Test void independentFtpOverridesDoNotWarnButExplicitSharedAddressesDo() throws Exception {
        FlowElement element = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        element.setPropertyValue("remoteHost", "localhost"); element.setPropertyValue("remotePort", 3121);
        Project other = project("FTP project", element); Project owner = mock(Project.class);
        List<Endpoint> stopped = List.of(new HarnessStopImpact.Endpoint("FTP", "127.0.0.1", 3121));
        assertThat(HarnessStopImpact.affected(owner, List.of(other), stopped)).hasSize(1);
        HarnessOverrideService overrides = mock(HarnessOverrideService.class); when(other.getService(HarnessOverrideService.class)).thenReturn(overrides);
        when(overrides.hasRunningLaunches()).thenReturn(true); when(overrides.isActive(element)).thenReturn(true);
        TestFtpServerService service = mock(TestFtpServerService.class); when(other.getService(TestFtpServerService.class)).thenReturn(service);
        when(service.listeningEndpoint()).thenReturn(new HarnessStopImpact.Endpoint("FTP", "127.0.0.1", 4121));
        assertThat(HarnessStopImpact.affected(owner, List.of(other), stopped)).isEmpty();
        verify(service, never()).localConfiguration();
    }
    @Test void runningMailOverridesTakePrecedenceOverConfiguredAddress() throws Exception {
        FlowElement element = email(587); Project other = project("Running", element); Project owner = mock(Project.class);
        HarnessOverrideService overrides = mock(HarnessOverrideService.class); when(other.getService(HarnessOverrideService.class)).thenReturn(overrides);
        when(overrides.hasRunningLaunches()).thenReturn(true); when(overrides.isActive(element)).thenReturn(true);
        assertThat(HarnessStopImpact.affected(owner, List.of(other), List.of(new HarnessStopImpact.Endpoint("SMTP", "::1", 1025))))
                .hasSize(1);
    }
    @Test void cancellingAggregateStopLeavesEveryOwnedServerRunning() throws Exception {
        Project owner = project("Owner", TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK));
        TestFtpServerService ftp = mock(TestFtpServerService.class); TestSftpServerService sftp = mock(TestSftpServerService.class); TestMailServerSessionService mail = mock(TestMailServerSessionService.class);
        when(owner.getService(TestFtpServerService.class)).thenReturn(ftp);
        when(owner.getService(TestSftpServerService.class)).thenReturn(sftp);
        when(owner.getService(TestMailServerSessionService.class)).thenReturn(mail);
        when(ftp.listeningEndpoint()).thenReturn(new HarnessStopImpact.Endpoint("FTP", "localhost", 3121));
        when(sftp.listeningEndpoint()).thenReturn(new HarnessStopImpact.Endpoint("SFTP", "localhost", 3122));
        when(mail.ownedEndpoints()).thenReturn(List.of(new HarnessStopImpact.Endpoint("SMTP", "localhost", 1025)));
        AtomicInteger decisions = new AtomicInteger();
        HarnessControlActions actions = new HarnessControlActions(owner, endpoints -> {
            assertThat(endpoints).hasSize(3); decisions.incrementAndGet(); return false;
        });
        actions.stopAction().actionPerformed(new java.awt.event.ActionEvent(this, 0, "stop"));
        assertThat(decisions.get()).isOne();
        verify(ftp, never()).stop(); verify(sftp, never()).stop(); verify(mail, never()).stopAnyOwned();
    }

    @Test void kindsAndUnallocatedPortsDoNotMatch() {
        Endpoint smtp = new HarnessStopImpact.Endpoint("SMTP", "localhost", 1025);
        assertThat(smtp.matches(new HarnessStopImpact.Endpoint("FTP", "127.0.0.1", 1025))).isFalse();
        assertThat(smtp.matches(new HarnessStopImpact.Endpoint("SMTP", "[::1]", 1025))).isTrue();
        assertThat(new HarnessStopImpact.Endpoint("FTP", "localhost", 0).matches(new HarnessStopImpact.Endpoint("FTP", "localhost", 0))).isFalse();
    }
}
