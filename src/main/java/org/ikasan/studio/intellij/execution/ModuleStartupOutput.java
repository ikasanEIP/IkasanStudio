package org.ikasan.studio.intellij.execution;

import java.util.Locale;
import java.util.regex.Pattern;

/** Bounded startup evidence only. Never exposes or logs captured application output. */
final class ModuleStartupOutput {
    private static final int WINDOW = 8192;
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern PORT = Pattern.compile(
            "(?i)\\bport\\s+([0-9]{1,5})\\s+(?:(?:was|is)\\s+)?already\\s+in\\s+use\\b");
    record Failure(Integer port, boolean prematureShutdown) {
        Failure(Integer port) { this(port, false); }
    }
    private final StringBuilder tail = new StringBuilder();
    private boolean failureBanner;
    private boolean webFailure;
    private boolean ready;
    private boolean prematureShutdown;
    private Integer port;

    synchronized Failure accept(String text) {
        for (int offset = 0; offset < text.length(); offset += 1024) {
            tail.append(text, offset, Math.min(text.length(), offset + 1024));
            String normalized = ANSI.matcher(tail).replaceAll("");
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!failureBanner && !webFailure && !prematureShutdown && lower.contains("started application in ")) ready = true;
            failureBanner |= normalized.contains("APPLICATION FAILED TO START")
                    || lower.contains("springapplication") && lower.contains("application run failed");
            webFailure |= lower.contains("web server failed to start") || normalized.contains("PortInUseException");
            // Ikasan can begin context cleanup before Spring reports its terminal failure.
            // Remaining listener threads may prevent that final report and process exit.
            if (!ready && lower.contains("moduleinitialisationserviceimpl")
                    && (lower.contains("shutting down quartz scheduler with bean name:")
                    || lower.contains("shutting down monitor with bean name:"))) prematureShutdown = true;
            if (failureBanner || webFailure || prematureShutdown) ready = false;
            if (webFailure && port == null) {
                var match = PORT.matcher(normalized);
                while (match.find()) {
                    int number = Integer.parseInt(match.group(1));
                    if (number > 0 && number <= 65535) { port = number; break; }
                }
            }
            if (tail.length() > WINDOW) tail.delete(0, tail.length() - WINDOW / 2);
        }
        return failure();
    }

    synchronized Failure failure() {
        if (failureBanner || webFailure) return new Failure(port);
        return prematureShutdown ? new Failure(null, true) : null;
    }
    synchronized boolean ready() { return ready; }
    synchronized int retainedCharacters() { return tail.length(); }
}
