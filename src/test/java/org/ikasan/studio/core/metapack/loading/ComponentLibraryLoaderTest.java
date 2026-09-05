package org.ikasan.studio.core.metapack.loading;

import org.junit.jupiter.api.Test;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentLibraryLoaderTest {
    @Test
    void loadsMetadataWithResourceIdentifiersAndNoUiObjects() {
        var components = new ComponentLibraryLoader().load("V3.3.9");

        assertThat(components).isNotEmpty();
        assertThat(components.values())
                .allMatch(component -> component.getIconResourceDirectory() != null)
                .allMatch(component -> component.getIconResourceDirectory().startsWith("studio/metapack/"));
    }

    @Test
    void loadsTheVersionAndBomContractFromEachPackManifest() throws Exception {
        var v3 = ComponentLibrary.getMetaPackManifest("V3.3.9");
        var v4 = ComponentLibrary.getMetaPackManifest("V4.1.6");

        assertThat(v3.ikasanVersion()).isEqualTo("3.3.9");
        assertThat(v3.javaVersion()).isEqualTo("11");
        assertThat(v4.ikasanVersion()).isEqualTo("4.1.6");
        assertThat(v4.javaVersion()).isEqualTo("17");
        assertThat(v4.dependencyManagement()).singleElement().satisfies(bom -> {
            assertThat(bom.artifactId()).isEqualTo("ikasan-eip-standalone-bom");
            assertThat(bom.version()).isEqualTo("4.1.6");
        });
    }

    @Test
    void packagedDependenciesAreBomManagedUnlessDeclaredAsOverrides() throws Exception {
        ComponentLibraryLoader loader = new ComponentLibraryLoader();
        for (String version : java.util.List.of("V3.3.9", "V4.1.6")) {
            var components = loader.load(version);
            var manifest = loader.loadManifest(version);
            org.ikasan.studio.core.metapack.validation.MetaPackValidator.validate(version, manifest, components);
            assertThat(components.values()).allSatisfy(component -> {
                if (component.getJarDependencies() != null) {
                    assertThat(component.getJarDependencies())
                            .allMatch(dependency -> dependency.getVersion() == null);
                }
            });
        }
    }

    @Test
    void simultaneousProjectsCanResolveDifferentMetapacksWithoutCrossContamination() throws Exception {
        CompletableFuture<String> v3 = CompletableFuture.supplyAsync(() -> implementingClass("V3.3.9"));
        CompletableFuture<String> v4 = CompletableFuture.supplyAsync(() -> implementingClass("V4.1.6"));

        assertThat(v3.get()).contains("javax.jms");
        assertThat(v4.get()).contains("jakarta.jms");
        assertThat(ComponentLibrary.getIkasanComponentByKeyMandatory("V3.3.9", "Basic AMQ Spring JMS Consumer")
                .getProducedOutputType()).isEqualTo("javax.jms.Message");
        assertThat(ComponentLibrary.getIkasanComponentByKeyMandatory("V4.1.6", "Basic AMQ Spring JMS Consumer")
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
