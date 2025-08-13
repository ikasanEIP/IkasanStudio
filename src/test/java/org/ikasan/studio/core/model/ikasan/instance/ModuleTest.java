package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.META_IKASAN_PACK_3_3_8;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {

    /**
     * This test can be expanded to guarentee and even document what changes between one version and the next.
     */
    @Test
    void cloneVersion3_3_3_to_3_3_8() throws StudioBuildException {
//        Module oldModule = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, Collections.singletonList(TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK)));
        Module oldModule = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>());
        assertNotNull(oldModule);
        Module newModule = oldModule.cloneToVersion(META_IKASAN_PACK_3_3_8);
        final String jarDependencies333 = "[Dependency {groupId=com.fasterxml.jackson.core, artifactId=jackson-annotations, version=2.12.3, type=jar}, Dependency {groupId=com.fasterxml.jackson.core, artifactId=jackson-core, version=2.12.3, type=jar}, Dependency {groupId=com.fasterxml.jackson.core, artifactId=jackson-databind, version=2.12.3, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-connector-base, version=3.1.0, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=3.1.0, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-ftp-endpoint, version=3.1.0, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=3.1.0, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=3.1.0, type=jar}, Dependency {groupId=org.ikasan.studio, artifactId=ikasan-studio-ide-mediator, version=1.0.2, type=jar}]";
        final String jarDependencies338 = "[Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan.studio, artifactId=ikasan-studio-ide-mediator, version=1.0.2, type=jar}]";

        checkUnchangedProperties(oldModule, newModule, Arrays.asList("applicationPackageName", "description", "version", "flowAutoStartup", "h2DbPortNumber", "h2WebPortNumber", "port", "useEmbeddedH2"));
        assertAll(
                "Check the module contains the expected values",
                () -> assertEquals(newModule.getFlows().size(), oldModule.getFlows().size()),
                () -> assertEquals(BASE_META_PACK, oldModule.getVersion()),
                () -> assertEquals(META_IKASAN_PACK_3_3_8, newModule.getVersion()),

                // componentMeta
                () -> assertThat(newModule.getComponentMeta())
                        .usingRecursiveComparison()
                        .ignoringFields("jarDependencies")
                        .withEqualsForType(
                                (ImageIcon a, ImageIcon b) -> {
                                    if (a == null && b == null) return true;
                                    if (a == null || b == null) return false;

                                    // Example: compare by image dimensions
                                    return a.getIconWidth() == b.getIconWidth()
                                            && a.getIconHeight() == b.getIconHeight();
                                },
                                ImageIcon.class
                        )
                        .isEqualTo(oldModule.getComponentMeta()),
                () -> assertEquals(jarDependencies333, new TreeSet<>(oldModule.getComponentMeta().getJarDependencies().stream().map(Dependency::toString).collect(Collectors.toList())).toString()),
                () -> assertEquals(jarDependencies338, new TreeSet<>(newModule.getComponentMeta().getJarDependencies().stream().map(Dependency::toString).collect(Collectors.toList())).toString())
        );
    }

    /**
     * Iterate over the properties and check that should not have changed between old version and new version.
     *
     * @param oldModule  the module from the old version
     * @param newModule  the module from the new version
     * @param properties the list of properties to check
     */
    private void checkUnchangedProperties(Module oldModule, Module newModule, List<String> properties) {
        for (String property : properties) {
            assertEquals(oldModule.getProperty(property).getMeta(), newModule.getProperty(property).getMeta(), "Property '" + property + "' should not have changed");
        }
    }
}