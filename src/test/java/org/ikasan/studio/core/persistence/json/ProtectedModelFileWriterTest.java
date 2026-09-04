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

    private Path model(String content) throws IOException {
        Path model = directory.resolve("model.json");
        Files.writeString(model, content, StandardCharsets.UTF_8);
        return model;
    }
}
