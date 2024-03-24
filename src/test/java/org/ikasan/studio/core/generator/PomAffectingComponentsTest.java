package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Not many components result in the need for adsitional elements in the pom.
 */
public class PomAffectingComponentsTest extends GeneratorTests {

    @BeforeEach
    public void setUp() throws StudioBuildException {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow_with_non_default_port() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter();
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
    


}