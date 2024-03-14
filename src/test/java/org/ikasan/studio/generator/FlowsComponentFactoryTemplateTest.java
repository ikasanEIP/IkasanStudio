package org.ikasan.studio.generator;

import org.ikasan.studio.TestFixtures;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowsComponentFactoryTemplateTest extends GeneratorTests {
    private static final String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }


    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }



    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }


    // ------------------------------------- FILTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/ComponentFactoryFullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilterComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedMessageFilterComponent.java"), templateString);
    }


    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverterComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpProducerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducerComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }
}