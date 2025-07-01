package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModuleConfigTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigEmptyIkasanModel.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateModuleWith_emptyIkasanModel(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java"), templateString);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateModuleWith_oneFlow(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Flow flow1 = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, Collections.singletonList(flow1));

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java"), templateString);
    }
}