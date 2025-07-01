package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowTemplateTest extends AbstractGeneratorTestFixtures {
    private static final String TEST_FLOW_NAME = "MyFlow1";
    //  ------------------------------- BROKER ----------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedBrokerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_brokerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBroker(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedBrokerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEventGeneratingConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_eventGeneratingConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_sftpConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLocalFileConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_localFileConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLocalFileConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedScheduledConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_scheduledConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getScheduledConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSpringJmsConsumerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_springJmsConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedSpringJmsConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedExceptionResolverComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_basicAmqSpringJmsConsumer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedBasicAmqSpringJmsConsumerComponent.java"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomConverterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customConverter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToObjectConverterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToObjectConverter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedObjectMessageToObjectConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToXmlStringConverterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToXmlStringConverter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java"), templateString);
    }

    // ------------------------------------- TRANSLATORS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomTranslatorComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customTranslator(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomTranslator(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedCustomTranslatorComponent.java"), templateString);
    }

    // ------------------------------------- DEBUG -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomConverterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_transitionDebugConverter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDebugTransition(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedDebugTransitionComponent.java"), templateString);
    }


    // ------------------------------------- EXCEPTION RESOLVER -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedExceptionResolverComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ExceptionResolver(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        ExceptionResolver exceptionResolver = TestFixtures.getExceptionResolver(metaPackVersion);
        String templateString = generateFlowWithExceptionResolverTemplateString(metaPackVersion, module, exceptionResolver);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, exceptionResolver, TEST_FLOW_NAME + "FullyPopulatedExceptionResolverComponent.java"), templateString);
    }

    // ------------------------------------- FILTER -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMessageFilterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageFilter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMessageFilter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedMessageFilterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMessageFilterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_defaultMessageFilter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultMessageFilter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedDefaultMessageFilterComponent.java"), templateString);
    }

    // ------------------------------------- ROUTER -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedMultiRecipientRouterComponent.java
     * The Flow functionality for a MRR bleeds partially onto subsequent components hence this test is less self contained.
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_multiRecipientRouter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, Collections.singletonList(TestFixtures.getEventGeneratingConsumerRouterFlow(metaPackVersion)));
        List<FlowElement> flowElements = module.getFlows().get(0).getFlowRoute().getFlowElements();
        BasicElement router = flowElements.get(flowElements.size() - 1);

        String templateString = generateFlowTemlateStringForModule(module);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, router, TEST_FLOW_NAME + "FullyPopulatedMultiRecipientRouterComponent.java"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_devNullProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDevNullProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEmailProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_emailProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedEmailProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedJmsProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_jmsProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getJmsProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedBasicAmqJmsProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_basicAmqJmsProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedBasicAmqJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_SftpProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedLoggingProducerComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_loggingProducer(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLoggingProducer(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }

    // ------------------------------------- SPLITTERS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedCustomComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customSplitter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomSplitter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedCustomSplitterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDefaultListSplitterComponent.java
     *
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_defaultListSplitter(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultListSplitter(metaPackVersion);
        String templateString = generateFlowTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_FLOW_NAME + "FullyPopulatedDefaultListSplitterComponent.java"), templateString);
    }
}
