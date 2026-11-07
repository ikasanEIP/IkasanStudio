package org.ikasan.studio.intellij.execution;

import com.intellij.execution.*;
import com.intellij.execution.application.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.analysis.HarnessOverrides;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.UiContext;

import java.util.ArrayList;
import java.util.Set;
import static org.mockito.Mockito.*;

public class HarnessOverrideServiceHeavyTest extends HeavyPlatformTestCase {
    public void testDebugVariantsPreserveExistingSavedCombinations() {
        var service = getProject().getService(IkasanRunConfigurationService.class);
        var first = service.findOrCreateConfiguration(getModule(), true, Set.of("flow.email"));
        var second = service.findOrCreateConfiguration(getModule(), true, Set.of("flow.sftp"));
        assertNotSame(first, second);
        assertSame(first, service.findOrCreateConfiguration(getModule(), true, Set.of("flow.email")));
        assertEquals(Set.of("flow.email"), HarnessOverrideService.explicitSelection(
                ((ApplicationConfiguration) first.getConfiguration()).getVMParameters()));
        assertEquals(Set.of("flow.sftp"), HarnessOverrideService.explicitSelection(
                ((ApplicationConfiguration) second.getConfiguration()).getVMParameters()));
    }

    public void testChangingNextLaunchDoesNotChangeRunningSnapshotOrComponentValues() throws Exception {
        var producer = TestFixtures.getEmailProducer("V4.1.6");
        producer.setComponentName("Send email");
        producer.setPropertyValue("mailSmtpHost", "dev.example.com");
        producer.setPropertyValue("mailSmtpPort", 465);
        var flow = new Flow("V4.1.6");
        flow.setName("Orders");
        producer.setContainingFlow(flow);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(producer);
        var module = TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", new ArrayList<>(java.util.List.of(flow)));
        getProject().getService(UiContext.class).setIkasanModule(module);
        var manager = RunManager.getInstance(getProject());
        var settings = manager.createConfiguration("Harness selection test",
                ApplicationConfigurationType.getInstance().getConfigurationFactories()[0]);
        var app = (ApplicationConfiguration) settings.getConfiguration();
        app.setMainClassName("org.ikasan.studio.boot.Application");
        manager.addConfiguration(settings); manager.setSelectedConfiguration(settings);
        var service = getProject().getService(HarnessOverrideService.class);
        String id = HarnessOverrides.id(producer);
        service.saveSelection(app, Set.of(id));
        assertTrue(service.isEnabled(producer));
        app.putUserData(HarnessOverrideService.LAUNCH_SELECTION, Set.of(id));
        var environment = mock(ExecutionEnvironment.class);
        when(environment.getRunProfile()).thenReturn(app);
        var handler = mock(ProcessHandler.class);
        var listener = getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC);
        listener.processStarted("Run", environment, handler);
        assertTrue(service.isActive(producer));
        service.saveSelection(app, Set.of());
        assertFalse(service.isEnabled(producer));
        assertTrue(service.isActive(producer));
        assertEquals("dev.example.com", producer.getPropertyValue("mailSmtpHost"));
        assertEquals(465, producer.getPropertyValue("mailSmtpPort"));
        producer.setComponentName("Renamed after launch");
        assertTrue(service.describe(service.activeSelection()).contains("Send email"));
        listener.processTerminated("Run", environment, handler, 0);
        assertFalse(service.isActive(producer));
    }
}
