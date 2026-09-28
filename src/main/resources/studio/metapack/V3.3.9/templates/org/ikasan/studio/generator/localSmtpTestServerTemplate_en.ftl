package org.ikasan.studio.flowtests.support;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import javax.mail.Part;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/** Per-test, loopback-only SMTP inbox. Never forwards mail or starts an external process. */
public final class LocalSmtpTestServer implements AutoCloseable {
    private final GreenMail server;
    private boolean closed;

    private LocalSmtpTestServer() {
        server = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        server.withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());
    }

    public static LocalSmtpTestServer start() {
        LocalSmtpTestServer fixture = new LocalSmtpTestServer();
        try { fixture.server.start(); return fixture; }
        catch (RuntimeException | Error failure) {
            try { fixture.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public int port() { return server.getSmtp().getPort(); }
    public MimeMessage[] receivedMessages() { return server.getReceivedMessages(); }

    /** Configures the pack's email producer before flow startup, including hard-coded application settings. */
    public void configure(Object producer) throws Exception {
        Object config = producer.getClass().getMethod("getConfiguration").invoke(producer);
        Class<?> type = config.getClass();
        type.getMethod("setMailHost", String.class).invoke(config, "127.0.0.1");
        type.getMethod("setMailSmtpHost", String.class).invoke(config, "127.0.0.1");
        type.getMethod("setMailSmtpPort", int.class).invoke(config, port());
        type.getMethod("setMailTransportProtocol", String.class).invoke(config, "smtp");
        type.getMethod("setMailSmtpClass", String.class).invoke(config, (Object) null);
        type.getMethod("setPassword", String.class).invoke(config, (Object) null);
        // Extended JavaMail settings override the ordinary setters. Enforce test isolation there too.
        Map<String, String> properties = new HashMap<>();
        properties.put("mail.smtp.host", "127.0.0.1");
        properties.put("mail.smtp.port", Integer.toString(port()));
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.ssl.enable", "false");
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.starttls.required", "false");
        type.getMethod("setExtendedMailSessionProperties", Map.class).invoke(config, properties);
    }

    /** Waits for a cumulative mailbox-delivery count, then checks the latest message body. */
    public void assertBody(int deliveryCount, String expected, Duration timeout) throws Exception {
        if (deliveryCount < 1 || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("Delivery count and timeout must be positive");
        if (!server.waitForIncomingEmail(timeout.toMillis(), deliveryCount))
            throw new AssertionError("No SMTP delivery " + deliveryCount + " within " + timeout
                    + "; received " + receivedMessages().length + " messages");
        MimeMessage[] messages = receivedMessages();
        assertEquals("SMTP mailbox delivery count (multiple recipients may create multiple copies)", deliveryCount, messages.length);
        assertEquals("Received email body for delivery " + deliveryCount, expected, textBody(messages[deliveryCount - 1]));
    }

    /** Finds the text body, excluding attachments; custom MIME structures can use receivedMessages(). */
    private static String textBody(Part part) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return null;
        if (part.isMimeType("text/*")) return (String) part.getContent();
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String body = textBody(multipart.getBodyPart(i));
                if (body != null) return body;
            }
        }
        return null;
    }

    @Override public void close() {
        if (!closed) { closed = true; server.stop(); }
    }
}
