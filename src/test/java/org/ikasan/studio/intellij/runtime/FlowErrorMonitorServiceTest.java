package org.ikasan.studio.intellij.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowErrorMonitorServiceTest {
    @Test
    void discardsResponseDeliveredAfterStopOrDisposal() throws Exception {
        for (boolean closeProject : new boolean[] {false, true}) {
            var project = org.mockito.Mockito.mock(com.intellij.openapi.project.Project.class);
            var context = org.mockito.Mockito.mock(org.ikasan.studio.ui.UiContext.class);
            var module = org.mockito.Mockito.mock(org.ikasan.studio.core.model.ikasan.instance.Module.class);
            org.mockito.Mockito.when(project.getService(org.ikasan.studio.ui.UiContext.class)).thenReturn(context);
            org.mockito.Mockito.when(context.getIkasanModule()).thenReturn(module);
            org.mockito.Mockito.when(module.getIdentity()).thenReturn("test");
            org.mockito.Mockito.when(module.getPort()).thenReturn("8080");
            var entered = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            var probe = new FlowErrorMonitorService.ModuleProbe() {
                public java.util.Map<String, String> states(org.ikasan.studio.core.model.ikasan.instance.Module ignored) throws Exception {
                    entered.countDown();
                    if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Test timed out");
                    return java.util.Map.of("flow", "running");
                }
                public org.ikasan.studio.integration.ikasan.ModuleControlClient.ErrorDetails details(
                        org.ikasan.studio.core.model.ikasan.instance.Module ignored, String flow) { return null; }
            };
            var service = new FlowErrorMonitorService(project, org.mockito.Mockito.mock(com.intellij.util.Alarm.class), probe);
            var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
            try {
                service.moduleProcessStarted();
                var poll = pool.submit(service::pollAndUpdateState);
                org.junit.jupiter.api.Assertions.assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
                var heartbeat = new java.util.concurrent.atomic.AtomicBoolean();
                javax.swing.SwingUtilities.invokeAndWait(() -> heartbeat.set(true));
                org.junit.jupiter.api.Assertions.assertTrue(heartbeat.get());
                if (closeProject) {
                    org.mockito.Mockito.when(project.isDisposed()).thenReturn(true);
                    service.dispose();
                } else service.moduleProcessStopped();
                release.countDown();
                poll.get(5, java.util.concurrent.TimeUnit.SECONDS);
                org.junit.jupiter.api.Assertions.assertNull(service.getFlowStatuses().getRawState("flow"));
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
