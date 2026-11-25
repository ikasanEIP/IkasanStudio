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
    void ownedEndpointSnapshotsDoNotProbeOrStopServers() {
        Project project = mock(Project.class);
        AtomicBoolean stopped = new AtomicBoolean();
        TestMailServerSessionService service = new TestMailServerSessionService(project, mock(Alarm.class), () -> 0,
                (host, port) -> { throw new AssertionError("Snapshot must not probe sockets"); });
        try {
            service.registerOwned("localhost", 1025, () -> stopped.set(true));
            assertEquals(java.util.List.of(new HarnessStopImpact.Endpoint("SMTP", "localhost", 1025)), service.ownedEndpoints());
            assertFalse(stopped.get());
            service.stopAnyOwned();
            assertTrue(service.ownedEndpoints().isEmpty());
        } finally { service.dispose(); }
    }

    @Test
    void immediatelyExitedHarnessLosesOwnershipAfterStartupGrace() throws Exception {
        Project project = mock(Project.class);
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        AtomicLong clock = new AtomicLong(1000);
        AtomicBoolean stopped = new AtomicBoolean();
        Process exited = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-failure-injection-invalid-option").redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(exited.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
            assertNotEquals(0, exited.exitValue());
        } finally { exited.destroyForcibly(); }
        TestMailServerSessionService service = new TestMailServerSessionService(project, mock(Alarm.class), clock::get, (host, port) -> false);
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
    void toolbarReadinessRequiresOwnedAndConfirmedListeningHarness() {
        Project project = mock(Project.class);
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        AtomicBoolean listening = new AtomicBoolean();
        TestMailServerSessionService service = new TestMailServerSessionService(project, mock(Alarm.class), () -> 0,
                (host, port) -> listening.get());
        try {
            assertFalse(service.hasAnyOwnedListening());
            service.registerOwned("localhost", 2525, () -> { });
            assertFalse(service.hasAnyOwnedListening(), "Starting is not confirmed listening");
            listening.set(true);
            service.probeAndUpdateState();
            assertTrue(service.hasAnyOwnedListening());
            service.stopAnyOwned();
            assertFalse(service.hasAnyOwnedListening(), "Cached listening alone does not establish ownership");
        } finally { service.dispose(); }
    }

    @Test
    void processRegisteredAfterDisposalIsStoppedImmediately() {
        Project project = mock(Project.class);
        AtomicBoolean stopped = new AtomicBoolean();
        TestMailServerSessionService service = new TestMailServerSessionService(project, mock(Alarm.class), () -> 0, (host, port) -> false);
        service.dispose();
        service.registerOwned("localhost", 2525, () -> stopped.set(true));
        assertTrue(stopped.get());
        assertFalse(service.hasAnyOwned());
    }
}
