package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Shared address-resolution/probing logic between {@link StartTestMailServerAction} and
 * {@link StopTestMailServerAction} - mirrors {@link StudioInjectClient}'s role as a shared, non-public helper
 * between SendTestMessageAction/TriggerScheduledConsumerAction, so both test-mail-server actions stay in sync.
 */
final class TestMailServerSupport {
    // Fixed, well-known title so Start and Stop can each independently find the same Terminal tab via the
    // IDE's own tool-window state - neither action needs to remember anything in plugin memory about whether
    // (or where) an instance is running, which is exactly what makes Stop reliable even if track of it is lost.
    static final String TERMINAL_TAB_TITLE = "Test Mail Server";
    // Sensible default when the component hasn't configured mailSmtpHost/mailSmtpPort itself - matches the
    // pair this feature was validated against manually before being wired into the plugin.
    static final String DEFAULT_SMTP_HOST = "127.0.0.1";
    static final int DEFAULT_SMTP_PORT = 1025;
    // The web inbox UI is a separate listener from SMTP and isn't user-configurable on the component at all -
    // always MailHog's own conventional default, so it's never a surprise to anyone who already knows the tool.
    static final String UI_HOST = "127.0.0.1";
    static final int UI_PORT = 8025;
    // Only ever used off-EDT (inside a Task.Backgroundable in both callers).
    private static final Duration PROBE_TIMEOUT = Duration.ofMillis(300);

    private TestMailServerSupport() {}

    static String resolveSmtpHost(FlowElement flowElement) {
        String mailSmtpHost = flowElement.getPropertyValueAsString("mailSmtpHost");
        if (mailSmtpHost != null && !mailSmtpHost.isBlank()) {
            return mailSmtpHost;
        }
        // mailSmtpHost overrides mailhost when set (see EmailProducer's own help text) - fall back the same way.
        String mailhost = flowElement.getPropertyValueAsString("mailhost");
        return (mailhost != null && !mailhost.isBlank()) ? mailhost : DEFAULT_SMTP_HOST;
    }

    static int resolveSmtpPort(FlowElement flowElement) {
        Integer configured = toInteger(flowElement.getPropertyValue("mailSmtpPort"));
        return configured != null ? configured : DEFAULT_SMTP_PORT;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.valueOf(stringValue.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Best-effort: a short-timeout TCP connect attempt.
     */
    static boolean isAlreadyListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) PROBE_TIMEOUT.toMillis());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
