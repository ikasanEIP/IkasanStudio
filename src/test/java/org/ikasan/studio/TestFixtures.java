package org.ikasan.studio;

import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.*;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta.*;

/**
 * Some of these text fixtures will be exported to the Meta Pack project
 * Ideally the Ikasan Packs should be loosely coupled with the IDE so that most
 * of the knoweldge and detail of what each pack support comes from the pack, not the IDE.
 */
public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";
    public static final String TEST_IKASAN_PACK = "Vtest.x";
    public static final String V3_3_IKASAN_PACK = "V3.3.x";
    public static final String TEST_FLOW_NAME = "MyFlow1";
    public static final String TEST_FLOW_DESCRIPTION = "MyFlowDescription";

    public static Module getMyFirstModuleIkasanModule(List<Flow> flows) {
        return Module.moduleBuilder()
                .version("1.3")
                .name("A to B convert")
                .description("My first module")
                .applicationPackageName("co.uk.test")
                .port("8091")
                .h2PortNumber("8092")
                .h2WebPortNumber("8093")
                .flows(flows)
                .build();
    }

    public static Flow.FlowBuilder getUnbuiltFlow() {
        return Flow.flowBuilder()
                .description(TEST_FLOW_DESCRIPTION)
                .name(TEST_FLOW_NAME);
    }

    public static FlowElement getDevNullProducer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "Dev Null Producer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My DevNull Producer")
                .description("The DevNull Description")
                .build();
    }

    public static FlowElement getLoggingProducer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Logging Producer");

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Logging Producer")
                .description("The Logging Description")
                .build();
        flowElement.setPropertyValue("configuredResourceId", "MyResourceID");
        flowElement.setPropertyValue("regExpPattern", "this");
        flowElement.setPropertyValue("replacementText", "that");
        flowElement.setPropertyValue("logEveryNth", 2);
        return flowElement;
    }

    public static FlowElement getCustomConverter() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "Custom Converter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Custom Converter")
                .description("The Custom Converter Description")
                .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(BESPOKE_CLASS_NAME, "myConverter");
        return flowElement;
    }
    public static FlowElement getEventGeneratingConsumer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKey(TEST_IKASAN_PACK, "Event Generating Consumer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Event Generating Consumer")
                .description("The Event Generating Consumer Description")
                .build();
    }

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerFlow() {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer();
        FlowElement customConverter = getCustomConverter();
        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        Transition transition = Transition.builder()
                .from(customConverter.getComponentName())
                .to(devNullProducer.getComponentName())
                .name("My Transition")
                .build();
        return getUnbuiltFlow()
                .consumer(eventGeneratingConsumer)
                .flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer)))
                .transitions(Collections.singletonList(transition))
                .build();
    }

    /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     *
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getFullyPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);

//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "MyConfiguredResourceId");
        component.setPropertyValue("Description", "Test description");
        component.setPropertyValue("IsConfiguredResource", true);
        return component;
    }

    /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     *
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getMinimumPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);

//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        return component;
    }

}
