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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta.*;

class ComponentIOTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";

    @BeforeAll
    public static void classSetup() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
    }
    @Test
    public void testFlowElementToJson() throws IOException {
        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertThat(ComponentIO.toJson(devNullProducer), is(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json")));
    }

    @Test
    public void testFlowToJson() throws IOException {
        Flow flow1 = Flow.flowBuilder().name("Flow1").build();
        String jsonString = ComponentIO.toJson(flow1);

        assertThat(jsonString, is(TestUtils.getFileAsString("/org/ikasan/studio/flow.json")));
    }

    @Test
    public void testModuleToJson() throws IOException {
        Module module = new Module();
        module.setVersion("1.3");
        module.setName("The Module Name");
        module.setDescription("The Description");
        assertThat(ComponentIO.toJson(module), is(TestUtils.getFileAsString("/org/ikasan/studio/module.json")));
    }


    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testPopulatedFlowToJson() throws IOException {
        String jsonString = ComponentIO.toJson(getFixtureEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertThat(jsonString, is(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow.json")));
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
        assertThat(ComponentIO.toJson(module), is(TestUtils.getFileAsString("/org/ikasan/studio/populated_module.json")));
    }

    @Test
    public void testFlowElementDeserialise() throws IOException {

        FlowElement devNullProducer = getFixtureDevNullProducer();
        assertThat(ComponentIO.toJson(devNullProducer), is(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json")));
    }

    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testModuleInstanceDeserialise() throws StudioException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module.json");
        assertThat(module.getVersion(), is("1.3"));
        assertThat(module.getName(), is("A to B convert"));
        assertThat(module.getDescription(), is("My first module"));
        assertThat(module.getApplicationPackageName(), is("co.uk.test"));
        assertThat(module.getH2PortNumber(), is("1"));
        assertThat(module.getH2WebPortNumber(), is("2"));
        assertThat(module.getPort(), is("3"));
        List<Flow> flows = module.getFlows();
//        List<Flow> flowList = module.getFlows();
//        assertThat(flowList.size(),is(2));
//        assertThat(flowList.get(0).getName(), is("flow1"));
//        assertThat(flowList.get(1).getName(), is("flow2"));
    }


    // Reusable Fixtures
    public FlowElement getFixtureDevNullProducer() {
        IkasanComponentMeta devNullProducerMeta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "DEV_NULL_PRODUCER");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(devNullProducerMeta)
                .componentName("My DevNumm Producer")
                .description("The DevNumm Description")
                .build();
        return flowElement;
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
                .build();
        return Flow.flowBuilder()
                .name("Flow1")
                .consumer(eventGeneratingConsumer)
                .flowElements(Arrays.asList(customConverter,devNullProducer))
                .transitions(Collections.singletonList(transition))
                .build();
    }

}