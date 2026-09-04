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

    private Path model(String content) throws IOException {
        Path model = directory.resolve("model.json");
        Files.writeString(model, content, StandardCharsets.UTF_8);
        return model;
    }
}
