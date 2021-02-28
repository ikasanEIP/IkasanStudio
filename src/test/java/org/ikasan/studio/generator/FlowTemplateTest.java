package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponentType;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowTemplateTest extends TestCase {
    IkasanModule ikasanModule = new IkasanModule();
    IkasanFlow ikasanFlow = new IkasanFlow();
    private static String TEST_FLOW_NAME = "MyFlow1";

    @Before
    public void setUp() {
        ikasanModule = new IkasanModule();
        ikasanFlow = new IkasanFlow();
        ikasanFlow.setName(TEST_FLOW_NAME);
        ikasanFlow.setDescription("MyFlowDescription");
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyFlow1OneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_oneFlow() throws IOException {
        String templateString = FlowTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_FLOW_NAME + "OneFlow.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyFlow1_eventGeneratingConsumert.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
        List<IkasanFlowComponent> components = ikasanFlow.getFlowComponentList() ;
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanFlowComponentType.EVENT_GENERATING_CONSUMER, ikasanFlow);
        component.setName("testEventGeneratingConsumer");
        components.add(component);

        String templateString = FlowTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_FLOW_NAME + "EventGeneratingConsumer.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpComponent(ikasanFlow));

        String templateString = FlowTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedFtpComponent.java")));
    }

}
