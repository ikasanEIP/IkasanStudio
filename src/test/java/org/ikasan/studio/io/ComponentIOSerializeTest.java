package org.ikasan.studio.io;

import org.ikasan.studio.generator.TestUtils;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Transition;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentIOSerializeTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";

    @BeforeAll
    public static void classSetup() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
    }

    @Test
    public void testFlowElementSerializeToJson() throws IOException {
        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    @Test
    public void testFlowSerializeToJson() throws IOException {
        Flow flow1 = Flow.flowBuilder().name("Flow1").build();
        String jsonString = ComponentIO.toJson(flow1);

        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flow.json"), jsonString);
    }

    @Test
    public void testModuleSerializeToJson() throws IOException {
        Module module = new Module();
        module.setVersion("1.3");
        module.setName("The Module Name");
        module.setDescription("The Description");
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/module.json"), ComponentIO.toJson(module));
    }


    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testPopulatedFlowSerializeToJson() throws IOException {
        String jsonString = ComponentIO.toJson(getFixtureEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow.json"), jsonString);
    }

    @Test
    public void testPopulatedModuleSerializeToJson() throws IOException {
        Flow flow1 = Flow.flowBuilder().name("Flow1").build();
        List<Flow> flows = new ArrayList<>();
        flows.add(flow1);

        Module module = Module.moduleBuilder()
                .version("1.3")
                .name("A to B convert")
                .description("My first module")
                .applicationPackageName("co.uk.test")
                .h2PortNumber("1")
                .h2WebPortNumber("2")
                .port("3")
                .flows(flows)
                .build();
        module.addFlow(getFixtureEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_module.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testDevNullFlowElementSerialiseToJson() throws IOException {

        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    // ---------- Fixtures

    public FlowElement getFixtureDevNullProducer() {
        IkasanComponentMeta devNullProducerMeta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "DEV_NULL_PRODUCER");
        return FlowElement.flowElementBuilder()
                .componentMeta(devNullProducerMeta)
                .componentName("My DevNull Producer")
                .description("The DevNull Description")
                .build();
    }
    public FlowElement getFixtureCustomConverter() {
        IkasanComponentMeta customConverterMeta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "CUSTOM_CONVERTER");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(customConverterMeta)
                .componentName("My Custom Converter")
                .description("The Custom Converter Description")
                .build();
        flowElement.setPropertyValue(FROM_CLASS, "java.lang.String");
        flowElement.setPropertyValue(TO_CLASS, "java.lang.Integer");
        flowElement.setPropertyValue(BESKPOKE_CLASS_NAME, "myConverter");
        return flowElement;
    }
    public FlowElement getFixtureEventGeneratingConsumer() {
        IkasanComponentMeta eventGeneratingConsumerMeta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "EVENT_GENERATING_CONSUMER");
        return FlowElement.flowElementBuilder()
                .componentMeta(eventGeneratingConsumerMeta)
                .componentName("My Event Generating Consumer")
                .description("The Event Generating Consumer Description")
                .build();
    }

    public Flow getFixtureEventGeneratingConsumerCustomConverterDevNullProducerFlow() {
        FlowElement eventGeneratingConsumer = getFixtureEventGeneratingConsumer();
        FlowElement customConverter = getFixtureCustomConverter();
        FlowElement devNullProducer = getFixtureDevNullProducer();
        Transition transition = Transition.builder()
                .from(customConverter.getComponentName())
                .to(devNullProducer.getComponentName())
                .name("My Transition")
                .build();
        return Flow.flowBuilder()
                .name("Flow1")
                .consumer(eventGeneratingConsumer)
                .flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer)))
                .transitions(Collections.singletonList(transition))
                .build();
    }
}