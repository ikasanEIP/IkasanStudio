package org.ikasan.studio.io;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Transition;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta.*;
import static org.junit.jupiter.api.Assertions.*;

class ComponentIODeserializeTest {
    //@NEXT EventGeneratingConsumer replicated

    @Test
    public void testModuleMetaDeserialise() throws StudioException {
        ComponentMeta component = (ComponentMeta)ComponentIO.deserializeMetaComponent("studio/Vtest.x/components/Module/attributes_en_GB.json");
        Dependency firstDependency = new Dependency();
        firstDependency.setArtifactId("ikasan-connector-base");
        firstDependency.setGroupId("org.ikasan");
        firstDependency.setVersion("3.1.0");
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(8, component.getJarDependencies().size()),
            () -> assertEquals(firstDependency.toString(), component.getJarDependencies().get(0).toString())
        );
    }

    @Test
    public void testMissingNameAttributesCausesError() {
        StudioException thrown = assertThrowsExactly(
            StudioException.class,
            () ->  ComponentIO.deserializeMetaComponent("studio/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json")
        );

        assertTrue(thrown.getMessage().contains(
            "The serialised data in [studio/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: name is marked non-null but is null"),
                "Incorrect exception message, was [" + thrown.getMessage() + "]");
    }
    @Test
    public void testMissingTypeAttributesCausesError() {
        StudioException thrown = assertThrowsExactly(
            StudioException.class,
            () ->  ComponentIO.deserializeMetaComponent("studio/Vtest.x/BadComponentMeta/no_type_attributes_en_GB.json")
        );

        assertTrue(thrown.getMessage().contains(
            "The serialised data in [studio/Vtest.x/BadComponentMeta/no_type_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: componentType is marked non-null but is null"),
                "Incorrect exception message, was [" + thrown.getMessage() + "]");
    }

    @Test
    public void testMissingImplementingClassAttributesCausesError() {
        StudioException thrown = assertThrowsExactly(
            StudioException.class,
            () ->  ComponentIO.deserializeMetaComponent("studio/Vtest.x/BadComponentMeta/no_implementing_class_attributes_en_GB.json")
        );

        assertTrue(thrown.getMessage().contains(
            "The serialised data in [studio/Vtest.x/BadComponentMeta/no_implementing_class_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: implementingClass is marked non-null but is null"),
                "Incorrect exception message, was [" + thrown.getMessage() + "]");
    }

    @Test
    public void testModuleInstanceDeserialize() throws StudioException {
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
            () -> assertEquals("8092", module.getH2PortNumber()),
            () -> assertEquals("8093", module.getH2WebPortNumber()),
            () -> assertEquals("8091", module.getPort()),

            () -> assertEquals(1, flows.size()),
            () -> assertEquals(2, flow1.getConfiguredProperties().size()),
            () -> assertEquals("MyFlow1", flow1.getName()),

            () -> assertEquals(1, eventGeneratingConsumer.getConfiguredProperties().size()),

            () -> assertEquals("My Event Generating Consumer", eventGeneratingConsumer.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> Assertions.assertNotNull(eventGeneratingConsumer.getViewHandler()),

            () -> assertEquals(2, transition.size()),
            () -> assertEquals("default", transition.get(0).getName()),
            () -> assertEquals("My Event Generating Consumer", transition.get(0).getFrom()),
            () -> assertEquals("My Custom Converter", transition.get(0).getTo()),

            () -> assertEquals(4, customConverter.getConfiguredProperties().size()),
            () -> assertEquals("Custom Converter", customConverter.getComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter", customConverter.getComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter.Custom", customConverter.getComponentMeta().getImplementingClass()),
            () -> assertEquals("myConverter", customConverter.getConfiguredProperties().get(USER_IMPLEMENTED_CLASS_NAME).getValue()),
            () -> assertEquals("My Custom Converter", customConverter.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("java.lang.String", customConverter.getConfiguredProperties().get(FROM_TYPE).getValue()),
            () -> assertEquals("java.lang.Integer", customConverter.getConfiguredProperties().get(TO_TYPE).getValue()),
            () -> Assertions.assertNotNull(customConverter.getViewHandler()),

            () -> assertEquals(1, devNullProducer.getConfiguredProperties().size()),
            () -> assertEquals("Dev Null Producer", devNullProducer.getComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.endpoint.Producer", devNullProducer.getComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl", devNullProducer.getComponentMeta().getImplementingClass()),
            () -> assertEquals("My DevNull Producer", devNullProducer.getConfiguredProperties().get(COMPONENT_NAME).getValue()),
            () -> Assertions.assertNotNull(devNullProducer.getViewHandler())
        );
    }

    @Test
    public void testModuleInstanceDeserializeIsRobustNotFailingWithEmptyElements() throws StudioException {
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
            () -> Assertions.assertTrue(transition.isEmpty())
        );
    }
}