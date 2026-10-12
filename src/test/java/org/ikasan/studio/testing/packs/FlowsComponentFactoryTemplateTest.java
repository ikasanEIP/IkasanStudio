package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.*;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("packs")
public class FlowsComponentFactoryTemplateTest extends AbstractGeneratorTestFixtures {
    private static final String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void emailProducersInSeparateBranchesHaveIndependentRecipientFields(String pack) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var flow = TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        for (String name : java.util.List.of("Output 1", "Output 3")) {
            FlowElement producer = TestFixtures.getEmailProducer(pack);
            producer.setComponentName(name);
            producer.setPropertyValue("toRecipients", name.equals("Output 1") ? "[first@example.com]" : "[third@example.com]");
            var route = org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                    .flow(flow).routeName(name).flowElements(java.util.List.of(producer)).build();
            producer.setContainingFlowRoute(route);
            flow.getFlowRoute().getChildRoutes().add(route);
        }
        String factory = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        String properties = PropertiesTemplate.create(module);
        for (String recipient : java.util.List.of("ToRecipients", "CcRecipients", "BccRecipients")) {
            for (String name : java.util.List.of("output1", "output3")) {
                String property = "myflow1." + name + ".email.producer." + Character.toLowerCase(recipient.charAt(0)) + recipient.substring(1);
                assertTrue(factory.contains(".set" + recipient + "(studioStringList(\"" + property + "\"))"), factory);
            }
        }
        assertTrue(properties.contains("myflow1.output1.email.producer.toRecipients=[\"first@example.com\"]"), properties);
        assertTrue(properties.contains("myflow1.output3.email.producer.toRecipients=[\"third@example.com\"]"), properties);
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void emailEnvironmentSettingsAreExternalizedPerComponent(String pack) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var flow = TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        for (String name : java.util.List.of("Output 1", "Output 2")) {
            var producer = FlowElement.flowElementBuilder()
                    .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer"))
                    .componentName(name).build();
            producer.setPropertyValue("toRecipient", name.equals("Output 1") ? "first@example.com" : "second@example.com");
            producer.setPropertyValue("from", "sender@example.com");
            producer.setPropertyValue("mailSmtpHost", "smtp.example.com");
            producer.setPropertyValue("mailSmtpPort", 2525);
            producer.setPropertyValue("mailSubject", "Orders: " + name);
            producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().getFlowElements().add(producer);
        }
        String factory = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        var properties = new java.util.Properties();
        properties.load(new java.io.StringReader(PropertiesTemplate.create(module)));
        for (int index = 1; index <= 2; index++) {
            String prefix = "myflow1.output" + index + ".email.producer.";
            String field = "myFlow1Output" + index + "EmailProducer";
            assertEquals(index == 1 ? "first@example.com" : "second@example.com", properties.getProperty(prefix + "toRecipient"));
            assertEquals("2525", properties.getProperty(prefix + "mailSmtpPort"));
            assertEquals("Orders: Output " + index, properties.getProperty(prefix + "mailSubject"));
            for (String property : java.util.List.of("toRecipient", "from", "mailSmtpHost", "mailSmtpPort", "mailSubject")) {
                assertTrue(factory.contains("@org.springframework.beans.factory.annotation.Value(\"${" + prefix + property + "}\")"), factory);
                String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
                assertTrue(factory.contains(".set" + suffix + "(" + field + suffix + ")"), factory);
            }
            assertFalse(properties.containsKey(prefix + "ccRecipient"), "Unset optional recipients must remain absent");
        }
        assertFalse(factory.contains("first@example.com"));
        assertFalse(factory.contains("smtp.example.com"));
    }

    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedBrokerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_brokerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBroker(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedBrokerComponent.java"), templateString);
    }

    //  ------------------------------- CONSUMERS (Generic) ----------------------------------
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_springJmsConsumerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSpringJmsConsumer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSpringJmsConsumerComponent.java"), templateString);
    }


    // ------------------------------------- FILTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/ComponentFactoryFullyPopulatedMessageFilterComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getEmailProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedEmailProducerComponent.java"), templateString);
    }

    /**
     * Regression test: an Email Producer with no toRecipient(s) entered must not emit a call to the
     * List&lt;String&gt;-typed setToRecipients(), which requires a non-empty argument to compile - see
     * {@link org.ikasan.studio.ui.component.properties.ComponentPropertyEditRowStringListTest}.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailProducerComponentWithNoRecipients(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Email Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Email Producer")
            .build();
        flowElement.setPropertyValue("from", "FromAddress");
        flowElement.defaultUnsetMandatoryProperties();
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertFalse(templateString.contains(".setToRecipients("), "no toRecipients were entered, so .setToRecipients( must not be generated:\n" + templateString);
        assertFalse(templateString.contains(".setToRecipient("), "no toRecipient was entered, so .setToRecipient( must not be generated:\n" + templateString);
    }

    /**
     * Regression test: EmailProducerConfiguration has no Java-level default for emailFormat (unlike e.g.
     * mailhost's "localhost") - a component left with no .setEmailFormat(...) call at all sends a null MIME
     * type straight through to javax.mail's MimeBodyPart, which throws "ParseException: Expected MIME type,
     * got null" at send time. emailFormat is now mandatory with a real defaultValue ("text/plain"), so
     * defaultUnsetMandatoryProperties() (called for every fresh component, e.g. on model.json load - see
     * ModuleDeserializer#getInitialFlowElement) fills it in automatically rather than leaving it unset.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailProducerComponentDefaultsEmailFormat(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Email Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Email Producer")
            .build();
        flowElement.setPropertyValue("from", "FromAddress");
        flowElement.setPropertyValue("toRecipient", "valid@example.com");
        flowElement.defaultUnsetMandatoryProperties();
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertTrue(PropertiesTemplate.create(module).contains("myflow1.myemailproducer.email.producer.emailFormat=text/plain"));
        assertTrue(templateString.contains(".setEmailFormat(myFlow1MyEmailProducerEmailProducerEmailFormat)"), "an unset emailFormat must default to a real MIME type, not be left out entirely:\n" + templateString);
    }

    /**
     * Regression test: a now-fixed older bug could persist the literal string "[]" (java.util.List#toString()
     * of an empty list) as a List property's value in model.json, rather than leaving it genuinely unset.
     * Reloading such a file must not resurrect an uncompilable/broken ".setToRecipients(...)" call - see
     * {@link org.ikasan.studio.core.model.ikasan.instance.ComponentPropertyTest}.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_emailProducerComponentWithStaleEmptyListLiteral(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Email Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Email Producer")
            .build();
        flowElement.setPropertyValue("from", "FromAddress");
        flowElement.setPropertyValue("toRecipient", "valid@example.com");
        // Simulates a stale model.json produced by the older bug, not a fresh UI edit.
        flowElement.setPropertyValue("toRecipients", "[]");
        flowElement.setPropertyValue("ccRecipients", "[]");
        flowElement.setPropertyValue("bccRecipients", "[]");
        flowElement.defaultUnsetMandatoryProperties();
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertFalse(templateString.contains(".setToRecipients("), "a stale \"[]\" toRecipients must not be generated:\n" + templateString);
        assertFalse(templateString.contains(".setCcRecipients("), "a stale \"[]\" ccRecipients must not be generated:\n" + templateString);
        assertFalse(templateString.contains(".setBccRecipients("), "a stale \"[]\" bccRecipients must not be generated:\n" + templateString);
        assertTrue(templateString.contains(".setToRecipient(myFlow1MyEmailProducerEmailProducerToRecipient)"), "the valid singular toRecipient must still be generated:\n" + templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedJmsProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_jmsProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getJmsProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedJmsProducerComponent.java"), templateString);
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_ftpProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java"), templateString);
    }

    /** Externalized passwords and paths must survive Java properties escaping unchanged. */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void stringPropertiesWithBackslashesAndQuotesRoundTripThroughProperties(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getFtpProducer(metaPackVersion);
        String password = "pa\"ss\\word";
        String path = "C:\\keys\\store.jks";
        flowElement.setPropertyValue("ftpsKeyStoreFilePassword", password);
        flowElement.setPropertyValue("ftpsKeyStoreFilePath", path);
        String factory = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        var properties = new java.util.Properties();
        properties.load(new java.io.StringReader(PropertiesTemplate.create(module)));
        assertEquals(password, properties.getProperty("myflow1.myftpproducer.ftp.producer.ftpsKeyStoreFilePassword"));
        assertEquals(path, properties.getProperty("myflow1.myftpproducer.ftp.producer.ftpsKeyStoreFilePath"));
        assertFalse(factory.contains(password));
        assertTrue(factory.contains(".setFtpsKeyStoreFilePassword(myFlow1MyFTPProducerFtpProducerFtpsKeyStoreFilePassword)"), factory);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_sftpProducerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpProducer(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java"), templateString);
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testSftpProducerCanGenerateOverwriteSetting(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSftpProducer(metaPackVersion);
        flowElement.setPropertyValue("overwrite", true);

        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);

        assertTrue(templateString.contains(".setOverwrite(myFlow1MySFTPProducerSftpProducerOverwrite)"), templateString);
        assertTrue(PropertiesTemplate.create(module).contains("myflow1.mysftpproducer.sftp.producer.overwrite=true"));
    }


    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
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
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateFlowWith_defaultListSplitterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultListSplitter(metaPackVersion);
        String templateString = generateFlowsComponentFactoryTemplateString(metaPackVersion, module, flowElement);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, TEST_COMPONENT_FACTORY + "FullyPopulatedDefaultListSplitterComponent.java"), templateString);
    }
}