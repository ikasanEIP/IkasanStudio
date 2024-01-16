package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.Module;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/Application.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void test_generateApplicationClass() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();

        String templateString = ApplicationTemplate.generateContents(ikasanModule);

        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(ApplicationTemplate.APPLICATION_CLASS_NAME + ".java")));
    }
}