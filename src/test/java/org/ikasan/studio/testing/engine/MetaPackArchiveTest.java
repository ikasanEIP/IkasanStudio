package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("engine")
class MetaPackArchiveTest {
    @TempDir Path temporary;

    @Test
    void rejectsExecutableOrAmbiguousArchives() throws Exception {
        for (String entry : new String[]{"Example.class", "META-INF/services/example.Provider",
                "studio/metapack/Other/metapack.json", "studio/metapack/Example/../outside.json"}) {
            Path file = temporary.resolve("invalid.jar");
            try (var jar = new JarOutputStream(Files.newOutputStream(file))) {
                write(jar, "studio/metapack/Example/metapack.json");
                write(jar, entry);
            }
            assertThrows(IOException.class, () -> MetaPackArchive.open(file), entry);
        }
    }

    @Test
    void indexesImplicitDirectoriesAndEncodedResourceNames() throws Exception {
        Path file = temporary.resolve("pack with spaces.jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(file))) {
            write(jar, "studio/metapack/Example/metapack.json");
            write(jar, "studio/metapack/Example/library/Producer/small icon #1.png");
        }
        var archive = MetaPackArchive.open(file);
        assertArrayEquals(new String[]{"studio/metapack/Example/library"}, archive.directories("studio/metapack/Example"));
        var resource = archive.resource("studio/metapack/Example/library/Producer/small icon #1.png");
        assertNotNull(resource);
        try (var stream = resource.openStream()) {
            assertEquals(42, stream.read());
        }
        assertNull(archive.resource("studio/metapack/Other/metapack.json"));
    }

    private void write(JarOutputStream jar, String name) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        jar.write(42);
        jar.closeEntry();
    }
}
