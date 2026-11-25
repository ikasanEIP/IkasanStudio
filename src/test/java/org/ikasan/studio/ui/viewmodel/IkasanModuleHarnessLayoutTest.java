package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class IkasanModuleHarnessLayoutTest {
    @Test void ftpReservationUsesTheActiveOverrideAddressRatherThanTheSavedRemoteAddress() throws Exception {
        Project project = mock(Project.class);
        TestFtpServerService ftp = mock(TestFtpServerService.class);
        HarnessOverrideService selections = mock(HarnessOverrideService.class);
        when(project.getService(TestFtpServerService.class)).thenReturn(ftp);
        when(project.getService(TestSftpServerService.class)).thenReturn(mock(TestSftpServerService.class));
        when(project.getService(HarnessOverrideService.class)).thenReturn(selections);
        FlowElement consumer = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        Module module = module(consumer);
        TestFtpServerConfiguration local = new TestFtpServerConfiguration("127.0.0.1", 43123, "test", "test");
        when(ftp.isRunning()).thenReturn(true);
        when(ftp.localConfiguration()).thenReturn(local);
        when(ftp.isRunningAt(local)).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isFalse();
        when(selections.isEnabled(consumer)).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isTrue();
        when(selections.hasRunningLaunches()).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isFalse();
        when(selections.isActive(consumer)).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isTrue();
        when(ftp.isRunning()).thenReturn(false);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isFalse();
    }

    @Test void sftpReservationRequiresARunningSelectedConsumer() throws Exception {
        Project project = mock(Project.class);
        TestSftpServerService sftp = mock(TestSftpServerService.class);
        HarnessOverrideService selections = mock(HarnessOverrideService.class);
        when(project.getService(TestFtpServerService.class)).thenReturn(mock(TestFtpServerService.class));
        when(project.getService(TestSftpServerService.class)).thenReturn(sftp);
        when(project.getService(HarnessOverrideService.class)).thenReturn(selections);
        FlowElement consumer = TestFixtures.getSftpConsumer(TestFixtures.BASE_META_PACK);
        Module module = module(consumer);
        when(selections.isEnabled(consumer)).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isFalse();
        when(sftp.isRunning()).thenReturn(true);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isTrue();
        when(selections.isEnabled(consumer)).thenReturn(false);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module)).isFalse();
        FlowElement producer = TestFixtures.getFtpProducer(TestFixtures.BASE_META_PACK);
        assertThat(IkasanModuleViewHandler.hasVisibleLeftFileServer(project, module(producer))).isFalse();
    }

    private static org.ikasan.studio.core.model.ikasan.instance.Module module(FlowElement element) throws Exception {
        Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("Harness layout").build();
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        if (element.getComponentMeta().isConsumer()) flow.setConsumer(element);
        else flow.getFlowRoute().getFlowElements().add(element);
        return TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
    }
}
