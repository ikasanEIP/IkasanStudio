package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowsBespokeClassTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyConverter";

        IkasanModule ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new IkasanFlow());
        IkasanFlow newFlow = ikasanModule.getFlows().get(0);
        IkasanFlowComponent ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanComponentType.CUSTOM_CONVERTER, newFlow);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.NAME, "bespokeVarName");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "Beskpoke conversion");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.FROM_TYPE, "java.lang.String");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.TO_TYPE, "java.lang.Integer");

        newFlow.addFlowComponent(ikasanFlowComponent);
        String templateString = FlowsBespokeClassTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanFlowComponent);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}