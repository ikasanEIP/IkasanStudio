package org.ikasan.studio.core.persistence.json;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Replaces model.json only after validation, while retaining three last-known-good revisions.
 * The temporary file is flushed before an atomic same-directory rename, so interruption cannot
 * leave a partially written source-of-truth file.
 */
public final class ProtectedModelFileWriter {
    public static final int BACKUP_COUNT = 3;

    private ProtectedModelFileWriter() {
    }

    @FunctionalInterface
    public interface Validator {
        void validate(String json) throws Exception;
    }

    public static void write(Path target, String candidate, Validator validator) throws IOException {
        if (target == null || candidate == null || validator == null) {
            throw new IllegalArgumentException("target, candidate and validator are required");
        }

        validate(candidate, validator, "candidate model");
        Files.createDirectories(target.getParent());

        if (Files.exists(target)) {
            String current = Files.readString(target, StandardCharsets.UTF_8);
            validate(current, validator, "existing model.json");
            rotateBackups(target);
            writeAtomically(backup(target, 1), current);
        }

        Path temporary = temporarySibling(target);
        try {
            writeAndFlush(temporary, candidate);
            validate(Files.readString(temporary, StandardCharsets.UTF_8), validator, "temporary model");
            atomicReplace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void rotateBackups(Path target) throws IOException {
        for (int index = BACKUP_COUNT; index > 1; index--) {
            Path older = backup(target, index - 1);
            if (Files.exists(older)) {
                Files.move(older, backup(target, index), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = temporarySibling(target);
        try {
            writeAndFlush(temporary, content);
            atomicReplace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // partial writes are handled via buffer.hasRemaining(), not the returned count
    private static void writeAndFlush(Path path, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static void atomicReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("The filesystem does not support atomic replacement of model.json; the existing file was preserved", e);
        }
    }

    private static void validate(String content, Validator validator, String description) throws IOException {
        try {
            validator.validate(content);
        } catch (Exception e) {
            throw new IOException("Refusing to replace model.json because the " + description + " is invalid", e);
        }
    }

    private static Path backup(Path target, int index) {
        return target.resolveSibling(target.getFileName() + ".bak." + index);
    }

    private static Path temporarySibling(Path target) {
        return target.resolveSibling("." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");
    }
}
