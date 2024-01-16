package org.ikasan.studio.generator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FreemarkerUtilsTest {

    @Test
    public void test_generateFromTemplate() throws IOException {
        Map<String, Object> configs = new HashMap<>();
        configs.putIfAbsent("className","Application");
        String templateString = FreemarkerUtils.generateFromTemplate("basicFreemarkerTest.ftl", configs);
        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is("Basic Test Application\n"));
    }
}