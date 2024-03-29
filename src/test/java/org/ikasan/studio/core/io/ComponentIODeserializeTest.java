package org.ikasan.studio.core.io;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.*;
import static org.junit.jupiter.api.Assertions.*;

class ComponentIODeserializeTest {
    //@NEXT EventGeneratingConsumer replicated

    @Test
    public void testModuleMetaDeserialise() throws StudioBuildException {
        ComponentMeta component = (ComponentMeta) ComponentIO.deserializeMetaComponent("studio/metapack/Vtest.x/library/Module/components/Module/attributes_en_GB.json");
        Dependency firstDependency = new Dependency();
        firstDependency.setArtifactId("ikasan-connector-base");
        firstDependency.setGroupId("org.ikasan");
        firstDependency.setVersion("3.1.0");
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(8, component.getJarDependencies().size())
        );
    }

    @Test
    public void testMissingNameAttributesCausesError() {
        StudioBuildException thrown = assertThrowsExactly(
                StudioBuildException.class,
            () ->  ComponentIO.deserializeMetaComponent("studio/metapack/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json")
        );

        assertTrue(thrown.getMessage().contains(
            "The serialised data in [studio/metapack/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.core.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: name is marked non-null but is null"),
                "Incorrect exception message, was [" + thrown.getMessage() + "]");
    }

    @Test
    public void testMissingImplementingClassAttributesCausesError() {
        StudioBuildException thrown = assertThrowsExactly(
                StudioBuildException.class,
            () ->  ComponentIO.deserializeMetaComponent("studio/metapack/Vtest.x/BadComponentMeta/no_implementing_class_attributes_en_GB.json")
        );

        assertTrue(thrown.getMessage().contains(
            "The serialised data in [studio/metapack/Vtest.x/BadComponentMeta/no_implementing_class_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.core.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: implementingClass is marked non-null but is null"),
                "Incorrect exception message, was [" + thrown.getMessage() + "]");
    }

    @Test
    public void testModuleInstanceDeserialize() throws StudioBuildException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_full_module_with_exception_resolver.json");
        List<Flow> flows = module.getFlows();
        Flow flow1 = flows.get(0);
        FlowElement eventGeneratingConsumer = flow1.getConsumer();
        List<Transition> transition = flow1.getTransitions();
        FlowElement customConverter = flow1.getFlowElements().get(0);
        FlowElement devNullProducer = flow1.getFlowElements().get(1);
        ExceptionResolver exceptionResolver = flow1.getExceptionResolver();
        ExceptionResolution resourceExceptionResolution = flow1.getExceptionResolver().getIkasanExceptionResolutionMap().get("javax.resource.ResourceException.class");
        ExceptionResolution jmsExceptionResolution = flow1.getExceptionResolver().getIkasanExceptionResolutionMap().get("javax.jms.JMSException.class");

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals("V3.3.x", module.getVersion()),
            () -> assertEquals("A to B convert", module.getName()),
            () -> assertEquals("My first module", module.getDescription()),
            () -> assertEquals("co.uk.test", module.getApplicationPackageName()),
            () -> assertEquals("8092", module.getH2PortNumber()),
            () -> assertEquals("8093", module.getH2WebPortNumber()),
            () -> assertEquals("8091", module.getPort()),

            () -> assertEquals(1, flows.size()),
            () -> assertEquals(2, flow1.getComponentProperties().size()),
            () -> assertEquals("MyFlow1", flow1.getName()),

            () -> assertEquals(2, exceptionResolver.getIkasanExceptionResolutionMap().size()),

            () -> assertEquals("javax.resource.ResourceException.class", resourceExceptionResolution.getExceptionsCaught()),
            () -> assertEquals("ignore", resourceExceptionResolution.getTheAction()),
            () -> Assertions.assertNull(resourceExceptionResolution.getComponentProperties()),

            () -> assertEquals("javax.jms.JMSException.class", jmsExceptionResolution.getExceptionsCaught()),
            () -> assertEquals("ignore", resourceExceptionResolution.getTheAction()),
            () -> assertEquals(2, jmsExceptionResolution.getComponentProperties().size()),
            () -> assertEquals("1", jmsExceptionResolution.getComponentProperties().get("delay").getValue().toString()),
            () -> assertEquals("2", jmsExceptionResolution.getComponentProperties().get("interval").getValue().toString()),

            () -> assertEquals(1, eventGeneratingConsumer.getComponentProperties().size()),

            () -> assertEquals("My Event Generating Consumer", eventGeneratingConsumer.getComponentProperties().get(COMPONENT_NAME).getValue()),

            () -> assertEquals(2, transition.size()),
            () -> assertEquals("default", transition.get(0).getName()),
            () -> assertEquals("My Event Generating Consumer", transition.get(0).getFrom()),
            () -> assertEquals("My Custom Converter", transition.get(0).getTo()),

            () -> assertEquals(4, customConverter.getComponentProperties().size()),
            () -> assertEquals("Custom Converter", customConverter.getComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter", customConverter.getComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.spec.component.transformation.Converter.Custom", customConverter.getComponentMeta().getImplementingClass()),
            () -> assertEquals("myConverter", customConverter.getComponentProperties().get(USER_IMPLEMENTED_CLASS_NAME).getValue()),
            () -> assertEquals("My Custom Converter", customConverter.getComponentProperties().get(COMPONENT_NAME).getValue()),
            () -> assertEquals("java.lang.String", customConverter.getComponentProperties().get(FROM_TYPE).getValue()),
            () -> assertEquals("java.lang.Integer", customConverter.getComponentProperties().get(TO_TYPE).getValue()),

            () -> assertEquals(1, devNullProducer.getComponentProperties().size()),
            () -> assertEquals("Dev Null Producer", devNullProducer.getComponentMeta().getName()),
            () -> assertEquals("org.ikasan.spec.component.endpoint.Producer", devNullProducer.getComponentMeta().getComponentType()),
            () -> assertEquals("org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl", devNullProducer.getComponentMeta().getImplementingClass()),
            () -> assertEquals("My DevNull Producer", devNullProducer.getComponentProperties().get(COMPONENT_NAME).getValue())
        );
    }

    @Test
    public void testModuleInstanceDeserializeIsRobustNotFailingWithEmptyElements() throws StudioBuildException {
        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module_with_empty_elements.json");
        List<Flow> flows = module.getFlows();
        Flow flow1 = flows.get(0);
        FlowElement elements = flow1.getConsumer();
        List<Transition> transition = flow1.getTransitions();

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals("V3.3.x", module.getVersion()),
            () -> assertEquals("A to B convert", module.getName()),
            () -> assertEquals("My first module", module.getDescription()),
            () -> assertEquals("co.uk.test", module.getApplicationPackageName()),
            () -> assertEquals("1", module.getH2PortNumber()),
            () -> assertEquals("2", module.getH2WebPortNumber()),
            () -> assertEquals("3", module.getPort()),

            () -> assertEquals(1, flows.size()),
            () -> assertEquals(1, flow1.getComponentProperties().size()),
            () -> assertEquals("Flow1", flow1.getName()),

            () -> Assertions.assertNull(elements),
            () -> Assertions.assertTrue(transition.isEmpty())
        );
    }

