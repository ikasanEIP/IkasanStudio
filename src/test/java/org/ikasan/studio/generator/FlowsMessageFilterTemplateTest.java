package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;
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
        newFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFilterComponent(newFlow));
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
}