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
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_bespokeComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedCustomConverterComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverter.java")));
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedFtpConsumerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpConsumerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpConsumerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedFtpProducerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpProducerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        System.out.println(templateString);
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedSftpConsumerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSftpConsumerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedFilterComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFilterComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFilterComponent.java")));
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedSftpProducerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpProducerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java")));
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedLocalFileConsumerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedLocalFileConsumerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedLocalFileConsumerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedScheduledConsumerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedScheduledConsumerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedScheduledConsumerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedJmsConsumerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSpringJmsConsumerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSpringJmsConsumerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedJmsProducerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedJmsProducerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedJmsProducerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedDevNullProducerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedDevNullProducerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedDevNullProducerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEmailProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedEmailProducerComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedEmailProducerComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedEmailProducerComponent.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToXmlStringConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedObjectMessageToXmlStringConverterComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedObjectMessageToXmlStringConverterComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java")));
    }
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedXmlStringObjectMessageConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_fullyPopulatedXmlStringObjectMessageConverterComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedXmlStringObjectMessageConverterComponent(ikasanFlow));

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedXmlStringObjectMessageConverterComponent.java")));
    }
}