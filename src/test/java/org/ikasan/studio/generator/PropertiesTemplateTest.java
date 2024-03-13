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


public class PropertiesTemplateTest {
    Module module;
    Flow ikasanFlow;

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
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
    
    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedEventGeneratingConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_eventGeneratingConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getEventGeneratingConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedEventGeneratingConsumerComponent.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedFtpConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getFtpConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpConsumerComponent.properties"), templateString);
    }

    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also application_fullyPopulatedSftpConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_sftpConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getSftpConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpConsumerComponent.properties"), templateString);
    }


    /**
     * See also application_fullyPopulatedLocalFileConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_localFileConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getLocalFileConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLocalFileConsumerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedScheduledConsumerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_scheduledConsumer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getScheduledConsumer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedScheduledConsumerComponent.properties"), templateString);
    }

    // ------------------------------------- FILTER -------------------------------------
    /**
     * See also application_fullyPopulatedMessageFilterComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilter() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getMessageFilter()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedMessageFilterComponent.properties"), templateString);
    }

    // ------------------------------------- CONVERTERS -------------------------------------
    /**
     * See also application_fullyPopulatedCustomConverterComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverter() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getCustomConverter()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedCustomConverterComponent.properties"), templateString);
    }


    // ------------------------------------- PRODUCERS -------------------------------------
    /**
     * See also application_fullyPopulatedDevNullProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_devNullProducer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getDevNullProducer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedDevNullProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedFtpProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_ftpProducer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getFtpProducer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedSftpProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_SftpProducer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getSftpProducer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpProducerComponent.properties"), templateString);
    }

    /**
     * See also application_fullyPopulatedLoggingProducerComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_loggingProducer() throws IOException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .flowElements(Collections.singletonList(TestFixtures.getLoggingProducer()))
                .build();
        module.addFlow(flow);

        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedLoggingProducerComponent.properties"), templateString);
    }



}