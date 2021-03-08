package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class BespokeClassTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyConverter";

        IkasanModule ikasanModule = new IkasanModule();
        ikasanModule.addAnonymousFlow(new IkasanFlow());
        IkasanFlow newFlow = ikasanModule.getFlows().get(0);
        IkasanFlowComponent ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanFlowComponentType.CUSTOM_CONVERTER, newFlow);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.NAME, "bespokeVarName");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "Beskpoke conversion");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.FROM_TYPE, "java.lang.String");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.TO_TYPE, "java.lang.Integer");

        newFlow.addFlowComponent(ikasanFlowComponent);
        String templateString = BespokeClassTemplate.generateContents(ikasanFlowComponent);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(clazzName + ".java")));
    }
}