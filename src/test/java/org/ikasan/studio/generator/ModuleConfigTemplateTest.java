package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.Flow;
import org.ikasan.studio.model.ikasan.Module;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ModuleConfigTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigEmptyIkasanModel.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateModuleWith_emptyIkasanModel() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.setDescription("New Module, please provide description");

        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateModuleWith_oneFlow() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.setDescription("New Module, please provide description");
        Flow ikasanFlow = new Flow();
        ikasanFlow.setComponentName("newFlow1");
        ikasanModule.addFlow(ikasanFlow);

        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java")));
    }
}