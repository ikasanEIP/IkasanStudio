package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.metapack.loading.ClasspathDirectoryScanner;
import org.ikasan.studio.core.metapack.loading.MetaPackResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("engine")
class MetaPackResourcePathsTest {
    @Test void resourceLookupCannotEscapeTheMetaPackNamespace() throws Exception {
        for (String path : List.of("../outside", "studio/metapack/../../outside", "studio/metapack/Example/../Other/metapack.json",
                "studio/metapack//Example/metapack.json", "/studio/metapack/Example/metapack.json",
                "C:/studio/metapack/Example/metapack.json", "file:/etc/passwd", "studio\\metapack\\Example",
                "META-INF/plugin.xml", "messages/studioBundle.properties", "studio/metapacks/Example/metapack.json",
                "studio/metapack/Example/./metapack.json", "studio/metapack/Example/.. /outside.json",
                "studio/metapack/Example./metapack.json", "studio/metapack/Example/unsafe\nfile.json")) {
            assertNull(MetaPackResources.resource(path), path);
            assertNull(MetaPackResources.open(path), path);
            assertThrows(IOException.class, () -> MetaPackResources.directories(path), path);
        }
    }

    @Test void directoryScannerAndTemplateLoaderRejectTraversalBeforeClasspathLookup() {
        for (String path : List.of("../outside", "a/../b", "a//b", "C:/outside", "/outside", "a\\b"))
            assertThrows(IOException.class, () -> ClasspathDirectoryScanner.getDirectories(path), path);
        for (String id : List.of("../Other", "V4/../../outside", "file:Example", ".", "..", "Example\\Other", "Example."))
            assertThrows(IllegalArgumentException.class, () -> MetaPackResources.templateLoader(id), id);
    }
}
