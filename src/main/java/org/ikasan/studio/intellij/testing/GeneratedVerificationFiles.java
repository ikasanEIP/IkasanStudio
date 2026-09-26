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
        Path applicationPom = project.resolve("generated/pom.xml");
        if (Files.isSymbolicLink(applicationPom) || !Files.readString(applicationPom).equals(bundle.originalApplicationPom()))
            throw new IOException("Application POM changed; generate again");
        if (Files.isSymbolicLink(pom) || !Files.readString(pom).equals(expectedPom)
                || !snapshot(directory).equals(expectedSnapshot)) throw new IOException("Project or verification files changed; generate again");
        Path stage = Files.createTempDirectory(project, ".verification-");
        Path backup = null;
        boolean installed = false;
        try {
            // Preserve unrelated tests. The archive retains corrections in Studio's own package too.
            if (Files.exists(directory)) {
                try (var paths = Files.walk(directory)) {
                    for (Path source : paths.toList()) {
                        Path relative = directory.relativize(source);
                        if (relative.startsWith("java/org/ikasan/studio/verification")
                                || relative.startsWith("resources/studio-verification")) continue;
                        Path target = stage.resolve(relative);
                        if (Files.isDirectory(source)) Files.createDirectories(target);
                        else { Files.createDirectories(target.getParent()); Files.copy(source, target); }
                    }
                }
            }
            for (var entry : bundle.files().entrySet()) {
                Path file = stage.resolve(entry.getKey()).normalize();
                if (!file.startsWith(stage)) throw new IOException("Invalid generated verification path");
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue());
            }
            if (!Files.readString(pom).equals(expectedPom) || !Files.readString(applicationPom).equals(bundle.originalApplicationPom())
                    || !snapshot(directory).equals(expectedSnapshot))
                throw new IOException("Project or verification files changed; generate again");
            ensureBackupIgnoreRule(project);
            if (Files.exists(directory)) {
                backup = directory.resolveSibling("test.bak"
                        + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID());
                Files.move(directory, backup, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.createDirectories(directory.getParent());
            Files.move(stage, directory, StandardCopyOption.ATOMIC_MOVE);
            installed = true;
            replace(applicationPom, bundle.applicationPom());
            Path newPom = Files.createTempFile(project, ".verification-pom-", ".tmp");
            try {
                Files.writeString(newPom, bundle.rootPom());
                Files.move(newPom, pom, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(newPom); }
            return backup;
        } catch (Exception failure) {
            try {
                if (installed) {
                    replace(applicationPom, bundle.originalApplicationPom());
                    replace(pom, expectedPom);
                    Files.move(directory, stage, StandardCopyOption.ATOMIC_MOVE);
                }
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

    /** Append only our backup rule; keep the developer's ignore entries intact. */
    private static void ensureBackupIgnoreRule(Path project) throws IOException {
        Path ignore = project.resolve(".gitignore");
        if (Files.isSymbolicLink(ignore)) throw new IOException("Refusing to update a symbolic-link .gitignore");
        String current = Files.exists(ignore) ? Files.readString(ignore) : "";
        String rule = "/generated/src/test.bak*/";
        if (current.lines().anyMatch(line -> line.trim().equals(rule))) return;
        String newline = current.contains("\r\n") ? "\r\n" : "\n";
        String addition = (current.isEmpty() || current.endsWith("\n") ? "" : newline)
                + newline + "# Studio verification-test backups" + newline + rule + newline;
        Files.writeString(ignore, addition, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void replace(Path path, String contents) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), ".verification-pom-", ".tmp");
        try {
            Files.writeString(temporary, contents);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
