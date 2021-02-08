package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ModuleConfigTemplateTest extends TestCase {

    @Test
    public void testCreateModuleVelocityWith_emptyIkasanModel() throws IOException {
        IkasanModule ikasanModule = new IkasanModule();

        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "_emptyIkasanModel.java")));
    }

    @Test
    public void testCreateModuleVelocityWith_oneFlow() throws IOException {
        IkasanModule ikasanModule = new IkasanModule();
        ikasanModule.addAnonymousFlow(new IkasanFlow());

        String templateString = ModuleConfigTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(ModuleConfigTemplate.MODULE_CLASS_NAME + "_oneFlow.java")));
    }
}