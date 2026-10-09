package org.ikasan.studio.flowtests.support.utils;

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
    private final java.util.List<String> verifiedBodies = new java.util.ArrayList<>();

    private LocalSmtpTestServer() {
        server = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        server.withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());
    }

    /**
     * Starts a fresh non-forwarding SMTP inbox on an allocated loopback port.
     * Use try-with-resources when called directly; shared test support closes it with Spring.
     * @return a running server with an empty inbox
     * @throws RuntimeException if binding or startup fails; partial startup is cleaned up
     */
    public static LocalSmtpTestServer start() {
        LocalSmtpTestServer fixture = new LocalSmtpTestServer();
        try { fixture.server.start(); return fixture; }
        catch (RuntimeException | Error failure) {
            try { fixture.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /** @return the allocated SMTP port; valid while this fixture is running */
    public int port() { return server.getSmtp().getPort(); }
    /**
     * Returns currently captured mailbox messages for subject, recipient and attachment checks.
     * This method does not wait. Multiple recipients can produce multiple mailbox copies.
     * @return captured messages; inspect after waiting for the expected delivery
     */
    public MimeMessage[] receivedMessages() { return server.getReceivedMessages(); }

    /**
     * Redirects an Ikasan email producer to this inbox before its managed resource starts.
     * Replaces host, port, transport, authentication and extended mail-session settings with
     * plain local SMTP settings. Preserves recipients, subject, format and payload mapping.
     * Changes only this in-memory test producer, never the model or application source.
     * @param producer component exposing the pack's email {@code getConfiguration()} contract
     * @throws Exception if the producer does not support that configuration contract
     */
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

    /**
     * Waits for a cumulative mailbox-delivery count and compares the latest decoded text body.
     * For a single-recipient scenario use counts 1 and 2 across two batches on the same server.
     * Attachments are skipped; the first non-attachment text MIME part is compared exactly.
     * Custom multipart or multiple-recipient scenarios should inspect {@link #receivedMessages()}.
     * @param deliveryCount total mailbox copies expected since server startup, not this batch's size
     * @param expected expected decoded body, including whitespace
     * @param timeout positive maximum wait; returns as soon as the expected count is available
     * @throws AssertionError on timeout, unexpected count or different body
     * @throws Exception if captured MIME content cannot be decoded
     */
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

    /**
     * Checks this batch's mailbox bodies plus all previously verified batches, ignoring mailbox order.
     * Include one entry per recipient copy (for two recipients, use List.of(expected, expected)).
     * Earlier messages remain in the inbox; callers need not repeat earlier expectations.
     * Decodes the first non-attachment text MIME part. Does not check subjects or recipient addresses;
     * use receivedMessages() for those assertions. Extra messages present at the check fail.
     * @param expectedBodies bodies delivered by this batch, including whitespace and duplicate copies
     * @param timeout positive maximum wait for the cumulative delivery count
     */
    public void assertBatchBodies(java.util.List<String> expectedBodies, Duration timeout) throws Exception {
        if (timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("Timeout must be positive");
        var cumulative = new java.util.ArrayList<>(verifiedBodies);
        cumulative.addAll(java.util.List.copyOf(expectedBodies));
        if (!cumulative.isEmpty() && !server.waitForIncomingEmail(timeout.toMillis(), cumulative.size()))
            throw new AssertionError("Expected " + cumulative.size() + " SMTP mailbox deliveries within " + timeout
                    + "; received " + receivedMessages().length);
        MimeMessage[] messages = receivedMessages();
        assertEquals("SMTP mailbox delivery count", cumulative.size(), messages.length);
        var actual = new java.util.ArrayList<String>();
        for (MimeMessage message : messages) actual.add(textBody(message));
        java.util.Collections.sort(cumulative);
        actual.sort(java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        assertEquals("SMTP mailbox bodies across verified batches", cumulative, actual);
        verifiedBodies.clear();
        verifiedBodies.addAll(cumulative);
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

    /** Stops the owned server and releases its port. Repeated calls have no effect. */
    @Override public void close() {
        if (!closed) { closed = true; server.stop(); }
    }
}
