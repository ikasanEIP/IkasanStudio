package org.ikasan.studio.core.io;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.TestUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
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
    public static void classSetup() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
    }

    @Test
    public void testFlowElementSerializeToJson() throws IOException {
        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

    @Test
    public void testFlowSerializeToJson() throws IOException {
        Flow flow1 = TestFixtures.getUnbuiltFlow().build();
        String jsonString = ComponentIO.toJson(flow1);

        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flow.json"), jsonString);
    }

    @Test
    public void testModuleSerializeToJson() throws IOException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/module.json"), ComponentIO.toJson(module));
    }


    //@NEXT EventGeneratingConsumer replicated
    @Test
    public void testPopulatedFlowSerializeToJson() throws IOException {
        String jsonString = ComponentIO.toJson(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow());
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_flow.json"), jsonString);
    }

    @Test
    public void testPopulatedModuleSerializeToJson() throws IOException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(Collections.singletonList(TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow()));
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/populated_module.json"), ComponentIO.toJson(module));
    }

    @Test
    public void testDevNullFlowElementSerialiseToJson() throws IOException {

        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        assertEquals(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json"), ComponentIO.toJson(devNullProducer));
    }

}