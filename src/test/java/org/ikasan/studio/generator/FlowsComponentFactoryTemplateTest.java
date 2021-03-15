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
    IkasanModule ikasanModule = TestFixtures.getIkasanModule();
    IkasanFlow ikasanFlow = new IkasanFlow();
    private static String TEST_FLOW_NAME = "MyFlow1";
    private static String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @Before
    public void setUp() {
        ikasanModule = TestFixtures.getIkasanModule();
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

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpComponent.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedSftpComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSftpComponent.java")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_bespokeComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedCustomConverter(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverter.java")));
    }
}