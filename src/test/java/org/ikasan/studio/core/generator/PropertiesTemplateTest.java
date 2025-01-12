package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class PropertiesTemplateTest extends AbstractGeneratorTestFixtures {

    @BeforeEach
    public void setUp() throws StudioBuildException {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow_with_non_default_port() throws IOException, StudioGeneratorException {
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow_with_use_embeddedH2() throws IOException, StudioGeneratorException {
        module.setPropertyValue("useEmbeddedH2", true);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow_useEmbeddedH2.properties"), templateString);
    }
    
    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow_with_use_embeddedH2_null() throws IOException, StudioGeneratorException {
        module.setPropertyValue("useEmbeddedH2", null);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedEventGeneratingConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEventGeneratingConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }
    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumerMandatoryOnly() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumerMandatoryOnly();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedScheduledConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedScheduledConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpConsumerComponent.properties"), templateString);
    }


    /**
     * See also application_fullyPopulatedSpringJmsConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_springJmsConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSpringJmsConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedBasicAmqSpringJmsConsumerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_basicAmqSpringJmsConsumer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedBasicAmqSpringJmsConsumerComponent.properties"), templateString);
    }


    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also application_fullyPopulatedMessageFilterComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilter() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedMessageFilterComponent.properties"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also application_fullyPopulatedCustomConverterComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverter() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedCustomConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
    }

    /**
     * See also application_fullyPopulatedObjectMessageToObjectConverterComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToObjectConverter() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter();
        assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedObjectMessageToObjectConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
    }

    /**
     * See also application_fullyPopulatedObjectMessageToXmlStringConverterComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToXmlStringConverter() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter();
        assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedObjectMessageToXmlStringConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also application_fullyPopulatedDevNullProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedDevNullProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedEmailProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_emailProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getEmailProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEmailProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedJmsProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_jmsProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getJmsProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedJmsProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedBasicAmqJmsProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_basicAmqJmsProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedBasicAmqJmsProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_SftpProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLoggingProducerComponent.properties
     * @throws IOException, StudioGeneratorException  if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducer() throws IOException, StudioGeneratorException , StudioBuildException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generatePropertiesTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLoggingProducerComponent.properties"), templateString);
    }




}