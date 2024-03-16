package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class PropertiesTemplateTest extends GeneratorTests {

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow_with_non_default_port() throws IOException {
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
    
    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedEventGeneratingConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEventGeneratingConsumerComponent.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedFtpConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpConsumerComponent.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedSftpConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpConsumerComponent.properties"), templateString);
    }


    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedScheduledConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumer() throws IOException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedScheduledConsumerComponent.properties"), templateString);
    }

    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also application_fullyPopulatedMessageFilterComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilter() throws IOException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedMessageFilterComponent.properties"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also application_fullyPopulatedCustomConverterComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverter() throws IOException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedCustomConverterComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedObjectMessageToObjectConverterComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToObjectConverter() throws IOException {
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedObjectMessageToObjectConverterComponent.properties"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also application_fullyPopulatedDevNullProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedDevNullProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_SftpProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLoggingProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducer() throws IOException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLoggingProducerComponent.properties"), templateString);
    }




}