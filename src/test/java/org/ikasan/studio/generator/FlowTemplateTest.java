package org.ikasan.studio.generator;

import org.ikasan.studio.TestFixtures;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowTemplateTest {
    Module module;
    private static final String TEST_FLOW_NAME = "MyFlow1";

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
//        ikasanFlow = new Flow();
//        ikasanFlow.setComponentName(TEST_FLOW_NAME);
//        ikasanFlow.setDescription("MyFlowDescription");
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1OneFlow.java
     * @throws IOException if the template cant be generated
     */
    @Disabled
    @Test
    public void testCreateFlowWith_oneFlow() {
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "OneFlow.java")));
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEventGeneratingConsumer.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getEventGeneratingConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedEventGeneratingConsumerComponent.java"), templateString);

    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
            .flowElements(Collections.singletonList(TestFixtures.getDevNullProducer()))
            .build();
        module.addFlow(flow);

        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java"), templateString);
    }



//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ftpConsumer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedFtpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedFtpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedFtpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ftpProducer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedFtpProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedFtpProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_sftpConsumer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedSftpConsumerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedSftpProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_sftpProducer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSftpProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedSftpProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedScheduledConsumerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_ScheduledConsumer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedSftpConsumerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedScheduledConsumerComponent.java")));
//    }
//
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedDevNullProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_devNullProducer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedDevNullProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedDevNullProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedEmailProducerComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_emailProducer() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedEmailProducerComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedEmailProducerComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedObjectMessageToXmlStringConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_objectMessageToXmlStringConverterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedObjectMessageToXmlStringConverterComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedObjectMessageToXmlStringConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1FullyPopulatedXmlStringObjectMessageConverterComponent.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_XmlStringObjectMessageConverterComponent() throws IOException {
//        ikasanFlow.getFlowElements().add(TestFixtures.getFullyPopulatedXmlStringObjectMessageConverterComponent(ikasanFlow));
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "FullyPopulatedXmlStringObjectMessageConverterComponent.java")));
//    }
//
//    /**
//     * See also resources/studio/templates/org/ikasan/studio/generator/MyFlow1WithExceptionResolver.java
//     * @throws IOException if the template cant be generated
//     */
//    //@Test - suspend while this is being redeveloped
//    public void testCreateFlowWith_exceptionResolver() throws IOException {
//        TestFixtures.populateFlowExceptionResolver(ikasanFlow);
//
//        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanModule, ikasanFlow);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(TEST_FLOW_NAME + "WithExceptionResolver.java")));
//    }
}
