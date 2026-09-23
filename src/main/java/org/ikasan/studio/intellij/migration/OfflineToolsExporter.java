package org.ikasan.studio.intellij.migration;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

/** Extracts only into a new staging directory; never overwrites customer files. */
final class OfflineToolsExporter {
    static final String RESOURCE = "/studio/offline/studio-offline-tools.zip";
    static final String DIRECTORY = "ikasan-studio-offline-tools";
    private static final long MAX_BYTES = 256L * 1024 * 1024;

    static Path export(InputStream archive, Path parent, Runnable checkCancelled) throws IOException {
        Path target = parent.resolve(DIRECTORY);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            throw new FileAlreadyExistsException(target.toString());
        Path staging = Files.createTempDirectory(parent, ".ikasan-offline-");
        try {
            extract(archive, staging, checkCancelled);
            checkCancelled.run();
            // No REPLACE_EXISTING: another export or a user-created directory must win.
            Files.move(staging, target);
            return target;
        } finally {
            if (Files.exists(staging)) {
                try (var paths = Files.walk(staging)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void extract(InputStream archive, Path root, Runnable checkCancelled) throws IOException {
        Set<Path> seen = new HashSet<>();
        long bytes = 0; int entries = 0; String prefix = null;
        try (var zip = new ZipInputStream(archive)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                checkCancelled.run();
                if (++entries > 2048) throw new IOException("Offline tools archive has too many entries");
                String name = entry.getName();
                if (name.contains("\\") || name.startsWith("/") || name.contains(":")) throw new IOException("Invalid archive path");
                int slash = name.indexOf('/');
                if (slash <= 0) throw new IOException("Missing distribution directory");
                String folder = name.substring(0, slash);
                if (!folder.startsWith("studio-cli-") || (prefix != null && !prefix.equals(folder)))
                    throw new IOException("Unexpected distribution directory");
                prefix = folder;
                String relative = name.substring(slash + 1);
                if (relative.isEmpty()) continue;
                Path path = root.resolve(relative).normalize();
                if (!path.startsWith(root) || path.equals(root) || !seen.add(path)) throw new IOException("Unsafe or duplicate archive path");
                if (entry.isDirectory()) { Files.createDirectories(path); continue; }
                Files.createDirectories(path.getParent());
                try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer = new byte[16384]; int count;
                    while ((count = zip.read(buffer)) != -1) {
                        checkCancelled.run(); bytes += count;
                        if (bytes > MAX_BYTES) throw new IOException("Offline tools archive exceeds extraction limit");
                        out.write(buffer, 0, count);
                    }
                }
                if (relative.equals("bin/studio-cli") && !path.toFile().setExecutable(true, true)
                        && FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
                    throw new IOException("Could not make the CLI launcher executable");
            }
        }
        for (String required : new String[]{"bin/studio-cli", "bin/studio-cli.bat", "bin/studio_upgrade.py", "README.md", "CommandLineMigration.md"})
            if (!Files.isRegularFile(root.resolve(required))) throw new IOException("Incomplete offline tools archive: " + required);
        if (!Files.isDirectory(root.resolve("lib"))) throw new IOException("Missing standalone dependencies");
    }
}
