package org.ikasan.studio.testing.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.jar.*;
import java.util.jar.JarOutputStream;
import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Tag("engine")
class MetaPackArchiveTest {
    @TempDir Path temporary;

    @Test
    void rejectsExecutableOrAmbiguousArchives() throws Exception {
        for (String entry : new String[]{"Example.class", "META-INF/services/example.Provider",
                "studio/metapack/Other/metapack.json", "studio/metapack/Example/../outside.json",
                "../outside.json", "studio/metapack/Example/.. /outside.json", "studio/metapack/Example./metapack.json", "/absolute.json", "C:/outside.json", "C:outside.json",
                "studio/metapack/Example/./inside.json", "studio/metapack/Example//inside.json",
                "studio/metapack/Example/unsafe\\file.json", "studio/metapack/Example/unsafe\nfile.json"}) {
            Path file = temporary.resolve("invalid.jar");
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
                write(jar, "studio/metapack/Example/metapack.json");
                write(jar, entry);
            }
            assertThrows(IOException.class, () -> MetaPackArchive.open(file), entry);
        }
    }

    @Test
    void indexesImplicitDirectoriesAndEncodedResourceNames() throws Exception {
        Path file = temporary.resolve("pack with spaces.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            write(jar, "studio/metapack/Example/metapack.json");
            write(jar, "studio/metapack/Example/library/Producer/small icon #1.png");
        }
        MetaPackArchive archive = MetaPackArchive.open(file);
        assertArrayEquals(new String[]{"studio/metapack/Example/library"}, archive.directories("studio/metapack/Example"));
        URL resource = archive.resource("studio/metapack/Example/library/Producer/small icon #1.png");
        assertNotNull(resource);
        try (InputStream stream = resource.openStream()) {
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
