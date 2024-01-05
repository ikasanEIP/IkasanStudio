package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.*;
import org.ikasan.studio.model.ikasan.Module;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowsBespokeComponentTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyConverter";

        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);
        FlowElement ikasanFlowComponent = FlowElement.getElement(IkasanComponentType.CUSTOM_CONVERTER, newFlow);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.NAME, "bespokeVarName");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "Beskpoke conversion");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.FROM_TYPE, "java.lang.String");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.TO_TYPE, "java.lang.Integer");

        newFlow.addFlowComponent(ikasanFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanFlowComponent);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}