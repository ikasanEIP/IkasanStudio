package org.ikasan.studio.intellij.metapack;

import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.ikasan.studio.core.metapack.loading.MetaPackResources;
import org.ikasan.studio.core.metapack.loading.ComponentLibraryLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotMetaPackDirectoryProviderTest {
    @TempDir Path directory;

    @Test void optionalPayloadIsResourceOnlyAndLoadsFromTheActivatedJar() throws Exception {
        try (var source = getClass().getClassLoader().getResourceAsStream(SnapshotMetaPackDirectoryProvider.RESOURCE)) {
            SnapshotMetaPackDirectoryProvider.prepare(directory, source);
        }
        var target = directory.resolve("studio-pack-v5.jar");
        var archive = MetaPackArchive.open(target);
        assertEquals("V5.0.1-SNAPSHOT", archive.id());
        assertTrue(SnapshotMetaPackDirectoryProvider.hasInstalledV5(directory));
        assertFalse(SnapshotMetaPackDirectoryProvider.hasInstalledV5(directory.resolve("missing")));
        MetaPackResources.inspect(archive, () -> {
            var manifest = new ComponentLibraryLoader().loadManifest(archive.id());
            assertEquals("26", manifest.javaVersion());
            assertEquals("5.0.1-SNAPSHOT", manifest.ikasanVersion());
            return null;
        });
        var stamp = Files.getLastModifiedTime(target);
        try (var source = Files.newInputStream(target)) {
            SnapshotMetaPackDirectoryProvider.prepare(directory, source);
        }
        assertEquals(stamp, Files.getLastModifiedTime(target), "Unchanged activation preserves the session source");
    }

    @Test void invalidOrMissingPayloadDoesNotReplacePreviouslyActivatedPack() throws Exception {
        try (var source = getClass().getClassLoader().getResourceAsStream(SnapshotMetaPackDirectoryProvider.RESOURCE)) {
            SnapshotMetaPackDirectoryProvider.prepare(directory, source);
        }
        byte[] previous = Files.readAllBytes(directory.resolve("studio-pack-v5.jar"));
        assertThrows(java.io.IOException.class, () -> SnapshotMetaPackDirectoryProvider.prepare(directory, null));
        assertThrows(java.io.IOException.class, () -> SnapshotMetaPackDirectoryProvider.prepare(
                directory, new ByteArrayInputStream(new byte[]{1, 2, 3})));
        assertArrayEquals(previous, Files.readAllBytes(directory.resolve("studio-pack-v5.jar")));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
}
