package org.ikasan.studio.architecture;

import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SharedMutableStateBoundaryTest {

    @Test
    void twoSelectionsResolveDifferentIkasanVersionsConcurrently() throws Exception {
        ComponentMeta v3 = ComponentLibrary.getIkasanComponentByKeyMandatory(
                "V3.3.9", "Basic AMQ Spring JMS Consumer");
        ComponentMeta v4 = ComponentLibrary.getIkasanComponentByKeyMandatory(
                "V4.1.6", "Basic AMQ Spring JMS Consumer");

        assertThat(v3).isNotSameAs(v4);
        assertThat(v3.getProducedOutputType()).isEqualTo("javax.jms.Message");
        assertThat(v4.getProducedOutputType()).isEqualTo("jakarta.jms.Message");
    }

    @Test
    void componentLibraryHasNoRuntimeMutableStaticCache() {
        for (Field field : ComponentLibrary.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                assertThat(Modifier.isFinal(field.getModifiers())).as(field.getName()).isTrue();
                assertThat(field.getName()).as(field.getName()).isEqualTo("PACKAGED_LIBRARIES");
            }
        }
    }

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void projectPathsTemplatesAndIconsHaveNoPluginLevelCaches() throws Exception {
        assertThat(Files.readString(Path.of("src/main/java/org/ikasan/studio/intellij/project/StudioProjectFiles.java")))
                .doesNotContain("static final Map<String, VirtualFile>", "static final Map<String, Path>");
        assertThat(Files.readString(Path.of("src/main/java/org/ikasan/studio/core/BuildContext.java")))
                .doesNotContain("configCache", "ConcurrentHashMap");
        assertThat(Files.readString(Path.of("src/main/java/org/ikasan/studio/ui/icons/ComponentIconProvider.java")))
                .doesNotContain("ICON_CACHE", "ConcurrentHashMap");
    }

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void productionCodeOnlyEnrichesMetadataInsideTheLoader() throws Exception {
        Path root = Path.of("src/main/java");
        try (var files = Files.walk(root)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.endsWith("ComponentLibraryLoader.java")) continue;
                String text = Files.readString(source);
                assertThat(text).as(source.toString())
                        .doesNotContain("getComponentMeta().getAllowableProperties().get(VERSION).setChoices");
            }
        }
    }
}
