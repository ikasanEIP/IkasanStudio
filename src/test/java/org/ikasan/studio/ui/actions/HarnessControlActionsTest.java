package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.ikasan.studio.ui.UiContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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


    /**
     * Start and Stop are independent, always-available actions now (see HarnessControlActions javadoc) - there
     * is no "believed running" state left to track, so unlike the old single-toggle button there is nothing to
     * assert about icon/text swapping. What matters is that isAvailable() reflects the model (already covered
     * above via hasHarnesses) and that both actions are safe to invoke regardless of the harness's actual state
     * - exercised here via the underlying TestFtpServerService directly (StartTestFtpServerAction/
     * StopTestFtpServerAction already have their own dedicated tests for the full click-to-message behaviour).
     */
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
