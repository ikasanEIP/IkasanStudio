package org.ikasan.studio.core.model.ikasan.meta;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IkasanComponentLibraryTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";
//    public static final String TEST_IKASAN_PACK = "V4.0.x";
//    public static final String TEST_IKASAN_PACK = "V3.3.x";
    @Test
    void testThatDeserializationPopulatesTheIkasanComponentLibrary() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(6, IkasanComponentLibrary.getNumberOfComponents(TEST_IKASAN_PACK)),
            () -> assertEquals(
                new TreeSet<>(Arrays.asList("Custom Converter", ComponentMeta.EXCEPTION_RESOLVER_NAME, "Event Generating Consumer", ComponentMeta.FLOW_NAME, ComponentMeta.MODULE_NAME, "Dev Null Producer")),
                new TreeSet<>(IkasanComponentLibrary.getIkasanComponentNames(TEST_IKASAN_PACK)))
        );

        Map<String, ComponentMeta> componentMetaList = IkasanComponentLibrary.getIkasanComponents(TEST_IKASAN_PACK);
        verifyDefaultModuleMeta(componentMetaList.get(ComponentMeta.MODULE_NAME));
        verifyDefaultFlowMeta(componentMetaList.get(ComponentMeta.FLOW_NAME));
        verifyDefaultExceptionResolverMeta((ExceptionResolutionMeta)componentMetaList.get(ComponentMeta.EXCEPTION_RESOLVER_NAME));
    }

    protected void verifyDefaultFlowMeta(ComponentMeta flow) {
        assertAll(
            "Check the flow contains the expected values",
            () -> assertEquals(ComponentMeta.FLOW_NAME, flow.getName()),
            () -> assertEquals("The flow is the container for components and generally represents an atomic action.", flow.getHelpText()),
            () -> assertEquals("org.ikasan.spec.flow.Flow", flow.getComponentType()),
            () -> assertEquals("https://github.com/ikasanEIP/ikasan/blob/3.1.x/ikasaneip/component/Readme.md", flow.getWebHelpURL()),
            () -> assertEquals("Small Flow icon", flow.getSmallIcon().getDescription()),
            () -> assertEquals("Medium Flow icon", flow.getCanvasIcon().getDescription()),
            () -> assertEquals(2, flow.getProperties().size())
        );
    }
    protected void verifyDefaultModuleMeta(ComponentMeta module) {
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(ComponentMeta.MODULE_NAME, module.getName()),
            () -> assertEquals("The module is the container for all flows", module.getHelpText()),
            () -> assertEquals("org.ikasan.spec.module.Module", module.getComponentType()),
            () -> assertEquals("Readme.md", module.getWebHelpURL()),
            () -> assertEquals("Small Module icon", module.getSmallIcon().getDescription()),
            () -> assertEquals("Medium Module icon", module.getCanvasIcon().getDescription()),
            () -> assertEquals(7, module.getProperties().size())
        );
    }

    protected void verifyDefaultExceptionResolverMeta(ExceptionResolutionMeta exceptionResolver) {
        assertAll(
                "Check the Exception Resolver contains the expected values",
                () -> assertEquals(ComponentMeta.EXCEPTION_RESOLVER_NAME, exceptionResolver.getName()),
                () -> assertEquals("Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow.", exceptionResolver.getHelpText()),
                () -> assertEquals("org.ikasan.exceptionResolver.ExceptionResolver", exceptionResolver.getComponentType()),
                () -> assertEquals("Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow.", exceptionResolver.getHelpText()),
                () -> assertEquals("Small Exception Resolver icon", exceptionResolver.getSmallIcon().getDescription()),
                () -> assertEquals("Medium Exception Resolver icon", exceptionResolver.getCanvasIcon().getDescription()),
                () -> assertEquals(2, exceptionResolver.getProperties().size()),
                () -> assertEquals(5, exceptionResolver.getActionList().size()),
                () -> assertEquals(2, exceptionResolver.getProperties().size())
        );
    }
}