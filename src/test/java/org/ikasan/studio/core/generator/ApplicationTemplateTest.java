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

public class ApplicationTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/Application.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void test_generateApplicationClass(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = ApplicationTemplate.create(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, ikasanModule, ApplicationTemplate.APPLICATION_CLASS_NAME + ".java"), templateString);
    }
}