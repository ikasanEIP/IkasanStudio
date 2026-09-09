package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.metapack.loading.ClasspathDirectoryScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@org.junit.jupiter.api.Tag("engine")
class ClasspathDirectoryScannerTest {
    @TempDir Path temp;

    @Test
    void mergesExplodedAndJarResourcesInsteadOfStoppingAtTheSchemaDirectory() throws Exception {
        Path engine = temp.resolve("engine");
        Files.createDirectories(engine.resolve("studio/metapack/schema"));
        Path pack = temp.resolve("pack.jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(pack))) {
            for (String entry : new String[]{"studio/", "studio/metapack/", "studio/metapack/Example/"}) {
                jar.putNextEntry(new JarEntry(entry));
                jar.closeEntry();
            }
        }
        try (var loader = new URLClassLoader(new java.net.URL[]{engine.toUri().toURL(), pack.toUri().toURL()}, null)) {
            assertArrayEquals(new String[]{"studio/metapack/Example", "studio/metapack/schema"},
                    ClasspathDirectoryScanner.getDirectories("studio/metapack", loader));
            assertArrayEquals(new String[0], ClasspathDirectoryScanner.getDirectories("missing", loader));
        }
    }
}
