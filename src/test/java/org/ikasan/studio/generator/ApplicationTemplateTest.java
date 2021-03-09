package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ApplicationTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyFlow1OneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void test_generateApplicationClass() throws IOException {
        IkasanModule ikasanModule = TestFixtures.getIkasanModule();

        String templateString = ApplicationTemplate.generateContents(ikasanModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ApplicationTemplate.APPLICATION_CLASS_NAME + ".java")));
    }
}