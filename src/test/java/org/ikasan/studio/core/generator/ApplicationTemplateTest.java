package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApplicationTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/Application.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void test_generateApplicationClass() throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());

        String templateString = ApplicationTemplate.generateContents(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ikasanModule, ApplicationTemplate.APPLICATION_CLASS_NAME + ".java"), templateString);
    }
}