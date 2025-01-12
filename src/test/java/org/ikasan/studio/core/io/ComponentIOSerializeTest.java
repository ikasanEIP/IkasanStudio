package org.ikasan.studio.core.io;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.TestUtils;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentIOSerializeTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";

    @BeforeAll
    public static void classSetup() throws StudioBuildException {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
    }

    @Test
    public void testFlowElementSerializeToJson() throws IOException, StudioBuildException {
        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    @Test
    public void testFlowSerializeToJson() throws IOException, StudioBuildException {
        Flow flow1 = TestFixtures.getUnbuiltFlow().build();
        String jsonString = ComponentIO.toJson(flow1);

        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flow.json"), jsonString);
    }

    @Test
    public void testModuleSerializeToJson() throws IOException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/module.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testBasicBooleanElementSerializeToJson() {
        String componentMetaKey = "booleanComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Boolean.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, true);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":true}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicIntegerElementSerializeToJson() {
        String componentMetaKey = "integerComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Integer.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, 99);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":99}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicLongElementSerializeToJson()  {
        String componentMetaKey = "longComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Long.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, 99L);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":99}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicShortElementSerializeToJson() {
        String componentMetaKey = "shortComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Short.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, 99);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":99}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicDoubleElementSerializeToJson() {
        String componentMetaKey = "doubleComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Double.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, 99.0);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":99.0}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicFloatElementSerializeToJson() {
        String componentMetaKey = "floatComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Double.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, 99.9);

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":99.9}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testBasicStringElementSerializeToJson() {
        String componentMetaKey = "stringComponentMeta";
        ComponentPropertyMeta componentMeta = ComponentPropertyMeta.builder()
                .propertyName(componentMetaKey)
                .propertyDataType(java.lang.Double.class)
                .defaultValue(false)
                .build();
        ComponentProperty componentProperty = new ComponentProperty(componentMeta, "bob");

        BasicElement basicElement = new BasicElement();
        basicElement.setComponentProperties(Collections.singletonMap(componentMetaKey, componentProperty));
        assertEquals("{\"" + componentMetaKey + "\":\"bob\"}", ComponentIO.toJson(basicElement));
    }

    @Test
    public void testPopulatedFlowSerializeToJson() throws IOException, StudioBuildException {
        String jsonString = ComponentIO.toJson(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow.json"), jsonString);
    }

    @Test
    public void testPopulatedFlowSerializeWithWiretapsToJson() throws IOException, StudioBuildException {
        String jsonString = ComponentIO.toJson(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow_with_wiretaps.json"), jsonString);
    }

    @Test
    public void testPopulatedFlowSerializeWithSomeFaultyWiretapsToJsonDoesNotCrashFlow() throws IOException, StudioBuildException {
        String jsonString = ComponentIO.toJson(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithFaultyWiretapsFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow_with_wiretaps.json"), jsonString);
    }
    @Test
    public void testPopulatedFlowWithMultiRecipientRouterSerializeToJson() throws IOException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(Collections.singletonList(TestFixtures.getEventGeneratingConsumerRouterFlow()));
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_module_with_router.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testPopulatedModuleSerializeToJson() throws IOException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(Collections.singletonList(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow()));
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_module.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testExceptionResolverModuleSerializeToJson() throws IOException, StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(
            Collections.singletonList(TestFixtures.getExceptionResolverFlow()));
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_module_with_exception_resolver.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testDevNullFlowElementSerialiseToJson() throws IOException, StudioBuildException {
        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    @Test
    public void testDebugIsNotSerialisedToJson() throws IOException, StudioBuildException {
        String jsonString = ComponentIO.toJson(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlowWithDebugOnEachElement());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/debug_populated_flow.json"), jsonString);
    }

}