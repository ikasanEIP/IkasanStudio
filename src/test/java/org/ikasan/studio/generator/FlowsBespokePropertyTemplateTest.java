package org.ikasan.studio.generator;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FlowsBespokePropertyTemplateTest {
    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyBespokeClassForProperty.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyBespokeClassForProperty";

        // Wait till refactor
//        ComponentPropertyMeta ikasanComponentPropertyMeta = new ComponentPropertyMeta(1, false, false, true, true, false,
//                "MessageProviderPostProcessor", null, String.class, "org.ikasan.component.endpoint.filesystem.messageprovider.MessageProviderPostProcessor", null, null, null, null);
//        ComponentProperty ikasanComponentProperty = new ComponentProperty(ikasanComponentPropertyMeta, clazzName);
//
//        String templateString = FlowsBespokePropertyTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, clazzName, ikasanComponentProperty, "");
//
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}