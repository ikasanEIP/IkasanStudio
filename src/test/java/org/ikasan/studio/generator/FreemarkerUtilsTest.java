package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FreemarkerUtilsTest extends TestCase {

    @Test
    public void test_generateFromTemplate() throws IOException {
        Map<String, Object> configs = new HashMap<>();
        configs.putIfAbsent("className","Application");
        String templateString = FreemarkerUtils.generateFromTemplate("BasicFreemarkerTest.ftl", configs);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is("Basic Test Application\r\n"));
    }
}