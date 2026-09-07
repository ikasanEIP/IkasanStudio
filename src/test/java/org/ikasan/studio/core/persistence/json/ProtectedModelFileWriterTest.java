package org.ikasan.studio.core.persistence.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedModelFileWriterTest {
    @TempDir
    Path directory;

    private final ProtectedModelFileWriter.Validator objectValidator = json -> {
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("not a JSON object");
        }
    };

    @Test
    void invalidCandidateNeverTouchesExistingModel() throws Exception {
        Path model = model("{\"revision\":1}");
        assertThrows(IOException.class,
                () -> ProtectedModelFileWriter.write(model, "CouldNotConvert", objectValidator));
        assertEquals("{\"revision\":1}", Files.readString(model));
        assertFalse(Files.exists(directory.resolve("model.json.bak.1")));
    }

    @Test
    void invalidExistingModelIsNeverOverwritten() throws Exception {
        Path model = model("damaged but potentially recoverable");
        assertThrows(IOException.class,
                () -> ProtectedModelFileWriter.write(model, "{\"revision\":2}", objectValidator));
        assertEquals("damaged but potentially recoverable", Files.readString(model));
    }

    @Test
    void validReplacementCreatesLastKnownGoodBackup() throws Exception {
        Path model = model("{\"revision\":1}");
        ProtectedModelFileWriter.write(model, "{\"revision\":2}", objectValidator);
        assertEquals("{\"revision\":2}", Files.readString(model));
        assertEquals("{\"revision\":1}", Files.readString(directory.resolve("model.json.bak.1")));
    }

    @Test
    void rotatesThreeRecoverableRevisions() throws Exception {
        Path model = model("{\"revision\":1}");
        ProtectedModelFileWriter.write(model, "{\"revision\":2}", objectValidator);
        ProtectedModelFileWriter.write(model, "{\"revision\":3}", objectValidator);
        ProtectedModelFileWriter.write(model, "{\"revision\":4}", objectValidator);
        ProtectedModelFileWriter.write(model, "{\"revision\":5}", objectValidator);

        assertEquals("{\"revision\":5}", Files.readString(model));
        assertEquals("{\"revision\":4}", Files.readString(directory.resolve("model.json.bak.1")));
        assertEquals("{\"revision\":3}", Files.readString(directory.resolve("model.json.bak.2")));
        assertEquals("{\"revision\":2}", Files.readString(directory.resolve("model.json.bak.3")));
    }

    @Test
    void failureAfterWritingTemporaryFileStillPreservesOriginal() throws Exception {
        Path model = model("{\"revision\":1}");
        AtomicInteger validations = new AtomicInteger();

        assertThrows(IOException.class, () -> ProtectedModelFileWriter.write(model, "{\"revision\":2}", json -> {
            objectValidator.validate(json);
            if (validations.incrementAndGet() == 3) {
                throw new IllegalStateException("simulated post-write validation failure");
            }
        }));

        assertEquals("{\"revision\":1}", Files.readString(model));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void simulatedPermissionFailurePreservesOriginal() throws Exception {
        Path model = model("{\"revision\":1}");
        assertThrows(java.nio.file.AccessDeniedException.class, () ->
                ProtectedModelFileWriter.write(model, "{\"revision\":2}", objectValidator,
                        () -> { throw new java.nio.file.AccessDeniedException(model.toString()); }));
        assertEquals("{\"revision\":1}", Files.readString(model));
    }

    @Test
    void simulatedDiskFullFailurePreservesOriginal() throws Exception {
        Path model = model("{\"revision\":1}");
        assertThrows(java.nio.file.FileSystemException.class, () ->
                ProtectedModelFileWriter.write(model, "{\"revision\":2}", objectValidator,
                        () -> { throw new java.nio.file.FileSystemException(model.toString(), null, "No space left on device"); }));
        assertEquals("{\"revision\":1}", Files.readString(model));
    }

    @Test
    void onlyValidBackupsAreOfferedForRecovery() throws Exception {
        Path model = model("damaged primary");
        Files.writeString(directory.resolve("model.json.bak.1"), "{\"revision\":3}");
        Files.writeString(directory.resolve("model.json.bak.2"), "damaged backup");
        Files.writeString(directory.resolve("model.json.bak.3"), "{\"revision\":1}");
        assertEquals(java.util.List.of(1, 3), ProtectedModelFileWriter.validBackupIndexes(model, objectValidator));
    }

    @Test
    void restorePreservesRejectedPrimaryAndAtomicallyInstallsValidatedBackup() throws Exception {
        Path model = model("damaged primary");
        Files.writeString(directory.resolve("model.json.bak.1"), "{\"revision\":3}");
        Path rejected = ProtectedModelFileWriter.restoreBackup(model, 1, objectValidator);
        assertEquals("{\"revision\":3}", Files.readString(model));
        assertEquals("damaged primary", Files.readString(rejected));
        assertTrue(rejected.getFileName().toString().startsWith("model.json.rejected."));
    }

    @Test
    void invalidBackupCannotReplacePrimary() throws Exception {
        Path model = model("damaged primary");
        Files.writeString(directory.resolve("model.json.bak.1"), "also damaged");
        assertThrows(IOException.class, () -> ProtectedModelFileWriter.restoreBackup(model, 1, objectValidator));
        assertEquals("damaged primary", Files.readString(model));
    }

    @Test
    void diskFullAfterPartialWritePreservesModelBackupAndUserFile() throws Exception {
        Path model = model("{\"revision\":1}");
        Path user = directory.resolve("Owned.java");
        Files.writeString(user, "// developer implementation");
        assertThrows(java.nio.file.FileSystemException.class, () ->
                ProtectedModelFileWriter.writeWithCandidateWriter(model, "{\"revision\":2}", objectValidator,
                        (temporary, candidate) -> {
                            Files.writeString(temporary, candidate.substring(0, 5));
                            throw new java.nio.file.FileSystemException(temporary.toString(), null, "No space left on device");
                        }));
        assertEquals("{\"revision\":1}", Files.readString(model));
        assertEquals("{\"revision\":1}", Files.readString(directory.resolve("model.json.bak.1")));
        assertEquals("// developer implementation", Files.readString(user));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
        ProtectedModelFileWriter.write(model, "{\"revision\":3}", objectValidator);
        assertEquals("{\"revision\":3}", Files.readString(model));
    }

    @Test
    void concurrentSavesKeepEveryBackupValidAndReleaseLockAfterFailure() throws Exception {
        Path model = model("{\"revision\":0}");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 1; i <= 20; i++) {
                final int revision = i;
                jobs.add(pool.submit(() -> {
                    try { ProtectedModelFileWriter.write(model, "{\"revision\":" + revision + "}", objectValidator); }
                    catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }));
            }
            for (var job : jobs) job.get(10, java.util.concurrent.TimeUnit.SECONDS);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var revisions = new java.util.HashSet<Integer>();
            revisions.add(mapper.readTree(Files.readString(model)).get("revision").asInt());
            for (int i = 1; i <= 3; i++) {
                revisions.add(mapper.readTree(Files.readString(directory.resolve("model.json.bak." + i))).get("revision").asInt());
            }
            assertEquals(4, revisions.size());
        } finally { pool.shutdownNow(); }
    }

    private Path model(String content) throws IOException {
        Path model = directory.resolve("model.json");
        Files.writeString(model, content, StandardCharsets.UTF_8);
        return model;
    }
}
