package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowsComponentFactoryTemplateTest extends AbstractGeneratorTestFixtures {
    private static final String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @BeforeEach
    public void setUp() throws StudioBuildException {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }


    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBrokerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_brokerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getBroker();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBrokerComponent.java"), templateString);
    }

    @Test
    public void testCreateFlowWith_genericBrokerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getGenericBroker();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedGenericBrokerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }



    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getFtpConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getSftpConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getLocalFileConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getScheduledConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }

   /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_springJmsConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSpringJmsConsumerComponent.java"), templateString);
    }

   /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBasicAmqSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_BasicAmqSpringJmsConsumerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBasicAmqSpringJmsConsumerComponent.java"), templateString);
    }


    // ------------------------------------- FILTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/ComponentFactoryFullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
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
    public void testCreateFlowWith_customConverterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToObjectConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToObjectConverterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToObjectConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToXmlStringConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_objectMessageToXmlStringConverterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java"), templateString);
    }

    // ------------------------------------- TRANSLATORS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomTranslatorComponent.java
     * @throws IOException if the template can't be generated
     */
    @Test
    public void testCreateFlowWith_customTranslatorComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getCustomTranslator();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomTranslatorComponent.java"), templateString);
    }

    // ------------------------------------- DEBUG -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_debugTransitionComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getDebugTransition();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDebugTransitionComponent.java"), templateString);
    }



    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getDevNullProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEmailProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_emailProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getEmailProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEmailProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_jmsProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getJmsProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedJmsProducerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBasicAmqJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_basicAmqJmsProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBasicAmqJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getFtpProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getSftpProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getLoggingProducer();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }

    // ------------------------------------- SPLITTERS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomSplitterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customSplitterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getCustomSplitter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomSplitterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDefaultListSplitterComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_defaultListSplitterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getDefaultListSplitter();
        String templateString = generateFlowsComponentFactoryTemplateString(flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDefaultListSplitterComponent.java"), templateString);
    }
}