package org.ikasan.studio.ui.actions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Optional;

/**
 * Shared constants/probing between {@link StartTestMailServerAction}, {@link StopTestMailServerAction} and
 * {@code org.ikasan.studio.intellij.runtime.TestMailServerSessionService} (a different UI subpackage, hence public
 * rather than package-private) - mirrors {@link org.ikasan.studio.integration.ikasan.StudioInjectClient}'s role as a shared helper between
 * SendTestMessageAction/TriggerScheduledConsumerAction. Address resolution (mailSmtpHost/mailSmtpPort, with
 * defaults) lives in the framework-independent
 * {@code org.ikasan.studio.core.model.analysis.TestMailServerLinks} instead, since the canvas's
 * shared-node grouping logic needs it too and that class has no UI dependency to inherit.
 */
public final class TestMailServerSupport {
    // Fixed, well-known title so Start and Stop can each independently find the same Terminal tab via the
    // IDE's own tool-window state - neither action needs to remember anything in plugin memory about whether
    // (or where) an instance is running, which is exactly what makes Stop reliable even if track of it is lost.
    public static final String TERMINAL_TAB_TITLE = "Test Mail Server";
    // The web inbox UI is a separate listener from SMTP and isn't user-configurable on the component at all -
    // always MailHog's own conventional default, so it's never a surprise to anyone who already knows the tool.
    public static final String UI_HOST = "127.0.0.1";
    public static final int UI_PORT = 8025;
    // Only ever used off-EDT (inside a Task.Backgroundable in both callers, or the session service's own
    // pooled-thread poll).
    private static final Duration PROBE_TIMEOUT = Duration.ofMillis(300);
    // MailHog's own CLI flag for binding its SMTP listener - the one piece of launch-command syntax that both
    // StartTestMailServerAction (builds the launch command) and StopTestMailServerAction (needs to recognise a
    // Studio-launched process from a different IDE window, where no Terminal tab is visible to it - see
    // StopTestMailServerAction's class javadoc) need to agree on. Centralised here, alongside the rest of the
    // MailHog-specific knowledge, so swapping the underlying tool only means changing this file plus
    // StartTestMailServerAction itself - StopTestMailServerAction stays tool-agnostic.
    private static final String SMTP_BIND_ADDR_FLAG = "-smtp-bind-addr";

    private TestMailServerSupport() {}

    /**
     * Best-effort: a short-timeout TCP connect attempt.
     */
    public static boolean isAlreadyListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) PROBE_TIMEOUT.toMillis());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** The exact launch-command argument StartTestMailServerAction uses to bind the SMTP listener. */
    public static String smtpBindAddrArgument(String smtpAddress) {
        return SMTP_BIND_ADDR_FLAG + " " + smtpAddress;
    }

    /**
     * Finds the OS process (if any) that was launched with {@link #smtpBindAddrArgument} for the given SMTP
     * address - real OS state, not scoped to any IntelliJ window, so it finds an instance started from a
     * different Studio window while leaving a genuinely external process (one Studio never launched) alone.
     * Command-line introspection isn't available on every platform/JVM, in which case this returns empty the
     * same as "no match found" - callers should treat that the same as a genuinely external process.
     */
    public static Optional<ProcessHandle> findLaunchedProcess(String smtpAddress) {
        String marker = smtpBindAddrArgument(smtpAddress);
        return ProcessHandle.allProcesses()
                .filter(handle -> handle.info().commandLine().map(cmd -> cmd.contains(marker)).orElse(false))
                .findFirst();
    }

    /**
     * Finds any currently running OS process launched with {@link #smtpBindAddrArgument}, regardless of which
     * address it was bound to. StartTestMailServerAction refuses to launch a second instance while any other
     * Studio-launched test mail server is already running (its web inbox port is fixed and shared - see
     * {@link #UI_PORT}), so there is by design at most one such process system-wide at any time - this is
     * therefore a reliable "is the harness actually running" signal even after the user has edited a
     * component's mailSmtpHost/mailSmtpPort *after* starting it, when the live process is still bound to
     * whatever address it actually started with, not the (now different) currently-configured one.
     */
    public static Optional<ProcessHandle> findAnyLaunchedProcess() {
        return ProcessHandle.allProcesses()
                .filter(handle -> handle.info().commandLine().map(cmd -> cmd.contains(SMTP_BIND_ADDR_FLAG)).orElse(false))
                .findFirst();
    }
}
