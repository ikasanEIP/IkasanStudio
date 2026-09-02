package org.ikasan.studio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LayerBoundaryTest {

    @Test
    void coreAndIntegrationDoNotDependOnPlatformOrUiLayers() throws Exception {
        assertSourcesDoNotContain(
                List.of(Path.of("src/main/java/org/ikasan/studio/core"),
                        Path.of("src/main/java/org/ikasan/studio/integration")),
                "import com.intellij", "import org.ikasan.studio.ui",
                "import org.ikasan.studio.intellij");
    }

    @Test
    void domainModelDoesNotDependOnPersistenceAdapters() throws Exception {
        assertSourcesDoNotContain(
                List.of(Path.of("src/main/java/org/ikasan/studio/core/model")),
                "import org.ikasan.studio.core.persistence");
    }

    @Test
    void removedLegacyPackagesStayRemoved() {
        assertThat(Path.of("src/main/java/org/ikasan/studio/ui/intellij")).doesNotExist();
        assertThat(Path.of("src/main/java/org/ikasan/studio/core/model/ikasan/meta")).doesNotExist();
        assertThat(Path.of("src/main/java/org/ikasan/studio/core/model/ikasan/instance/serialization"))
                .doesNotExist();
    }

    // UseOptimizedEelFunctions flags plain java.nio.file I/O as potentially inefficient under IntelliJ's
    // remote-dev (Eel) abstraction - not applicable here: this is a plain JUnit test walking this plugin's own
    // source tree during a Gradle build, with no live IDE/Project context to route through the Eel API at all.
    @SuppressWarnings("UseOptimizedEelFunctions")
    private static void assertSourcesDoNotContain(List<Path> roots, String... forbidden) throws Exception {
        for (Path root : roots) {
            try (var files = Files.walk(root)) {
                for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(source)).as("architecture boundary: %s", source)
                            .doesNotContain(forbidden);
                }
            }
        }
    }
}
