package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

/** Resolves local FTP test-server settings from an FTP Consumer. */
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
