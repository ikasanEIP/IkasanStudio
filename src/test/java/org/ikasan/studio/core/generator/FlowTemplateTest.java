package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowTemplateTest extends GeneratorTests {
    private static final String TEST_FLOW_NAME = "MyFlow1";

    @BeforeEach
    public void setUp() throws StudioBuildException {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());

    }

    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedBrokerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_brokerComponent() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getBroker();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedBrokerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }

   /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_springJmsConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedSpringJmsConsumerComponent.java"), templateString);
    }

  /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedExceptionResolverComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_basicAmqSpringJmsConsumer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedBasicAmqSpringJmsConsumerComponent.java"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverter() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToObjectConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToObjectConverter() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedObjectMessageToObjectConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToXmlStringConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToXmlStringConverter() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java"), templateString);
    }

    // ------------------------------------- EXCEPTION RESOLVER -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedExceptionResolverComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ExceptionResolver() throws IOException, StudioBuildException {
        ExceptionResolver exceptionResolver = TestFixtures.getExceptionResolver();
        String templateString = generateFlowWithExceptionResolverTemplateString(exceptionResolver);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(exceptionResolver, TEST_FLOW_NAME + "FullyPopulatedExceptionResolverComponent.java"), templateString);
    }

    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilter() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedMessageFilterComponent.java"), templateString);
    }
//
//    // ------------------------------------- ROUTER -------------------------------------
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMultiRecipientRouterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_multiRecipientRouter() throws IOException, StudioBuildException {
//        FlowElement flowElement = TestFixtures.getMultiRecipientRouter();
//        String templateString = generateFlowTemplateString(flowElement);
//        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedMultiRecipientRouterComponent.java"), templateString);
//    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEmailProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_emailProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getEmailProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedEmailProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_jmsProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getJmsProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedBasicAmqJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_basicAmqJmsProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedBasicAmqJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_SftpProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducer() throws IOException, StudioBuildException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }
}
