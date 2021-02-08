package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class BespokeClassTemplateTest extends TestCase {

    @Test
    public void testCreateWith_Converter() throws IOException {
        String clazzName = "MyConverter";

        IkasanModule ikasanModule = new IkasanModule();
        ikasanModule.addAnonymousFlow(new IkasanFlow());
        IkasanFlow newFlow = ikasanModule.getFlows().get(0);
        IkasanFlowComponent ikasanFlowComponent = new IkasanFlowComponent(IkasanFlowComponentType.CUSTOM_CONVERTER, newFlow);
        ikasanFlowComponent.setPropertyValue(IkasanComponentPropertyMeta.NAME, "bespokeVarName");
        ikasanFlowComponent.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "Beskpoke conversion");
        ikasanFlowComponent.setPropertyValue(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
        ikasanFlowComponent.setPropertyValue(IkasanComponentPropertyMeta.FROM_TYPE, "java.lang.String");
        ikasanFlowComponent.setPropertyValue(IkasanComponentPropertyMeta.TO_TYPE, "java.lang.Integer");

        newFlow.addFlowComponent(ikasanFlowComponent);
        String templateString = BespokeClassTemplate.generateContents(ikasanFlowComponent);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(clazzName + ".java")));
    }
}