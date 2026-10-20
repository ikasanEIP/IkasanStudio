package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.metapack.loading.MetaPackDirectoryProvider;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Optional resource-only packs. Invoked by background discovery, never by settings UI. */
public final class SnapshotMetaPackDirectoryProvider implements MetaPackDirectoryProvider {
    public static final String RESOURCE = "studio/development-packs/studio-pack-v5.jar";

    @Override public Path directory() {
        Path directory = Path.of(PathManager.getConfigPath(), "ikasan-studio", "snapshot-meta-packs");
        // An explicit development-classpath build already provides V5; avoid conflicting IDs.
        if (getClass().getClassLoader().getResource("studio/metapack/V5.0.1-SNAPSHOT/metapack.json") != null
                || hasInstalledV5(new StudioMetaPackDirectoryProvider().directory())) {
            return directory.resolve("unused-classpath-build");
        }
        if (IkasanStudioSettings.areSnapshotMetaPacksAllowed()) {
            try (var source = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
                prepare(directory, source);
            } catch (IOException failure) {
                Logger.getInstance(getClass()).warn("Cannot activate bundled V5 snapshot meta-pack", failure);
            }
        }
        // Keep previously activated resources available for existing projects after opt-out.
        // New selections are separately gated; session sources remain immutable until restart.
        return directory;
    }

    static boolean hasInstalledV5(Path directory) {
        if (!Files.isDirectory(directory)) return false;
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                try {
                    if ("V5.0.1-SNAPSHOT".equals(org.ikasan.studio.core.metapack.loading.MetaPackArchive.open(file).id())) return true;
                } catch (IOException ignored) {
                    // Normal discovery reports malformed user-installed packs independently.
                }
            }
        } catch (IOException failure) {
            Logger.getInstance(SnapshotMetaPackDirectoryProvider.class).warn("Cannot inspect installed V5 packs", failure);
        }
        return false;
    }

    static void prepare(Path directory, InputStream source) throws IOException {
        if (source == null) throw new IOException("Bundled V5 snapshot resource is missing");
        byte[] content = source.readAllBytes();
        Path target = directory.resolve("studio-pack-v5.jar");
        if (Files.isRegularFile(target) && java.util.Arrays.equals(Files.readAllBytes(target), content)) return;
        Files.createDirectories(directory);
        Path staged = Files.createTempFile(directory, "v5-", ".tmp");
        try {
            Files.write(staged, content);
            org.ikasan.studio.core.metapack.loading.MetaPackArchive.open(staged);
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
    }
}
