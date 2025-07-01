package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
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
    /**
     * See also application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_non_default_port(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter(metaPackVersion);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
}