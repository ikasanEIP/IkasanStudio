package org.ikasan.studio.testing.engine;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.ikasan.studio.core.metapack.loading.ClasspathDirectoryScanner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Tag("engine")
class ClasspathDirectoryScannerTest {
    @TempDir Path temp;

    @Test
    void mergesExplodedAndJarResourcesInsteadOfStoppingAtTheSchemaDirectory() throws Exception {
        Path engine = temp.resolve("engine");
        Files.createDirectories(engine.resolve("studio/metapack/schema"));
        Path pack = temp.resolve("pack.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(pack))) {
            for (String entry : new String[]{"studio/", "studio/metapack/", "studio/metapack/Example/"}) {
                jar.putNextEntry(new JarEntry(entry));
                jar.closeEntry();
            }
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{engine.toUri().toURL(), pack.toUri().toURL()}, null)) {
            assertArrayEquals(new String[]{"studio/metapack/Example", "studio/metapack/schema"},
                    ClasspathDirectoryScanner.getDirectories("studio/metapack", loader));
            assertArrayEquals(new String[0], ClasspathDirectoryScanner.getDirectories("missing", loader));
        }
    }
}
