package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowsMessageFilterTemplateTest extends TestCase {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_MessageFilter() throws IOException {
        String clazzName = "MyMessageFilter";

        IkasanModule ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new IkasanFlow());
        IkasanFlow newFlow = ikasanModule.getFlows().get(0);
        IkasanFlowComponent ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanComponentType.MESSAGE_FILTER, newFlow);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.NAME, "bespokeVarName");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "Beskpoke message filter");
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
        ikasanFlowComponent.updatePropertyValue(IkasanComponentPropertyMeta.FROM_TYPE, "java.lang.String");

        newFlow.addFlowComponent(ikasanFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanFlowComponent);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}