package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.PathManager;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailHarnessProcessRecoveryTest {
    private final Path cache = Path.of(PathManager.getSystemPath(), "ikasan-studio", "mailhog");
    private final String[] args = {"-smtp-bind-addr", "127.0.0.1:1025", "-api-bind-addr", "127.0.0.1:8025",
            "-ui-bind-addr", "127.0.0.1:8025"};

    @Test
    void requiresTheCachedBinaryAndExactHarnessAddresses() {
        String binary = cache.resolve("MailHog_linux_amd64").toString();
        assertTrue(MailHarnessProcessRecovery.matchesLaunch(cache, binary, args, "127.0.0.1", 1025));
        assertTrue(MailHarnessProcessRecovery.matchesLaunch(cache, binary, args, "localhost", 1025));
        assertFalse(MailHarnessProcessRecovery.matchesLaunch(cache, binary, args, "127.0.0.1", 2525));
        assertFalse(MailHarnessProcessRecovery.matchesLaunch(cache, "/other/MailHog_linux_amd64", args, "127.0.0.1", 1025));
        assertFalse(MailHarnessProcessRecovery.matchesLaunch(cache, cache.resolve("sh").toString(), args, "127.0.0.1", 1025));
        assertFalse(MailHarnessProcessRecovery.matchesLaunch(cache, binary, new String[]{"-smtp-bind-addr", "127.0.0.1:1025"}, "127.0.0.1", 1025));
    }

    @Test
    void recoversOnlyAUniqueDescendantOfTheProjectTerminal() {
        ProcessHandle shell = mock(ProcessHandle.class);
        when(shell.isAlive()).thenReturn(true);
        ProcessHandle harness = harness();
        when(shell.descendants()).thenAnswer(call -> Stream.of(harness));
        assertSame(harness, MailHarnessProcessRecovery.findHarness(shell, "127.0.0.1", 1025));
        ProcessHandle second = harness();
        when(shell.descendants()).thenAnswer(call -> Stream.of(harness, second));
        assertNull(MailHarnessProcessRecovery.findHarness(shell, "127.0.0.1", 1025));
        when(shell.descendants()).thenAnswer(call -> Stream.empty());
        assertNull(MailHarnessProcessRecovery.findHarness(shell, "127.0.0.1", 1025));
        assertNull(MailHarnessProcessRecovery.findHarness(null, "127.0.0.1", 1025));
        verify(harness, never()).destroy();
    }

    private ProcessHandle harness() {
        ProcessHandle process = mock(ProcessHandle.class);
        ProcessHandle.Info info = mock(ProcessHandle.Info.class);
        when(process.info()).thenReturn(info);
        when(info.command()).thenReturn(Optional.of(cache.resolve("MailHog_linux_amd64").toString()));
        when(info.arguments()).thenReturn(Optional.of(args));
        return process;
    }
}
