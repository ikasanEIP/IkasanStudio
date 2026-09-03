package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

/**
 * Resolves local FTP test-server settings from any component that {@code ComponentMeta#supportsTestFtpServer()} -
 * the FTP Consumer (which reads files from it) and the FTP Producer (which delivers files to it) both expose the
 * same remoteHost/remotePort/username/password properties, so one resolver serves both.
 */
public record TestFtpServerConfiguration(String host, int port, String username, String password) {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 2121;
    public static final String DEFAULT_USERNAME = "ikasan";
    public static final String DEFAULT_PASSWORD = "ikasan";

    public static TestFtpServerConfiguration from(FlowElement element) {
        return new TestFtpServerConfiguration(text(element, "remoteHost", DEFAULT_HOST),
                number(element, "remotePort", DEFAULT_PORT), text(element, "username", DEFAULT_USERNAME),
                text(element, "password", DEFAULT_PASSWORD));
    }
    public String address() { return host + ":" + port; }

    /**
     * True if this and {@code other} name the same FTP endpoint, i.e. the same port on the same host, with
     * loopback spellings treated as equal - a Consumer configured for "127.0.0.1" and a Producer configured for
     * "localhost" are pointing at the same running test server and must link to the same canvas node.
     * -
     * Credentials are deliberately NOT part of this comparison: it answers "is this component addressing that
     * server", which is what the canvas draws and what the Start/Stop/Show actions gate on. A credential
     * mismatch is a genuine runtime authentication failure, not a reason to pretend the component points
     * somewhere else.
     * -
     * Purely string-based on purpose - this is called from the canvas paint loop, so it must never perform a
     * name lookup (see {@link #isLoopback}).
     */
    public boolean sameAddressAs(TestFtpServerConfiguration other) {
        return other != null && port == other.port && sameHost(host, other.host);
    }

    private static boolean sameHost(String left, String right) {
        String normalisedLeft = normaliseHost(left);
        String normalisedRight = normaliseHost(right);
        if (isLoopback(normalisedLeft) && isLoopback(normalisedRight)) {
            return true;
        }
        return normalisedLeft.equals(normalisedRight);
    }

    private static String normaliseHost(String host) {
        if (host == null) {
            return "";
        }
        String normalised = host.trim().toLowerCase();
        // A literal IPv6 host may be written bracketed (e.g. "[::1]") - compare on the address itself.
        if (normalised.startsWith("[") && normalised.endsWith("]") && normalised.length() > 2) {
            normalised = normalised.substring(1, normalised.length() - 1);
        }
        return normalised;
    }

    /**
     * Recognises the loopback spellings by inspection rather than resolving them, since this runs on the EDT
     * inside the canvas paint loop where a name lookup would be a blocking network call. That covers every
     * spelling the embedded test server can actually be bound to - it refuses to start on anything that isn't
     * already a loopback address (see {@code TestFtpServerService#isLocalHost}, which does resolve, but only
     * once, off the paint path).
     */
    private static boolean isLoopback(String normalisedHost) {
        if (normalisedHost.equals("localhost") || normalisedHost.equals("::1") || normalisedHost.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        // The whole 127.0.0.0/8 block is loopback, not just 127.0.0.1.
        return normalisedHost.startsWith("127.");
    }

    private static String text(FlowElement element, String property, String fallback) {
        Object rawValue = element.getPropertyValue(property);
        if (rawValue == null) return fallback;
        String value = rawValue.toString();
        return value.isBlank() ? fallback : value.trim();
    }
    private static int number(FlowElement element, String property, int fallback) {
        Object value = element.getPropertyValue(property);
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) {
            try { return Integer.parseInt(text.trim()); } catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }
}
