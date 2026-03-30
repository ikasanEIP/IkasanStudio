package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioComparitors;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ikasan.studio.core.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {

    /**
     * This test can be expanded to guarentee and even document what changes between one version and the next.
     */
    @Test
    void cloneVersionOlderToNewer() throws StudioBuildException {
        Module oldModule = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>());
        assertNotNull(oldModule);
        Module newModule = oldModule.cloneToVersion(META_IKASAN_PACK_4_0_0);
        final String jarDependenciesOld = "[Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=3.3.8, type=jar}, Dependency {groupId=org.ikasan.studio, artifactId=ikasan-studio-ide-mediator, version=1.0.2, type=jar}]";
        final String jarDependenciesNew = "[Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=4.1.1, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=4.1.1, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test, version=4.1.1, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=4.1.1, type=jar}, Dependency {groupId=org.ikasan.studio, artifactId=ikasan-studio-ide-mediator, version=1.0.2, type=jar}]";

        checkUnchangedProperties(oldModule, newModule, Arrays.asList("applicationPackageName", "description", "version", "flowStartupType", "h2DbPortNumber", "h2WebPortNumber", "port", "useEmbeddedH2"));
        assertAll(
                "Check the module contains the expected values",
                () -> assertEquals(newModule.getFlows().size(), oldModule.getFlows().size()),
                () -> assertEquals(BASE_META_PACK, oldModule.getVersion()),
                () -> assertEquals(META_IKASAN_PACK_4_0_0, newModule.getVersion()),

                // componentMeta
                () -> assertThat(newModule.getComponentMeta())
                        .usingRecursiveComparison()
                        .ignoringFields("jarDependencies")
                        .withEqualsForType(StudioComparitors::imageIconsEqual, ImageIcon.class)
                        .isEqualTo(oldModule.getComponentMeta()),
                () -> assertEquals(jarDependenciesOld, new TreeSet<>(oldModule.getComponentMeta().getJarDependencies().stream().map(Dependency::toString).collect(Collectors.toList())).toString()),
                () -> assertEquals(jarDependenciesNew, new TreeSet<>(newModule.getComponentMeta().getJarDependencies().stream().map(Dependency::toString).collect(Collectors.toList())).toString())
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
            Object oldMeta = (oldModule.getProperty(property) != null) ? oldModule.getProperty(property).getMeta() : null;
            Object newMeta = (newModule.getProperty(property) != null) ? newModule.getProperty(property).getMeta() : null;
            assertEquals(oldMeta, newMeta, "Meta for property '" + property + "' should not have changed, old property = " + oldMeta + " new property = " + newMeta);
        }
    }
}