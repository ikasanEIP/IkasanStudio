package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Not many components result in the need for adsitional elements in the pom.
 */
public class PomAffectingComponentsTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SFTP Consumer", "SFTP Producer"})
    void java11SftpComponentsDeclareEd25519Provider(String componentName) throws Exception {
        var component = org.ikasan.studio.core.metapack.ComponentLibrary
                .getIkasanComponentByKeyMandatory("V3.3.9", componentName);
        var providers = component.getJarDependencies().stream()
                .filter(dependency -> "org.bouncycastle".equals(dependency.getGroupId())
                        && "bcprov-jdk18on".equals(dependency.getArtifactId()))
                .toList();
        assertEquals(1, providers.size());
        assertEquals("1.85.2", providers.get(0).getVersion());
        org.junit.jupiter.api.Assertions.assertFalse(Boolean.parseBoolean(providers.get(0).getOptional()),
                "The provider must reach the generated application's runtime classpath");
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_non_default_port(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
}