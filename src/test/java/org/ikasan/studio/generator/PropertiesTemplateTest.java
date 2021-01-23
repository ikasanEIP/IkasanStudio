package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PropertiesTemplateTest extends TestCase {

    @Test
    public void testCreatePropertiesVelocity() throws IOException {

        String templateString = PropertiesTemplate.createPropertiesVelocity();
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + ".properties")));
    }
}