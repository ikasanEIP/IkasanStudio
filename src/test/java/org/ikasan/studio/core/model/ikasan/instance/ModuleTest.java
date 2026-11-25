package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioComparitors;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void warmUpComponentLibrary() throws StudioBuildException {
        // Loading the meta-pack lazily starts process-lifetime IntelliJ/NIO daemon threads.
        // Start them before the per-test ThreadLeakTracker captures its thread baseline.
        ComponentLibrary.refreshComponentLibrary(BASE_META_PACK);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void h2DefaultsFollowHttpOnlyWhileUnset(String pack) throws Exception {
        Module module = new Module(pack);
        module.setPort("12410"); module.defaultUnsetMandatoryProperties();
        assertEquals("12411", module.getH2PortNumber()); assertEquals("12412", module.getH2WebPortNumber());
        assertTrue(module.hasAutomaticH2Default("h2DbPortNumber"));
        assertFalse(org.ikasan.studio.core.io.ComponentIO.toJson(module).contains("automaticH2Defaults"));
        module.setPort("12500"); module.defaultUnsetMandatoryProperties();
        assertEquals("12411", module.getH2PortNumber()); assertEquals("12412", module.getH2WebPortNumber());
        module.setH2DbPortNumber("23000"); module.setH2WebPortNumber(null); module.defaultUnsetMandatoryProperties();
        assertEquals("23000", module.getH2PortNumber()); assertEquals("12502", module.getH2WebPortNumber());
        assertFalse(module.hasAutomaticH2Default("h2DbPortNumber"));
    }

    @Test void h2DefaultsUseDeclaredHttpDefaultAndNeverOverflow() throws Exception {
        Module module = new Module(BASE_META_PACK); module.defaultUnsetMandatoryProperties();
        assertEquals(Integer.toString(Integer.parseInt(module.getPort()) + 1), module.getH2PortNumber());
        assertEquals(Integer.toString(Integer.parseInt(module.getPort()) + 2), module.getH2WebPortNumber());
        assertEquals("65535", Module.derivedH2Port("65534", 1));
        assertNull(Module.derivedH2Port("65534", 2)); assertNull(Module.derivedH2Port("65535", 1));
        for (String invalid : List.of("", "${PORT}", "-1", "0", "65536", "123456789012345")) assertNull(Module.derivedH2Port(invalid, 1));
    }

    /**
     * This test can be expanded to guarentee and even document what changes between one version and the next.
     */
    @Test
    void cloneVersionOlderToNewer() throws StudioBuildException {
        Module oldModule = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>());
        assertNotNull(oldModule);
        Module newModule = oldModule.cloneToVersion(META_IKASAN_PACK_4_1_6);
        final String jarDependenciesOld = "[Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=null, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=null, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=null, type=jar}]";
        final String jarDependenciesNew = "[Dependency {groupId=org.ikasan, artifactId=ikasan-eip-standalone, version=null, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-h2-standalone-persistence, version=null, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test, version=null, type=jar}, Dependency {groupId=org.ikasan, artifactId=ikasan-test-endpoint, version=null, type=jar}]";

        checkUnchangedProperties(oldModule, newModule, Arrays.asList("applicationPackageName", "description", "flowStartupType", "h2DbPortNumber", "h2WebPortNumber", "port", "useEmbeddedH2"));
        assertAll(
                "Check the module contains the expected values",
                () -> assertEquals(newModule.getFlows().size(), oldModule.getFlows().size()),
                () -> assertEquals(BASE_META_PACK, oldModule.getVersion()),
                () -> assertEquals(META_IKASAN_PACK_4_1_6, newModule.getVersion()),

                // componentMeta
                () -> assertThat(newModule.getComponentMeta())
                        .usingRecursiveComparison()
                        .ignoringFields("jarDependencies", "iconResourceDirectory", "allowableProperties.version.choices")
                        .withEqualsForType(StudioComparitors::imageIconsEqual, Icon.class)
                        // com.google.re2j.Pattern has no equals(), so compare validation patterns by their text.
                        .withComparatorForType(java.util.Comparator.nullsFirst(java.util.Comparator.comparing(com.google.re2j.Pattern::pattern)), com.google.re2j.Pattern.class)
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
