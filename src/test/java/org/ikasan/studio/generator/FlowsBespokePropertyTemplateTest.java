package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlowsBespokePropertyTemplateTest {
    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyBespokeClassForProperty.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyBespokeClassForProperty";
        IkasanComponentPropertyMeta ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(1, false, false, true, true, false,
                "MessageProviderPostProcessor", null, String.class, "org.ikasan.component.endpoint.filesystem.messageprovider.MessageProviderPostProcessor", null, null, null, null);
        IkasanComponentProperty ikasanComponentProperty = new IkasanComponentProperty(ikasanComponentPropertyMeta, clazzName);

        String templateString = FlowsBespokePropertyTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, clazzName, ikasanComponentProperty, "");

        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}