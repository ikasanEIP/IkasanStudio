package org.ikasan.studio.io;

import org.ikasan.studio.StudioException;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentIOTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";

    @BeforeAll
    public static void classSetup() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
    }
    @Test
    public void testFlowElementToJson() throws IOException {
        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    @Test
    public void testFlowToJson() throws IOException {
        Flow flow1 = Flow.flowBuilder().name("Flow1").build();
        String jsonString = ComponentIO.toJson(flow1);

        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flow.json"), jsonString);
    }

    @Test
    public void testModuleToJson() throws IOException {
        Module module = new Module();
        module.setVersion("1.3");
        module.setName("The Module Name");
        module.setDescription("The Description");
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/module.json"), ComponentIO.toJson(module));
    }


    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testPopulatedFlowToJson() throws IOException {
        String jsonString = ComponentIO.toJson(getFixtureEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow.json"), jsonString);
    }

    @Test
    public void testPopulatedModuleToJson() throws IOException {
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
    public void testFlowElementDeserialise() throws IOException {

        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testModuleInstanceDeserialise() throws StudioException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module.json");
        List<Flow> flows = module.getFlows();
        Flow flow1 = flows.get(0);
        FlowElement eventGeneratingConsumer = flow1.getConsumer();
        List<Transition> transition = flow1.getTransitions();
        FlowElement customConverter = flow1.getFlowElements().get(0);
        FlowElement devNullProducer = flow1.getFlowElements().get(1);

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals("1.3", module.getVersion()),
            () -> assertEquals("A to B convert", module.getName()),
            () -> assertEquals("My first module", module.getDescription()),
            () -> assertEquals("co.uk.test", module.getApplicationPackageName()),
            () -> assertEquals("1", module.getH2PortNumber()),
            () -> assertEquals("2", module.getH2WebPortNumber()),
            () -> assertEquals("3", module.getPort()),

            () -> assertEquals(1, flows.size()),
            () -> assertEquals(1, flow1.getConfiguredProperties().size()),
            () -> assertEquals("Flow1", flow1.getName()),

            () -> assertEquals(2, eventGeneratingConsumer.getConfiguredProperties().size()),

            () -> assertEquals("My Event Generating Consumer", eventGeneratingConsumer.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("The Event Generating Consumer Description", eventGeneratingConsumer.getDescription()),

            () -> assertEquals(1, transition.size()),
            () -> assertEquals("My Transition", transition.get(0).getName()),
            () -> assertEquals("My Custom Converter", transition.get(0).getFrom()),
            () -> assertEquals("My DevNull Producer", transition.get(0).getTo()),

            () -> assertEquals(5, customConverter.getConfiguredProperties().size()),
            () -> assertEquals("Custom Converter", customConverter.getIkasanComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter", customConverter.getIkasanComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter.Custom", customConverter.getIkasanComponentMeta().getImplementingClass()),
            () -> assertEquals("myConverter", customConverter.getConfiguredProperties().get(BESKPOKE_CLASS_NAME).getValue()),
            () -> assertEquals("My Custom Converter", customConverter.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("The Custom Converter Description", customConverter.getDescription()),
            () -> assertEquals("java.lang.String", customConverter.getConfiguredProperties().get(FROM_CLASS).getValue()),
            () -> assertEquals("java.lang.Integer", customConverter.getConfiguredProperties().get(TO_CLASS).getValue()),

            () -> assertEquals(2, devNullProducer.getConfiguredProperties().size()),
            () -> assertEquals("Dev Null Producer", devNullProducer.getIkasanComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.endpoint.Producer", devNullProducer.getIkasanComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl", devNullProducer.getIkasanComponentMeta().getImplementingClass()),
            () -> assertEquals("My DevNull Producer", devNullProducer.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("The DevNull Description", devNullProducer.getDescription())
        );

    }


    // Reusable Fixtures
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
                .flowElements(Arrays.asList(customConverter,devNullProducer))
                .transitions(Collections.singletonList(transition))
                .build();
    }

}