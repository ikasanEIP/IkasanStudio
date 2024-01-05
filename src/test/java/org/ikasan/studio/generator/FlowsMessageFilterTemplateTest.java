package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.Flow;
import org.ikasan.studio.model.ikasan.FlowElement;
import org.ikasan.studio.model.ikasan.Module;
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
    public void testCreateWith_fullyPopulatedFilterComponent() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);
        newFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFilterComponent(newFlow));
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfiguredResource.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_minimumPopulatedFilterComponent() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);
        newFlow.getFlowComponentList().add(TestFixtures.getMinimumPopulatedFilterComponent(newFlow));
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterMinimum.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_filterComponentConfiguredResourceIDButNoIsConfiguredResource() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);

        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(newFlow);
        filterFlowComponent.setPropertyValue("IsConfiguredResource", false);

        newFlow.getFlowComponentList().add(filterFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        Assert.assertThat(templateString, is(notNullValue()));
                Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfigured.java")));
    }

    @Test
    public void testCreateWith_filterComponentIsConfiguredResourceButNoConfiguredResourceID() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);

        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(newFlow);
        filterFlowComponent.setPropertyValue("ConfiguredResourceId", null);

        newFlow.getFlowComponentList().add(filterFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfiguredResource.java")));
    }
}