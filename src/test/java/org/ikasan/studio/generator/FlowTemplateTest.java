package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.Flow;
import org.ikasan.studio.model.ikasan.Module;
import org.junit.Before;

public class FlowTemplateTest extends TestCase {
    Module ikasanModule = TestFixtures.getIkasanModule();
    Flow ikasanFlow = new Flow();
    private static String TEST_FLOW_NAME = "MyFlow1";

    @Before
    public void setUp() {
        ikasanModule = TestFixtures.getIkasanModule();
        ikasanFlow = new Flow();
        ikasanFlow.setComponentName(TEST_FLOW_NAME);
        ikasanFlow.setDescription("MyFlowDescription");
    }

//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1OneFlow.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_oneFlow() throws IOException {
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "OneFlow.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1EventGeneratingConsumer.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
//        List<FlowElement> components = ikasanFlow.getFlowComponentList() ;
//        FlowElement component = FlowElement.getElement(IkasanComponentTypeMeta.EVENT_GENERATING_CONSUMER, ikasanFlow);
//        component.setComponentName("testEventGeneratingConsumer");
//        components.add(component);
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "EventGeneratingConsumer.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ftpConsumer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedFtpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ftpProducer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedFtpProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_sftpConsumer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedSftpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_sftpProducer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedSftpProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedScheduledConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ScheduledConsumer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedScheduledConsumerComponent.java")));
//    }
//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_devNullProducer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedDevNullProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEmailProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_emailProducer() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedEmailProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedEmailProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToXmlStringConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_objectMessageToXmlStringConverterComponent() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedObjectMessageToXmlStringConverterComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedXmlStringObjectMessageConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_XmlStringObjectMessageConverterComponent() throws IOException {
//        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedXmlStringObjectMessageConverterComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedXmlStringObjectMessageConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1WithExceptionResolver.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_exceptionResolver() throws IOException {
//        TestFixtures.populateFlowExceptionResolver(ikasanFlow);
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        Assert.assertThat(templateString, is(notNullValue()));
//        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "WithExceptionResolver.java")));
//    }
}
