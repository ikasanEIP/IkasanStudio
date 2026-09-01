package org.ikasan.studio.ui.actions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Shared constants/probing between {@link StartTestMailServerAction}, {@link StopTestMailServerAction} and
 * {@code org.ikasan.studio.ui.intellij.TestMailServerSessionService} (a different UI subpackage, hence public
 * rather than package-private) - mirrors {@link StudioInjectClient}'s role as a shared helper between
 * SendTestMessageAction/TriggerScheduledConsumerAction. Address resolution (mailSmtpHost/mailSmtpPort, with
 * defaults) lives in the framework-independent
 * {@code org.ikasan.studio.core.model.ikasan.instance.TestMailServerLinks} instead, since the canvas's
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
}
