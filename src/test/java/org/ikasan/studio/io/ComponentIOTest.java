package org.ikasan.studio.io;

import org.ikasan.studio.StudioException;
import org.ikasan.studio.generator.TestUtils;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

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
    public void testModuleInstanceDeserialise() throws StudioException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module.json");
        assertThat(module.getName(), is("My Module"));
        assertThat(module.getDescription(), is("The Description"));
        List<Flow> flowList = module.getFlows();
        assertThat(flowList.size(),is(2));
        assertThat(flowList.get(0).getName(), is("flow1"));
        assertThat(flowList.get(1).getName(), is("flow2"));
    }
}