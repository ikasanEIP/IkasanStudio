package org.example.metapack;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.ikasan.studio.core.metapack.loading.MetaPackInstaller;
import static org.junit.jupiter.api.Assertions.*;

class PackArchiveIT {
    @Test void packagedArtifactIsAcceptedByTheStudioInstaller() throws Exception {
        Path jar = Path.of("target/example-pack-0.1.0-SNAPSHOT.jar");
        assertEquals("ExampleIkasan4", MetaPackInstaller.validate(MetaPackArchive.open(jar)).id());
        try (var archive = new JarFile(jar.toFile())) {
            assertFalse(archive.stream().anyMatch(entry -> entry.getName().endsWith(".class")));
            assertNotNull(archive.getEntry("studio/metapack/ExampleIkasan4/library/Converter/components/UppercaseConverter/component-meta_en_GB.json"));
        }
    }
}
