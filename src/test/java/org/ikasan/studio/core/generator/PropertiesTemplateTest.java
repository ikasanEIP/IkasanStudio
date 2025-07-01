package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PropertiesTemplateTest extends AbstractGeneratorTestFixtures {

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_non_default_port(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_use_embeddedH2(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        module.setPropertyValue("useEmbeddedH2", true);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow_useEmbeddedH2.properties"), templateString);
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_use_embeddedH2_null(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        module.setPropertyValue("useEmbeddedH2", null);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_use_flowAutoStartup(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        module.setPropertyValue("flowAutoStartup", true);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow_flowAutoStartup.properties"), templateString);
    }


    /**
     * See also application_emptyFlow.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_use_flowAutoStartup_null(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        module.setPropertyValue("flowAutoStartup", null);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedEventGeneratingConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_eventGeneratingConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEventGeneratingConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEventGeneratingConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_localFileConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLocalFileConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }
    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_localFileConsumerMandatoryOnly(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLocalFileConsumerMandatoryOnly(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedScheduledConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_scheduledConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getScheduledConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedScheduledConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_sftpConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpConsumerComponent.properties"), templateString);
    }


    /**
     * See also application_fullyPopulatedSpringJmsConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_springJmsConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSpringJmsConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedBasicAmqSpringJmsConsumerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_basicAmqSpringJmsConsumer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqSpringJmsConsumer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedBasicAmqSpringJmsConsumerComponent.properties"), templateString);
    }


    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also application_fullyPopulatedMessageFilterComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageFilter(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMessageFilter(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedMessageFilterComponent.properties"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also application_fullyPopulatedCustomConverterComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customConverter(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(4, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        }
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedCustomConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(5, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
        }
    }

    /**
     * See also application_fullyPopulatedObjectMessageToObjectConverterComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToObjectConverter(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToObjectConverter(metaPackVersion);
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(4, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        }
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedObjectMessageToObjectConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(5, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
        }
    }

    /**
     * See also application_fullyPopulatedObjectMessageToXmlStringConverterComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_objectMessageToXmlStringConverter(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getObjectMessageToXmlStringtConverter(metaPackVersion);

        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(4, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        }
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedObjectMessageToXmlStringConverterComponent.properties"), templateString);
        // The Converter requires a new jar dependency
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(5, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
        }
    }

    // ------------------------------------- TRANSLATORS -------------------------------------
    /**
     * See also application_fullyPopulatedCustomConverterComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customTranslator(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomTranslator(metaPackVersion);

        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(4, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(9, module.getAllUniqueSortedJarDependencies().size());
        }

        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedCustomTranslatorComponent.properties"), templateString);
        // The Translator requires a new jar dependency
        switch (metaPackVersion) {
            case TestFixtures.META_IKASAN_PACK_3_3_7:
                assertEquals(5, module.getAllUniqueSortedJarDependencies().size());
                break;
            default:
                assertEquals(10, module.getAllUniqueSortedJarDependencies().size());
        }
    }

    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also application_fullyPopulatedDevNullProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_devNullProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDevNullProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedDevNullProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedEmailProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_emailProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEmailProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_ftpProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedJmsProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_jmsProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getJmsProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedJmsProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedBasicAmqJmsProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_basicAmqJmsProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBasicAmqJmsProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedBasicAmqJmsProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_SftpProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLoggingProducerComponent.properties
     * @throws IOException, StudioGeneratorException, StudioBuildException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_loggingProducer(String metaPackVersion) throws IOException, StudioGeneratorException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getLoggingProducer(metaPackVersion);
        String templateString = generatePropertiesTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLoggingProducerComponent.properties"), templateString);
    }
}