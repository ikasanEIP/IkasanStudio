package org.ikasan.studio.intellij.testing;

import org.ikasan.studio.core.generator.GeneratedVerification;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** On-demand baseline writer. A refresh archives the entire previous bundle, never merges tests. */
public final class GeneratedVerificationFiles {
    private GeneratedVerificationFiles() { }

    public static String snapshot(Path root) throws Exception {
        if (!Files.exists(root)) return "missing";
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) throw new IOException("Expected a real verification directory");
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted().toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("Verification directory contains a symbolic link: " + path);
                if (root.relativize(path).startsWith("target")) continue;
                if (Files.isRegularFile(path)) {
                    hash.update(root.relativize(path).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    hash.update((byte) 0);
                    hash.update(Files.readAllBytes(path));
                }
            }
        }
        return HexFormat.of().formatHex(hash.digest());
    }

    public static Path write(Path project, String expectedPom, String expectedSnapshot,
                             GeneratedVerification.Bundle bundle) throws Exception {
        Path directory = project.resolve(GeneratedVerification.DIRECTORY);
        Path pom = project.resolve("pom.xml");
        if (Files.isSymbolicLink(pom) || !Files.readString(pom).equals(expectedPom)
                || !snapshot(directory).equals(expectedSnapshot)) throw new IOException("Project or verification files changed; generate again");
        Path stage = Files.createTempDirectory(project, ".verification-");
        Path backup = null;
        boolean installed = false;
        try {
            for (var entry : bundle.files().entrySet()) {
                Path file = stage.resolve(entry.getKey()).normalize();
                if (!file.startsWith(stage)) throw new IOException("Invalid generated verification path");
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue());
            }
            if (!Files.readString(pom).equals(expectedPom) || !snapshot(directory).equals(expectedSnapshot))
                throw new IOException("Project or verification files changed; generate again");
            if (Files.exists(directory)) {
                backup = directory.resolveSibling(GeneratedVerification.DIRECTORY + ".bak"
                        + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID());
                Files.move(directory, backup, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.move(stage, directory, StandardCopyOption.ATOMIC_MOVE);
            installed = true;
            Path newPom = Files.createTempFile(project, ".verification-pom-", ".tmp");
            try {
                Files.writeString(newPom, bundle.rootPom());
                Files.move(newPom, pom, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(newPom); }
            return backup;
        } catch (Exception failure) {
            try {
                if (installed) Files.move(directory, stage, StandardCopyOption.ATOMIC_MOVE);
                if (backup != null) Files.move(backup, directory, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception restore) { failure.addSuppressed(restore); }
            throw failure;
        } finally {
            if (Files.exists(stage)) {
                try (var paths = Files.walk(stage)) {
                    for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
                }
            }
        }
    }
}