//    @Test
//    public void testModuleInstanceDeserializeIsRobustNoVersion() throws StudioBuildException {
//        Module module = ComponentIO.deserializeModuleInstance("org/ikasan/studio/populated_module_no_version.json");
//        List<Flow> flows = module.getFlows();
//        Flow flow1 = flows.get(0);
//        FlowElement elements = flow1.getConsumer();
//        List<Transition> transition = flow1.getTransitions();
//
//        assertAll(
//            "Check the module contains the expected values",
//            () -> assertEquals("V3.3.x", module.getVersion()),
//            () -> assertEquals("A to B convert", module.getName()),
//            () -> assertEquals("My first module", module.getDescription()),
//            () -> assertEquals("co.uk.test", module.getApplicationPackageName()),
//            () -> assertEquals("1", module.getH2PortNumber()),
//            () -> assertEquals("2", module.getH2WebPortNumber()),
//            () -> assertEquals("3", module.getPort()),
//
//            () -> assertEquals(1, flows.size()),
//            () -> assertEquals(1, flow1.getComponentProperties().size()),
//            () -> assertEquals("Flow1", flow1.getName()),
//
//            () -> Assertions.assertNull(elements),
//            () -> Assertions.assertTrue(transition.isEmpty())
//        );
//    }
//
//    @Test
//    public void testMissingNameAttributesCausesError() {
//        StudioBuildException thrown = assertThrowsExactly(
//                StudioBuildException.class,
//                () ->  ComponentIO.deserializeMetaComponent("studio/metapack/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json")
//        );
//
//        assertTrue(thrown.getMessage().contains(
//                        "The serialised data in [studio/metapack/Vtest.x/BadComponentMeta/no_name_attributes_en_GB.json] could not be read due to [Cannot construct instance of `org.ikasan.studio.core.model.ikasan.meta.ComponentMeta$ComponentMetaBuilderImpl`, problem: name is marked non-null but is null"),
//                "Incorrect exception message, was [" + thrown.getMessage() + "]");
//    }
}