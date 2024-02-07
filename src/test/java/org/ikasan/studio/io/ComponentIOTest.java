package org.ikasan.studio.io;

import org.ikasan.studio.StudioException;
import org.ikasan.studio.generator.TestUtils;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.IkasanBaseElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ComponentIOTest {

    @Test
    public void testModuleToJson() throws IOException {
        Module module = new Module();
        module.setVersion("1.3");
        module.setComponentName("");
        module.setDescription("The Description");
        assertThat(ComponentIO.toJson(module), is(TestUtils.getFileAsString("/org/ikasan/studio/module.json")));
    }

    public static final String TEST_IKASAN_PACK = "Vtest.x";
    @Test
    public void testInstanceComponentToJson() throws IOException {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);
        IkasanComponentMeta devNullProducerMeta = IkasanComponentLibrary.getIkasanComponent(TEST_IKASAN_PACK, "DEV_NULL_PRODUCER");
        FlowElement devNullProducer = new FlowElement(devNullProducerMeta, null);
        devNullProducer.setComponentName("My DN Producer");
        devNullProducer.setDescription("The DN Description");
        assertThat(ComponentIO.toJson(devNullProducer), is(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json")));
    }

    @Test
    public void testModuleInstanceDeserialise() throws IOException, StudioException {
        IkasanBaseElement module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/module.json");
        System.out.println("BB");

//        assertThat(ComponentIO.toJson(devNullProducer), is(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json")));
    }
//    @Test
//    public void testInstanceComponentDeserialise() throws IOException, StudioException {
//        IkasanBaseElement devNullProducer = ComponentIO.deserializeInstanceComponent("org/ikasan/studio/flowElement.json");
//        System.out.println("BB");
//
//        assertThat(ComponentIO.toJson(devNullProducer), is(TestUtils.getFileAsString("/org/ikasan/studio/flowElement.json")));
//    }

}