package org.ikasan.studio.intellij.ai;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** Background-only scan. Two identical observations avoid announcing partially written files. */
final class StudioAiProposalInbox {
    record Stamp(long size, FileTime modified) {}
    private boolean initialised;
    private Map<Path, Stamp> previous = Map.of();
    private final Map<Path, Stamp> announced = new HashMap<>();

    static Map<Path, Stamp> scan(Path directory) throws IOException {
        Map<Path, Stamp> files = new HashMap<>();
        if (!Files.isDirectory(directory)) return files;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.studio-proposal.json")) {
            for (Path file : stream) {
                try {
                    var attrs = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (attrs.isRegularFile()) files.put(file, new Stamp(attrs.size(), attrs.lastModifiedTime()));
                } catch (NoSuchFileException ignored) { /* Atomic replacement; retry next scan. */ }
            }
        }
        return files;
    }

    Optional<Path> observe(Map<Path, Stamp> files) {
        if (!initialised) {
            initialised = true;
            previous = Map.copyOf(files);
            announced.putAll(files);
            return Optional.empty();
        }
        Map<Path, Stamp> ready = new HashMap<>();
        files.forEach((path, stamp) -> {
            if (stamp.equals(previous.get(path)) && !stamp.equals(announced.get(path))) {
                ready.put(path, stamp);
                announced.put(path, stamp);
            }
        });
        announced.keySet().retainAll(files.keySet());
        previous = Map.copyOf(files);
        return latest(ready);
    }

    static Optional<Path> latest(Map<Path, Stamp> files) {
        return files.keySet().stream().max(Comparator
                .comparing((Path path) -> files.get(path).modified()).thenComparing(Path::toString));
    }
}
