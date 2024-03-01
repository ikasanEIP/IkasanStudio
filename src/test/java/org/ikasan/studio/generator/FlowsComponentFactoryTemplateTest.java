package org.ikasan.studio.generator;

import org.ikasan.studio.TestFixtures;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowsComponentFactoryTemplateTest {
    Module module;
//    Flow ikasanFlow = new Flow();
    private static final String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());

//        ikasanFlow = new Flow();
//        ikasanFlow.setComponentName(TEST_FLOW_NAME);
//        ikasanFlow.setDescription("MyFlowDescription");
    }


    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEventGeneratingConsumer.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumerComponent() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
            .flowElements(Collections.singletonList(TestFixtures.getEventGeneratingConsumer()))
            .build();
        module.addFlow(flow);

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedEventGeneratingConsumer.java"), templateString);
    }


    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedCustomConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverterComponent() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getCustomConverter()))
                .build();
        module.addFlow(flow);

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedCustomConverter.java"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducerComponent() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
            .flowElements(Collections.singletonList(TestFixtures.getDevNullProducer()))
            .build();
        module.addFlow(flow);

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLoggingProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducerComponent() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
            .flowElements(Collections.singletonList(TestFixtures.getLoggingProducer()))
            .build();
        module.addFlow(flow);

        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedLoggingProducerComponent.java"), templateString);
    }



//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedFtpConsumerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedFtpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFtpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedFtpProducerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedFtpProducerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        System.out.println(templateString);
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFtpProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedSftpConsumerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSftpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFilterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedFilterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedFilterComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFilterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryMinimumPopulatedFilterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_minimumPopulatedFilterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getMinimumPopulatedFilterComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "MinimumPopulatedFilterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedFilterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_filterComponentConfiguredResourceIDButNoIsConfiguredResource() throws IOException {
//
//        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(ikasanFlow);
//        filterFlowComponent.setPropertyValue("IsConfiguredResource", false);
//        ikasanFlow.getFlowElements().add(filterFlowComponent);
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedFilterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFilterComponentIsConfiguredResourceButNoConfiguredResourceId.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_filterComponentIsConfiguredResourceButNoConfiguredResourceID() throws IOException {
//
//        FlowElement filterFlowComponent = TestFixtures.getFullyPopulatedFilterComponent(ikasanFlow);
//        filterFlowComponent.setPropertyValue("ConfiguredResourceId", null);
//        ikasanFlow.getFlowElements().add(filterFlowComponent);
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FilterComponentIsConfiguredResourceButNoConfiguredResourceId.java")));
//    }
//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSftpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedSftpProducerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSftpProducerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSftpProducerComponent.java")));
//    }
//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedLocalFileConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedLocalFileConsumerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedLocalFileConsumerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedLocalFileConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedScheduledConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedScheduledConsumerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedScheduledConsumerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedScheduledConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedSpringJmsConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedJmsConsumerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSpringJmsConsumerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedSpringJmsConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedJmsProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedJmsProducerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedJmsProducerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedJmsProducerComponent.java")));
//    }
//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEmailProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedEmailProducerComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedEmailProducerComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedEmailProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToObjectConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedObjectMessageToObjectConverterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedObjectMessageToObjectConverterComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToObjectConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedObjectMessageToXmlStringConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedObjectMessageToXmlStringConverterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedObjectMessageToXmlStringConverterComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedXmlStringObjectMessageConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    @Test
//    public void testCreateFlowWith_fullyPopulatedXmlStringObjectMessageConverterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedXmlStringObjectMessageConverterComponent(ikasanFlow));
//
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_COMPONENT_FACTORY + "FullyPopulatedXmlStringObjectMessageConverterComponent.java")));
//    }
}