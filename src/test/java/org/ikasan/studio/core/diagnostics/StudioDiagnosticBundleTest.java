package org.ikasan.studio.core.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class StudioDiagnosticBundleTest {
    @TempDir Path directory;

    @Test void eventsKeepContextAndCauseLocationsWithoutValuesOrMessages() {
        var failure = new IllegalArgumentException("password=CANARY_SECRET https://user:CANARY_SECRET@host",
                new java.io.IOException("{model: CANARY_MODEL}"));
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("org.ikasan.studio.core.generator.Example", "generate", "CANARY_PATH", 42)});
        String event = StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_FAILED,
                failure, "CANARY_MODULE", "CANARY_FLOW", "CANARY_COMPONENT");
        assertFalse(event.contains("CANARY"));
        assertTrue(event.contains("java.lang.IllegalArgumentException"));
        assertTrue(event.contains("java.io.IOException"));
        assertTrue(event.contains("Example.generate:42"));
        assertEquals(event, StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_FAILED,
                failure, "CANARY_MODULE", "CANARY_FLOW", "CANARY_COMPONENT"));
        assertNotEquals(event, StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_FAILED,
                failure, "other", "CANARY_FLOW", "CANARY_COMPONENT"));
    }

    @Test void archiveContainsOnlyApprovedEntriesAndFields() throws Exception {
        String event = StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_COMPLETED,
                null, "CANARY_MODULE", null, null);
        Path log = directory.resolve("idea.log");
        Files.writeString(log, "other plugin: CANARY_OTHER\nSTUDIO: properties={password=CANARY_SECRET}\n"
                + "{\n  \"model\": \"CANARY_MODEL\"\n}\n"
                + "2026-09-07 10:00:00,000 [1] INFO - studio - " + event + "\n");
        Files.writeString(directory.resolve("model.json"), "CANARY_MODEL_FILE");
        Path target = directory.resolve("diagnostics.zip");
        StudioDiagnosticBundle.write(target, log, Map.of("pluginVersion", "1.2.3", "ideVersion", "2024.3.7",
                "ideBuild", "IC-243.28141.18", "metaPack", "V3.3.9", "password", "CANARY_METADATA"));
        try (ZipFile zip = new ZipFile(target.toFile())) {
            Set<String> names = new HashSet<>();
            StringBuilder contents = new StringBuilder();
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement(); names.add(entry.getName());
                contents.append(new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            assertEquals(Set.of("environment.txt", "studio-redacted.log", "README.txt"), names);
            assertFalse(contents.toString().contains("CANARY"));
            assertTrue(contents.toString().contains("metaPack=V3.3.9"));
            assertTrue(contents.toString().contains(event));
        }
    }

    @Test void rejectsUnrecognizedFieldsAndOversizedRecords() {
        String event = StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_FAILED, null, null, null, null);
        String redacted = StudioDiagnosticBundle.redactLogs("2026-09-07 10:00:00,000 [1] INFO - studio - STUDIO: new value=" + event + " error=CANARY_EMBEDDED\n"
                + "2026-09-07 10:00:00,000 [1] WARN - studio - " + event + " password=CANARY_SECRET\n"
                + "2026-09-07 10:00:00,000 [1] WARN - studio - " + event + " error=CANARY_SECRET".repeat(10000) + "\n"
                + "2026-09-07 10:00:00,000 [1] WARN - studio - " + event.replace("GENERATION_FAILED", "CANARY_UNKNOWN_EVENT") + "\n");
        assertFalse(redacted.contains("CANARY"));
        assertFalse(redacted.contains("STUDIO-DIAG"));
    }

    @Test void readsOnlyBoundedTailAndHandlesMissingLog() throws Exception {
        Path log = directory.resolve("idea.log");
        Files.writeString(log, "STUDIO: CANARY_OLD\n" + "x".repeat(StudioDiagnosticBundle.MAX_LOG_BYTES) + "\n"
                + "2026-09-07 10:00:00,000 [1] WARN - studio - "
                + StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.CONFIGURATION_INVALID, null, null, null, null) + "\n");
        Path target = directory.resolve("tail.zip");
        StudioDiagnosticBundle.write(target, log, Map.of());
        try (ZipFile zip = new ZipFile(target.toFile())) {
            String text = new String(zip.getInputStream(zip.getEntry("studio-redacted.log")).readAllBytes());
            assertTrue(text.contains("CONFIGURATION_INVALID")); assertFalse(text.contains("CANARY"));
        }
        StudioDiagnosticBundle.write(target, directory.resolve("missing.log"), Map.of("metaPack", "V3\nCANARY"));
        try (ZipFile zip = new ZipFile(target.toFile())) {
            assertTrue(new String(zip.getInputStream(zip.getEntry("studio-redacted.log")).readAllBytes()).contains("not available"));
            assertFalse(new String(zip.getInputStream(zip.getEntry("environment.txt")).readAllBytes()).contains("CANARY"));
        }
    }

    @Test void failedExportPreservesDeveloperFileAndLeavesNoTemporaryFiles() throws Exception {
        Path model = directory.resolve("model.json"); Files.writeString(model, "developer data");
        assertThrows(java.io.IOException.class, () -> StudioDiagnosticBundle.write(model, directory.resolve("missing.log"), Map.of()));
        assertEquals("developer data", Files.readString(model));
        Path destination = Files.createDirectory(directory.resolve("blocked.zip"));
        Files.writeString(destination.resolve("keep.txt"), "keep");
        assertThrows(java.io.IOException.class, () -> StudioDiagnosticBundle.write(destination, directory.resolve("missing.log"), Map.of()));
        assertEquals("keep", Files.readString(destination.resolve("keep.txt")));
        try (var files = Files.list(directory)) { assertFalse(files.anyMatch(f -> f.getFileName().toString().endsWith(".tmp"))); }
    }
}
