package org.ikasan.studio.core.diagnostics;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Explicit allowlist export. No project traversal and no model-file access. */
public final class StudioDiagnosticBundle {
    public static final int MAX_LOG_BYTES = 2 * 1024 * 1024;
    private StudioDiagnosticBundle() { }

    public static String redactLogs(String source) {
        StringBuilder result = new StringBuilder();
        int omitted = 0;
        for (String line : source.split("\n")) {
            int start = line.indexOf(StudioDiagnosticEvent.PREFIX);
            if (start < 0) {
                if (line.contains("STUDIO:") || line.contains("Studio:")) omitted++;
                continue;
            }
            // The record must be the entire IDE log message, never a substring of a property value.
            String envelope = line.substring(0, start);
            if (!envelope.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9:,.]+ +\\[ *[0-9]+] +(INFO|WARN|DEBUG|ERROR) +- +[#a-zA-Z0-9_.$]+ +- +")) {
                omitted++; continue;
            }
            String event = line.substring(start).strip();
            // A strict whole-record grammar rejects multiline payloads, paths, values and extra fields.
            if (event.length() > 8192 || !event.matches("STUDIO-DIAG v1 event=[A-Z_]+ level=(INFO|WARN|DEBUG) module=(none|[a-f0-9]{16}) flow=(none|[a-f0-9]{16}) component=(none|[a-f0-9]{16})( error=[a-zA-Z0-9_.$]+| frame=org\\.ikasan\\.studio\\.[a-zA-Z0-9_.$]+:[0-9]+)*")) {
                omitted++; continue;
            }
            try { StudioDiagnosticEvent.Event.valueOf(event.split(" ")[2].substring(6)); }
            catch (IllegalArgumentException unknown) { omitted++; continue; }
            // Timestamp and severity only from the IDE envelope, never arbitrary prefix text.
            var timestamp = java.util.regex.Pattern.compile("^([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9:,.]+)").matcher(line);
            if (timestamp.find()) result.append(timestamp.group(1)).append(' ');
            result.append(event).append('\n');
        }
        return result.append("Legacy/unrecognized Studio records omitted for privacy: ").append(omitted).append('\n').toString();
    }

    public static void write(Path target, Path ideaLog, Map<String, String> environment) throws IOException {
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
            throw new IOException("Diagnostics destination must have a .zip extension");
        String logs;
        if (Files.isRegularFile(ideaLog)) {
            try (var file = new RandomAccessFile(ideaLog.toFile(), "r")) {
                long offset = Math.max(0, file.length() - MAX_LOG_BYTES);
                file.seek(offset);
                byte[] bytes = new byte[(int) Math.min(MAX_LOG_BYTES, file.length() - offset)];
                file.readFully(bytes);
                String tail = new String(bytes, StandardCharsets.UTF_8);
                // Ignore a partial record at the tail boundary.
                logs = redactLogs(offset == 0 ? tail : (tail.indexOf('\n') < 0 ? "" : tail.substring(tail.indexOf('\n') + 1)));
            }
        } else logs = "IDE log is not available. No logs collected.\n";
        StringBuilder metadata = new StringBuilder();
        for (String key : new String[] {"pluginVersion", "ideVersion", "ideBuild", "metaPack"}) {
            String value = environment.getOrDefault(key, "unavailable");
            // Versions/pack identifiers only; never arbitrary strings from a model or environment.
            metadata.append(key).append('=').append(value.matches("[a-zA-Z0-9 ._()+-]{1,120}") ? value : "[omitted]").append('\n');
        }
        Path temp = Files.createTempFile(target.toAbsolutePath().getParent(), ".studio-diagnostics-", ".tmp");
        try {
            try (var zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                entry(zip, "environment.txt", metadata.toString());
                entry(zip, "studio-redacted.log", logs);
                entry(zip, "README.txt", "Local diagnostics only; nothing is uploaded.\nModel contents, generated files, credentials, raw exception messages, project paths and other plugins' logs are excluded.\nOnly approved structured events from the last 2 MiB of idea.log are included. Older free-text records are omitted.\nContext identifiers are session-salted hashes; they are correlation identifiers, not names.\nReview this archive before sharing. Attach a model separately only if you explicitly choose to do so.\n");
            }
            // Do not silently fall back to a partial replacement on filesystems without atomic moves.
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static void entry(ZipOutputStream zip, String name, String contents) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(contents.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
}
