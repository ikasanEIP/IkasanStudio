package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlowsMessageFilterTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    @Disabled
    public void testCreateWith_fullyPopulatedFilterComponent() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);
        newFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFilterComponent(newFlow));
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfiguredResource.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    @Disabled
    public void testCreateWith_minimumPopulatedFilterComponent() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);
        newFlow.getFlowComponentList().add(TestFixtures.getMinimumPopulatedFilterComponent(newFlow));
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterMinimum.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyMessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    @Disabled
    public void testCreateWith_filterComponentConfiguredResourceIDButNoIsConfiguredResource() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);

        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(newFlow);
        filterFlowComponent.setPropertyValue("IsConfiguredResource", false);

        newFlow.getFlowComponentList().add(filterFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        assertThat(templateString, is(notNullValue()));
                assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfigured.java")));
    }

    @Test
    @Disabled
    public void testCreateWith_filterComponentIsConfiguredResourceButNoConfiguredResourceID() throws IOException {
        Module ikasanModule = TestFixtures.getIkasanModule();
        ikasanModule.addFlow(new Flow());
        Flow newFlow = ikasanModule.getFlows().get(0);

        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(newFlow);
        filterFlowComponent.setPropertyValue("ConfiguredResourceId", null);

        newFlow.getFlowComponentList().add(filterFlowComponent);
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, newFlow.getFlowComponentList().get(0));
        assertThat(templateString, is(notNullValue()));
        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile("MyMessageFilterConfiguredResource.java")));
    }
}