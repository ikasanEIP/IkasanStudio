package org.ikasan.studio.core.metapack.loading;

import org.junit.jupiter.api.Test;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentLibraryLoaderTest {
    @Test
    void loadsMetadataWithResourceIdentifiersAndNoUiObjects() {
        var components = new ComponentLibraryLoader().load("V3.3.8");

        assertThat(components).isNotEmpty();
        assertThat(components.values())
                .allMatch(component -> component.getIconResourceDirectory() != null)
                .allMatch(component -> component.getIconResourceDirectory().startsWith("studio/metapack/"));
    }

    @Test
    void simultaneousProjectsCanResolveDifferentMetapacksWithoutCrossContamination() throws Exception {
        CompletableFuture<String> v3 = CompletableFuture.supplyAsync(() -> implementingClass("V3.3.8"));
        CompletableFuture<String> v4 = CompletableFuture.supplyAsync(() -> implementingClass("V4.0.x"));

        assertThat(v3.get()).contains("javax.jms");
        assertThat(v4.get()).contains("jakarta.jms");
        assertThat(ComponentLibrary.getIkasanComponentByKeyMandatory("V3.3.8", "Basic AMQ Spring JMS Consumer")
                .getProducedOutputType()).isEqualTo("javax.jms.Message");
        assertThat(ComponentLibrary.getIkasanComponentByKeyMandatory("V4.0.x", "Basic AMQ Spring JMS Consumer")
                .getProducedOutputType()).isEqualTo("jakarta.jms.Message");
    }

    private static String implementingClass(String version) {
        try {
            return ComponentLibrary.getIkasanComponentByKeyMandatory(
                    version, "JMS Object Message To Object Converter").getExpectedInputTypes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
