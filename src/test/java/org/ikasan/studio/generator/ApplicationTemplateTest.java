package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ApplicationTemplateTest extends TestCase {

    @Test
    public void test_generateApplicationClass() throws IOException {
        String templateString = ApplicationTemplate.generateContents();
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(ApplicationTemplate.APPLICATION_CLASS_NAME + ".java")));
    }
}