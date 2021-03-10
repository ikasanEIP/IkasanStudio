package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;
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
    public void testCreateModuleVelocityWith_emptyIkasanModel() throws IOException {
        IkasanModule ikasanModule = TestFixtures.getIkasanModule();
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
    public void testCreateModuleVelocityWith_oneFlow() throws IOException {
        IkasanModule ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.setDescription("New Module, please provide description");
        IkasanFlow ikasanFlow = new IkasanFlow();
        ikasanFlow.setName("newFlow1");
        ikasanModule.addFlow(ikasanFlow);

        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java")));
    }
}