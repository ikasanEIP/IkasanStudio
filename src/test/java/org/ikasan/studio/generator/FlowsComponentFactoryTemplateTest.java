package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowsComponentFactoryTemplateTest extends TestCase {
    IkasanModule ikasanModule = new IkasanModule();
    IkasanFlow ikasanFlow = new IkasanFlow();
    private static String TEST_FLOW_NAME = "MyFlow1";
    private static String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @Before
    public void setUp() {
        ikasanModule = new IkasanModule();
        ikasanFlow = new IkasanFlow();
        ikasanFlow.setName(TEST_FLOW_NAME);
        ikasanFlow.setDescription("MyFlowDescription");
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedFtpComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpComponent.java")));
    }
}