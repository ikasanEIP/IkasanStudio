package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

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
    @Test
    public void testCreateModuleWith_emptyIkasanModel() throws IOException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java"), templateString);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateModuleWith_oneFlow() throws IOException, StudioBuildException {
        Flow flow1 = TestFixtures.getUnbuiltFlow().build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(Collections.singletonList(flow1));

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java"), templateString);
    }
}