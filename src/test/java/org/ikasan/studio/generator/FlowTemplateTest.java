package org.ikasan.studio.generator;

import org.ikasan.studio.TestFixtures;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowTemplateTest extends GeneratorTests {
    private static final String TEST_FLOW_NAME = "MyFlow1";

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());

    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }


    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }

    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilter() throws IOException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedMessageFilterComponent.java"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverter() throws IOException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_SftpProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generateFlowTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_FLOW_NAME + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }
}
