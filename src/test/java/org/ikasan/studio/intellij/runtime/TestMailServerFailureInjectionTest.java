package org.ikasan.studio.intellij.runtime;

import org.junit.jupiter.api.Test;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import org.ikasan.studio.ui.UiContext;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TestMailServerFailureInjectionTest {
    @Test
    void immediatelyExitedHarnessLosesOwnershipAfterStartupGrace() throws Exception {
        var project = mock(Project.class);
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        var clock = new AtomicLong(1000);
        var stopped = new AtomicBoolean();
        Process exited = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-failure-injection-invalid-option").redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(exited.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
            assertNotEquals(0, exited.exitValue());
        } finally { exited.destroyForcibly(); }
        var service = new TestMailServerSessionService(project, mock(Alarm.class), clock::get, (host, port) -> false);
        try {
            service.registerOwned("localhost", 2525, () -> stopped.set(true));
            service.probeAndUpdateState();
            assertTrue(service.isOwned("localhost", 2525));
            assertFalse(service.isListening("localhost", 2525));
            clock.addAndGet(5001);
            service.probeAndUpdateState();
            assertFalse(service.hasAnyOwned());
            assertFalse(service.stopAnyOwned());
            assertFalse(stopped.get(), "Must not stop a later external listener");
        } finally { service.dispose(); }
    }

    @Test
    void processRegisteredAfterDisposalIsStoppedImmediately() {
        var project = mock(Project.class);
        var stopped = new AtomicBoolean();
        var service = new TestMailServerSessionService(project, mock(Alarm.class), () -> 0, (host, port) -> false);
        service.dispose();
        service.registerOwned("localhost", 2525, () -> stopped.set(true));
        assertTrue(stopped.get());
        assertFalse(service.hasAnyOwned());
    }
}
