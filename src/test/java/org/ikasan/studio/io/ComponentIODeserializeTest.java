package org.ikasan.studio.io;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Transition;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentIODeserializeTest {
    //@NEXT EventGeneratingConsumer replicated

    @Test
    public void testModuleMetaDeserialise() throws StudioException {
        IkasanComponentMeta component = (IkasanComponentMeta)ComponentIO.deserializeMetaComponent("studio/Vtest.x/components/MODULE/attributes_en_GB.json");
        Dependency firstDependency = new Dependency();
        firstDependency.setArtifactId("ikasan-connector-base");
        firstDependency.setGroupId("org.ikasan");
        firstDependency.setVersion("3.1.0");
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(8, component.getJarDepedencies().size()),
            () -> assertEquals(firstDependency.toString(), component.getJarDepedencies().get(0).toString())
        );
    }


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
            () -> Assertions.assertNotNull(eventGeneratingConsumer.getViewHandler()),

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
            () -> Assertions.assertNotNull(customConverter.getViewHandler()),

            () -> assertEquals(2, devNullProducer.getConfiguredProperties().size()),
            () -> assertEquals("Dev Null Producer", devNullProducer.getIkasanComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.endpoint.Producer", devNullProducer.getIkasanComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl", devNullProducer.getIkasanComponentMeta().getImplementingClass()),
            () -> assertEquals("My DevNull Producer", devNullProducer.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("The DevNull Description", devNullProducer.getDescription()),
            () -> Assertions.assertNotNull(devNullProducer.getViewHandler())
        );
    }

    @Test
    public void testModuleInstanceDeserialiseIsRobustNotFailingWithEmptyElements() throws StudioException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module_with_empty_elements.json");
        List<Flow> flows = module.getFlows();
        Flow flow1 = flows.get(0);
        FlowElement elements = flow1.getConsumer();
        List<Transition> transition = flow1.getTransitions();

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

            () -> Assertions.assertNull(elements),
            () -> Assertions.assertNull(transition)
        );
    }
}