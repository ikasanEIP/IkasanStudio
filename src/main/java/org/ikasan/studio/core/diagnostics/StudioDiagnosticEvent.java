package org.ikasan.studio.core.diagnostics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/** Privacy-safe fields only: never exception messages, property values or model serialization. */
public final class StudioDiagnosticEvent {
    public enum Event { GENERATION_STARTED, GENERATION_COMPLETED, GENERATION_FAILED, MODEL_SAVE_FAILED,
        PROPERTY_CHANGED, FILE_WRITE_FAILED, CONFIGURATION_INVALID, RUNTIME_FAILED,
        NOTIFICATION_WARNING, NOTIFICATION_ERROR, DIAGNOSTICS_COLLECTED, DIAGNOSTICS_FAILED }
    private static final String SESSION_SALT = UUID.randomUUID().toString();
    public static final String PREFIX = "STUDIO-DIAG v1 ";
    private StudioDiagnosticEvent() { }

    public static String format(Event event, Throwable failure, String module, String flow, String component) {
        StringBuilder result = new StringBuilder(PREFIX).append("event=").append(event.name())
                .append(" level=").append(level(event)).append(" module=").append(id(module)).append(" flow=").append(id(flow))
                .append(" component=").append(id(component));
        // Preserve cause types and plugin call sites without unsafe exception messages or source excerpts.
        int depth = 0;
        for (Throwable current = failure; current != null && depth++ < 4; current = current.getCause()) {
            result.append(" error=").append(current.getClass().getName().replaceAll("[^a-zA-Z0-9_.$]", "_"));
            int frames = 0;
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith("org.ikasan.studio.") && frames++ < 8) {
                    result.append(" frame=").append(frame.getClassName().replaceAll("[^a-zA-Z0-9_.$]", "_"))
                            .append('.').append(frame.getMethodName().replaceAll("[^a-zA-Z0-9_$]", "_"))
                            .append(':').append(Math.max(0, frame.getLineNumber()));
                }
            }
        }
        return result.toString();
    }

    public static String level(Event event) {
        return switch (event) {
            case PROPERTY_CHANGED -> "DEBUG";
            case GENERATION_STARTED, GENERATION_COMPLETED, DIAGNOSTICS_COLLECTED -> "INFO";
            default -> "WARN";
        };
    }

    private static String id(String name) {
        if (name == null) return "none";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((SESSION_SALT + name).getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
