package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarnessControlActionsTest {
    @Test
    void availableForFtpConsumerOrProducer() throws Exception {
        assertThat(HarnessControlActions.hasHarnesses(moduleWith(TestFixtures.getFtpConsumer(BASE_META_PACK)))).isTrue();
        assertThat(HarnessControlActions.hasHarnesses(moduleWith(TestFixtures.getFtpProducer(BASE_META_PACK)))).isTrue();
    }

    @Test
    void availableForEmailEndpoint() throws Exception {
        FlowElement emailProducer = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer"))
                .componentName("mail")
                .build();

        assertThat(HarnessControlActions.hasHarnesses(moduleWith(emailProducer))).isTrue();
    }

    @Test
    void unavailableWithoutSupportedEndpoints() throws Exception {
        assertThat(HarnessControlActions.hasHarnesses(moduleWith(TestFixtures.getDevNullProducer(BASE_META_PACK)))).isFalse();
        assertThat(HarnessControlActions.hasHarnesses(null)).isFalse();
    }


    @Test
    void startAndStopActionsAreIndependentlyAvailableRegardlessOfRunningState() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        Module module = moduleWith(TestFixtures.getFtpConsumer(BASE_META_PACK));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        HarnessControlActions actions = new HarnessControlActions(project);

        assertThat(actions.isAvailable()).isTrue();
        assertThat(actions.startAction()).isNotNull();
        assertThat(actions.stopAction()).isNotNull();
    }

    @Test
    void runningIndicatorsUseOnlyThisProjectsCachedServiceState() {
        Project project = mock(Project.class);
        TestFtpServerService ftp = mock(TestFtpServerService.class);
        TestSftpServerService sftp = mock(TestSftpServerService.class);
        TestMailServerSessionService mail = mock(TestMailServerSessionService.class);
        when(project.getService(TestFtpServerService.class)).thenReturn(ftp);
        when(project.getService(TestSftpServerService.class)).thenReturn(sftp);
        when(project.getService(TestMailServerSessionService.class)).thenReturn(mail);
        HarnessControlActions actions = new HarnessControlActions(project);
        assertThat(actions.runningHarnesses()).isEmpty();
        when(ftp.isRunning()).thenReturn(true);
        when(mail.hasAnyOwnedListening()).thenReturn(true);
        assertThat(actions.runningHarnesses()).containsExactly("FTP", "SMTP");
        when(sftp.isRunning()).thenReturn(true);
        assertThat(actions.runningHarnesses()).containsExactly("FTP", "SFTP", "SMTP");
        when(ftp.isRunning()).thenReturn(false);
        when(mail.hasAnyOwnedListening()).thenReturn(false);
        assertThat(actions.runningHarnesses()).containsExactly("SFTP");
        when(project.isDisposed()).thenReturn(true);
        assertThat(actions.runningHarnesses()).isEmpty();
    }

    @Test
    void sharedMailAvailabilityLightsIndicatorWithoutTakingOwnership() throws Exception {
        Project project = mock(Project.class); UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        FlowElement email = FlowElement.flowElementBuilder().componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK,
                "Email Producer")).componentName("Mail").build();
        email.setPropertyValue("mailSmtpHost", "localhost"); email.setPropertyValue("mailSmtpPort", 2525);
        when(context.getIkasanModule()).thenReturn(moduleWith(email));
        TestMailServerSessionService mail = mock(TestMailServerSessionService.class); when(project.getService(TestMailServerSessionService.class)).thenReturn(mail);
        HarnessControlActions actions = new HarnessControlActions(project);
        when(mail.isListening("127.0.0.1", 1025)).thenReturn(true);
        assertThat(actions.runningHarnesses()).isEmpty(); // An unrelated/default listener is not this component's endpoint.
        when(mail.isListening("localhost", 2525)).thenReturn(true);
        assertThat(actions.runningHarnesses()).containsExactly(StudioBundle.message("harness.smtp.sharedAvailable"));
        assertThat(actions.status().owned()).isEmpty();
        assertThat(actions.status().shared()).hasSize(1);
        Mockito.verify(mail, Mockito.never()).registerOwned(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any());
        when(mail.isListening("localhost", 2525)).thenReturn(false);
        assertThat(actions.runningHarnesses()).isEmpty();
        HarnessOverrideService overrides = mock(HarnessOverrideService.class);
        when(project.getService(HarnessOverrideService.class)).thenReturn(overrides);
        when(overrides.hasRunningLaunches()).thenReturn(true); when(overrides.isActive(email)).thenReturn(true);
        assertThat(actions.runningHarnesses()).containsExactly(StudioBundle.message("harness.smtp.sharedAvailable"));
        assertThat(actions.status().owned()).isEmpty();
        assertThat(actions.status().shared()).hasSize(1);
    }

    @Test
    void unavailableWhenProjectIsDisposed() {
        Project project = mock(Project.class);
        when(project.isDisposed()).thenReturn(true);
        HarnessControlActions actions = new HarnessControlActions(project);

        assertThat(actions.isAvailable()).isFalse();
    }

    @Test
    void selectsMailHarnessBinariesForSupportedDesktopPlatforms() throws Exception {
        assertThat(StartTestMailServerAction.mailHogAssetName("Windows 11", "amd64"))
                .isEqualTo("MailHog_windows_amd64.exe");
        assertThat(StartTestMailServerAction.mailHogAssetName("Mac OS X", "aarch64"))
                .isEqualTo("MailHog_darwin_amd64");
        assertThat(StartTestMailServerAction.mailHogAssetName("Linux", "amd64"))
                .isEqualTo("MailHog_linux_amd64");
        assertThat(StartTestMailServerAction.mailHogAssetName("Linux", "arm"))
                .isEqualTo("MailHog_linux_arm");
    }

    @Test
    void mailHarnessOwnershipIsProjectScopedIdempotentAndNotRestoredAfterRestart() {
        Project project = mock(Project.class);
        AtomicInteger stops = new AtomicInteger();
        TestMailServerSessionService firstSession = new TestMailServerSessionService(project);
        TestMailServerSessionService restartedSession = new TestMailServerSessionService(project);
        try {
            firstSession.registerOwned("127.0.0.1", 1025, stops::incrementAndGet);

            assertThat(firstSession.hasAnyOwned()).isTrue();
            assertThat(restartedSession.hasAnyOwned()).isFalse();
            assertThat(restartedSession.stopAnyOwned()).isFalse();
            assertThat(stops).hasValue(0);
            assertThat(firstSession.stopAnyOwned()).isTrue();
            assertThat(firstSession.stopAnyOwned()).isFalse();
            assertThat(stops).hasValue(1);
        } finally {
            firstSession.dispose();
            restartedSession.dispose();
        }
    }

    private static Module moduleWith(FlowElement element) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        if (element.getComponentMeta().isConsumer()) {
            flow.setConsumer(element);
        } else {
            flow.getFlowRoute().getFlowElements().add(element);
        }
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flow));
    }
}
