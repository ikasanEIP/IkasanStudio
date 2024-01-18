package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ModuleConfigTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigEmptyIkasanModel.java
     * @throws IOException if the template cant be generated
     */
    @Test
    @Disabled
    public void testCreateModuleWith_emptyIkasanModel() throws IOException {
        System.out.println("Wait till refector");
//        Module ikasanModule = TestFixtures.getIkasanModule();
//        ikasanModule.setDescription("New Module, please provide description");
//
//        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Test
    @Disabled
    public void testCreateModuleWith_oneFlow() throws IOException {
        System.out.println("Wait till refector");

//        Module ikasanModule = TestFixtures.getIkasanModule();
//        ikasanModule.setDescription("New Module, please provide description");
//        Flow ikasanFlow = new Flow();
//        ikasanFlow.setComponentName("newFlow1");
//        ikasanModule.addFlow(ikasanFlow);
//
//        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java")));
    }
}