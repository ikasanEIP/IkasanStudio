package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.*;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("packs")
public class FlowsUserImplementedComponentTemplateTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void inputNamesDistinguishPayloadsFromFullEvents(String pack) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        for (var component : java.util.List.of(TestFixtures.getCustomConverter(pack),
                TestFixtures.getEmailConverter(pack), TestFixtures.getBroker(pack),
                TestFixtures.getCustomSplitter(pack))) {
            for (String inputType : java.util.List.of("java.lang.String",
                    "org.ikasan.spec.flow.FlowEvent",
                    "org.ikasan.spec.flow.FlowEvent<java.lang.String, java.lang.String>")) {
                component.setPropertyValue("fromType", inputType);
                String generated = generateUserImplementedComponentTemplate(pack, module, component);
                boolean fullEvent = inputType.startsWith("org.ikasan.spec.flow.FlowEvent");
                String inputName = fullEvent ? "event" : "payload";
                assertTrue(generated.contains("(" + inputType + " " + inputName + ")"), generated);
                assertFalse(generated.contains("payload.getPayload()"), generated);
                if (fullEvent) {
                    assertFalse(generated.contains("payload =="), generated);
                    assertFalse(generated.contains("payload.toString()"), generated);
                    assertFalse(generated.contains("valueOf(payload)"), generated);
                    assertFalse(generated.contains("@param payload"), generated);
                }
            }
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void customComponentsIncludeLoggingExamples(String version) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, new ArrayList<>());
        for (var component : java.util.List.of(TestFixtures.getGenericConsumer(version),
                TestFixtures.getGenericProducer(version), TestFixtures.getBroker(version),
                TestFixtures.getCustomConverter(version), TestFixtures.getEmailConverter(version))) {
            String generated = generateUserImplementedComponentTemplate(version, module, component);
            assertTrue(generated.contains("private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("));
            boolean producer = component.getComponentMeta().isProducer();
            assertTrue(generated.contains(producer ? "\nLOG.debug(" : "// LOG.debug("));
            assertFalse(generated.contains("System.out.println"));
            assertEquals(producer, generated.lines().anyMatch(line -> line.stripLeading().startsWith("LOG.")));
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void brokerGenericReturnTypeUpdatesBothSignaturesWithoutInvalidValueOf(String pack) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var broker = TestFixtures.getBroker(pack);
        broker.setPropertyValue("fromType", "org.quartz.JobExecutionContext");
        broker.setPropertyValue("toType", "java.util.List<java.lang.String>");
        String generated = generateUserImplementedComponentTemplate(pack, module, broker);
        assertTrue(generated.contains("implements Broker<org.quartz.JobExecutionContext, java.util.List<java.lang.String>>"));
        assertTrue(generated.contains("public java.util.List<java.lang.String> invoke(org.quartz.JobExecutionContext payload)"));
        assertFalse(generated.contains(".valueOf(payload)"));
        assertTrue(generated.contains("throw new EndpointException("));
    }

    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyBroker.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_brokerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBroker(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyBroker.java"), templateString);
    }

    //  ------------------------------- CONSUMER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Consumer/MyGenericConsumer.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_genericConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getGenericConsumer(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyGenericConsumer.java"), templateString);
    }

    //  ------------------------------- PRODUCER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Producer/MyGenericProducer.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_genericProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getGenericProducer(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyGenericProducer.java"), templateString);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    public void testFileTransferToEmailAttachmentRecipe(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement converter = TestFixtures.getCustomConverter(metaPackVersion);
        converter.setPropertyValue("fromType", "org.ikasan.filetransfer.Payload");
        converter.setPropertyValue("toType", "org.ikasan.component.endpoint.email.producer.EmailPayload");
        var matches = converter.getComponentMeta().getConversionRecipes().stream()
                .filter(recipe -> recipe.matches(converter.getPropertyValueAsString("fromType"),
                        converter.getPropertyValueAsString("toType"))).toList();
        org.junit.jupiter.api.Assertions.assertEquals(2, matches.size());
        converter.setPropertyValue("conversionRecipeId", matches.stream().filter(r -> r.getId().equals("file-transfer-payload-to-email-attachment")).findFirst().orElseThrow().getId());
        String generated = generateUserImplementedComponentTemplate(metaPackVersion, module, converter);
        assertTrue(generated.contains("implements Converter<org.ikasan.filetransfer.Payload, org.ikasan.component.endpoint.email.producer.EmailPayload>"));
        // Attachment is generated commented out - the Email Producer's own "hasAttachments" property, not this
        // Converter, gates whether an attachment is actually sent (see construct-email-attachment.ftl) - and the
        // default body only names the file rather than claiming one is attached.
        assertTrue(generated.contains("// result.addAttachment(filename, \"application/octet-stream\", attachmentContent)"));
        assertFalse(generated.contains("\n            result.addAttachment("));
        assertTrue(generated.contains("hasAttachments"));
        assertTrue(generated.contains("result.setEmailBody(\"Source file: \" + filename)"));
        assertFalse(generated.toLowerCase().contains("please see the attached file"));
        assertTrue(generated.contains("payload.getAttribute(\"fileName\")"));
        assertTrue(generated.contains("throw new TransformationException"));
        org.junit.jupiter.api.Assertions.assertFalse(generated.contains("payload.toString()"));
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_customConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyConverter.java"), templateString);
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testJmsToFileTransferRecipeSelectsItsMetapackTemplate(String metaPackVersion) throws Exception {
        // The conversion recipe is registered per metapack against that version's own JMS namespace
        // (javax.jms for V3.3.9, jakarta.jms for V4.1.6) - see Converter/component-meta_en_GB.json's
        // conversionRecipes[].sourceType - so the FROM_TYPE used to select the recipe must match.
        String jmsMessageType = PackExpectations.enterpriseNamespace(metaPackVersion) + ".jms.Message";
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE, jmsMessageType);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE, "org.ikasan.filetransfer.Payload");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.CONVERSION_RECIPE_ID,
                "jms-message-to-file-transfer-payload");

        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);

        assertTrue(templateString.contains("implements Converter<" + jmsMessageType + ", org.ikasan.filetransfer.Payload>"));
        assertTrue(templateString.contains("new org.ikasan.filetransfer.component.DefaultPayload"));
        assertTrue(templateString.contains("FilePayloadAttributeNames.FILE_NAME"));
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testAutoConvertedJmsContentRecipeSelectsPayloadWrapperTemplate(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE, "java.lang.Object (auto-converted)");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE, "org.ikasan.filetransfer.Payload");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.CONVERSION_RECIPE_ID,
                "auto-converted-jms-content-to-file-transfer-payload");

        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);

        assertTrue(templateString.contains("implements Converter<java.lang.Object, org.ikasan.filetransfer.Payload>"));
        // V3.3.9 targets JDK 11 (no pattern-matching instanceof) so it renders "instanceof byte[])" with a
        // separate cast, while V4.1.6 (JDK 17) renders "instanceof byte[] bytes" - assert on the shared prefix
        // rather than a version-specific form.
        assertTrue(templateString.contains("body instanceof byte["));
        assertTrue(templateString.contains("new org.ikasan.filetransfer.component.DefaultPayload"));
    }


    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testJmsToEmailRecipeSelectsItsMetapackTemplate(String metaPackVersion) throws Exception {
        // See testJmsToFileTransferRecipeSelectsItsMetapackTemplate's comment: the recipe's registered
        // sourceType is namespace-specific per metapack.
        String jmsMessageType = PackExpectations.enterpriseNamespace(metaPackVersion) + ".jms.Message";
        String jmsPackage = PackExpectations.enterpriseNamespace(metaPackVersion) + ".jms";
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE, jmsMessageType);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE,
                "org.ikasan.component.endpoint.email.producer.EmailPayload");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.CONVERSION_RECIPE_ID,
                "jms-message-to-email-payload");

        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);

        assertTrue(templateString.contains("implements Converter<" + jmsMessageType + ", org.ikasan.component.endpoint.email.producer.EmailPayload>"));
        assertTrue(templateString.contains("result.setEmailBody(text(body, charset))"));
        assertTrue(templateString.contains("payload instanceof " + jmsPackage + ".TextMessage"));
        assertTrue(templateString.contains("payload instanceof " + jmsPackage + ".BytesMessage"));
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testAutoConvertedJmsContentToEmailRecipeUsesEmailTemplate(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE,
                "java.lang.Object (auto-converted)");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE,
                "org.ikasan.component.endpoint.email.producer.EmailPayload");
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.CONVERSION_RECIPE_ID,
                "auto-converted-jms-content-to-email-payload");

        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);

        assertTrue(templateString.contains("implements Converter<java.lang.Object, org.ikasan.component.endpoint.email.producer.EmailPayload>"));
        assertTrue(templateString.contains("result.setEmailBody(text(body, charset))"));
        assertFalse(templateString.contains("payload.toString()"));
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyEmailConverter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailConverter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyEmailConverter.java"), templateString);
    }

    /**
     * Regression test: fromType is free text, and a user can copy an upstream component's Output: display text
     * (e.g. "java.lang.Object (auto-converted)" for a JMS consumer with Auto Content Conversion on) verbatim into
     * it, which used to generate an uncompilable type literal - see StudioBuildUtils#toJavaTypeLiteral.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailConverterComponent_stripsAnAutoConvertedAnnotationFromFromType(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailConverter(metaPackVersion);
        flowElement.setPropertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE, "java.lang.Object (auto-converted)");
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertTrue(templateString.contains("Converter<java.lang.Object, EmailPayload>"));
        assertTrue(templateString.contains("convert(java.lang.Object payload)"));
        assertFalse(templateString.contains("auto-converted"));
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyTranslator.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_customTranslatorComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomTranslator(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyTranslator.java"), templateString);
    }

    //  ------------------------------- DEBUG ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_debugTransitionComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDebugTransition(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyDebugTransition.java"), templateString);
    }
    //  ------------------------------- FILTERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_messageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMessageFilter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"MessageFilter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_defaultMessageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultMessageFilter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"DefaultMessageFilter.java"), templateString);
    }


    //  ------------------------------- ROUTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_messageMultiRecipientRouterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMultiRecipientRouter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"MultiRecipientRouter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_messageSingleRecipientRouterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSingleRecipientRouter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"SingleRecipientRouter.java"), templateString);
    }

    //  ------------------------------- SPLITTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Splitter/MySplitter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_customSplitterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomSplitter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyCustomSplitter.java"), templateString);
    }
}