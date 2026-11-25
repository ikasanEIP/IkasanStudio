package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.ModuleControlClient.ErrorDetails;
import org.ikasan.studio.intellij.runtime.FlowErrorMonitorService.ModuleProbe;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class FlowErrorMonitorServiceTest {
    @Test
    void discardsResponseDeliveredAfterStopOrDisposal() throws Exception {
        for (boolean closeProject : new boolean[] {false, true}) {
            Project project = Mockito.mock(Project.class);
            UiContext context = Mockito.mock(UiContext.class);
            Module module = Mockito.mock(Module.class);
            Mockito.when(project.getService(UiContext.class)).thenReturn(context);
            Mockito.when(context.getIkasanModule()).thenReturn(module);
            Mockito.when(module.getIdentity()).thenReturn("test");
            Mockito.when(module.getPort()).thenReturn("8080");
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ModuleProbe probe = new FlowErrorMonitorService.ModuleProbe() {
                public Map<String, String> states(Module ignored) throws Exception {
                    entered.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Test timed out");
                    return Map.of("flow", "running");
                }
                public ErrorDetails details(
                        Module ignored, String flow) { return null; }
            };
            FlowErrorMonitorService service = new FlowErrorMonitorService(project, Mockito.mock(Alarm.class), probe);
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                service.moduleProcessStarted();
                Future<?> poll = pool.submit(service::pollAndUpdateState);
                Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
                AtomicBoolean heartbeat = new AtomicBoolean();
                SwingUtilities.invokeAndWait(() -> heartbeat.set(true));
                Assertions.assertTrue(heartbeat.get());
                if (closeProject) {
                    Mockito.when(project.isDisposed()).thenReturn(true);
                    service.dispose();
                } else service.moduleProcessStopped();
                release.countDown();
                poll.get(5, TimeUnit.SECONDS);
                Assertions.assertNull(service.getFlowStatuses().getRawState("flow"));
            } finally { release.countDown(); service.dispose(); pool.shutdownNow(); }
        }
    }

    // The last case is statically deducible from the other three (shouldPoll is a plain &&), but is kept to
    // spell out the full 2x2 truth table for this gate explicitly rather than leaving it implied.
    @SuppressWarnings("ConstantValue")
    @Test
    void pollsOnlyWhenMonitoringIsEnabledAndModuleProcessIsRunning() {
        assertThat(FlowErrorMonitorService.shouldPoll(true, true)).isTrue();
        assertThat(FlowErrorMonitorService.shouldPoll(true, false)).isFalse();
        assertThat(FlowErrorMonitorService.shouldPoll(false, true)).isFalse();
        assertThat(FlowErrorMonitorService.shouldPoll(false, false)).isFalse();
    }
}
