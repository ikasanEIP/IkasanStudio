package org.ikasan.studio.architecture;

import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureBoundaryTest {

    @Test
    void domainObjectsDoNotOwnViewHandlers() {
        assertThat(allFields(IkasanObject.class))
                .noneMatch(field -> field.getName().toLowerCase().contains("viewhandler"));
        assertThat(IkasanObject.class.getSuperclass()).isEqualTo(Object.class);
    }

    @Test
    void componentMetadataCarriesResourceIdentifiersNotSwingIcons() {
        assertThat(allFields(ComponentMeta.class))
                .noneMatch(field -> Icon.class.isAssignableFrom(field.getType()));
    }

    // UseOptimizedEelFunctions flags plain java.nio.file I/O as potentially inefficient under IntelliJ's
    // remote-dev (Eel) abstraction - not applicable here: this is a plain JUnit test walking this plugin's own
    // source tree during a Gradle build, with no live IDE/Project context to route through the Eel API at all.
    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void coreMetapackLoadingDoesNotDependOnIntellijOrSwing() throws Exception {
        Path root = Path.of("src/main/java/org/ikasan/studio/core/metapack/loading");
        try (var files = Files.walk(root)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                assertThat(text)
                        .as("platform-free metadata loader: %s", source)
                        .doesNotContain("com.intellij", "javax.swing", "org.ikasan.studio.ui");
            }
        }
    }

    private static Field[] allFields(Class<?> type) {
        return type.getDeclaredFields();
    }
}
