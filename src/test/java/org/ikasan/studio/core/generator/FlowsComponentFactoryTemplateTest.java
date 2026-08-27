package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowsComponentFactoryTemplateTest extends AbstractGeneratorTestFixtures {
    private static final String TEST_COMPONENT_FACTORY = "ComponentFactory";

    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBrokerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_brokerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBroker(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBrokerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS (Generic) ----------------------------------
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_genericConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getGenericConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedGenericConsumerComponent.java"), templateString);
    }

    /**
     * requiresStub=false: the factory field must be declared using the user-supplied fully-qualified class name
     * verbatim, not Studio's managed user-package - see also
     * resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedGenericConsumerNoStubComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_genericConsumerNoStubComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getGenericConsumer(metaPackVersion);
        flowElement.setPropertyValue(ComponentPropertyMeta.REQUIRES_STUB, false);
        flowElement.setPropertyValue(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME, "com.acme.reusable.MyExistingConsumer");
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedGenericConsumerNoStubComponent.java"), templateString);
    }

    //  ------------------------------- PRODUCER (Generic) ----------------------------------
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_genericProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getGenericProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedGenericProducerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_eventGeneratingConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);
    }



    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_sftpConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLocalFileConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_localFileConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLocalFileConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLocalFileConsumerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedScheduledConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_scheduledConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getScheduledConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedScheduledConsumerComponent.java"), templateString);
    }

   /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_springJmsConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSpringJmsConsumerComponent.java"), templateString);
    }

   /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBasicAmqSpringJmsConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_BasicAmqSpringJmsConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBasicAmqSpringJmsConsumerComponent.java"), templateString);
    }


    // ------------------------------------- FILTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/ComponentFactoryFullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMessageFilter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedMessageFilterComponent.java"), templateString);
    }

    /**
     * Regression test: org.ikasan.filter.DefaultMessageFilter has no no-arg constructor - it requires a
     * FilterRule instance be passed in. Studio generates a FilterRule stub (see FlowsUserImplementedComponentTemplateTest's
     * DefaultMessageFilter.java) and this factory must wrap it: "new DefaultMessageFilter(stub)", then return
     * that wrapping instance (not the raw stub, which doesn't itself satisfy the Filter interface this method
     * declares as its return type).
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/ComponentFactoryFullyPopulatedDefaultMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_defaultMessageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultMessageFilter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDefaultMessageFilterComponent.java"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToObjectConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToObjectConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToObjectConverterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToXmlStringConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToXmlStringConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java"), templateString);
    }

    // ------------------------------------- TRANSLATORS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomTranslatorComponent.java
     * @throws IOException if the template can't be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customTranslatorComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomTranslator(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomTranslatorComponent.java"), templateString);
    }

    // ------------------------------------- DEBUG -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_debugTransitionComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDebugTransition(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDebugTransitionComponent.java"), templateString);
    }



    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_devNullProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDevNullProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEmailProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_emailProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEmailProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_jmsProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getJmsProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedJmsProducerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBasicAmqJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_basicAmqJmsProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBasicAmqJmsProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_sftpProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_loggingProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLoggingProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }

    // ------------------------------------- SPLITTERS -------------------------------------

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomSplitterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customSplitterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomSplitter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedCustomSplitterComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDefaultListSplitterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_defaultListSplitterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultListSplitter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDefaultListSplitterComponent.java"), templateString);
    }
}