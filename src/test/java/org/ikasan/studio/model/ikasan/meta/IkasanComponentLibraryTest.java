package org.ikasan.studio.model.ikasan.meta;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.model.ikasan.meta.ComponentMeta.FLOW_NAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IkasanComponentLibraryTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";
    @Test
    void testThatDeserializationPopulatesTheIkasanComponentLibrary() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(6, IkasanComponentLibrary.getNumberOfComponents(TEST_IKASAN_PACK)),
            () -> assertEquals(
                new TreeSet<>(Arrays.asList("Custom Converter", "Exception Resolver", "Event Generating Consumer", ComponentType.Flow.toString(), ComponentType.Module.toString(), "Dev Null Producer")),
                new TreeSet<>(IkasanComponentLibrary.getIkasanComponentNames(TEST_IKASAN_PACK)))
        );

        Map<String, ComponentMeta> componentMetaList = IkasanComponentLibrary.getIkasanComponents(TEST_IKASAN_PACK);
        verifyDefaultModuleMeta(componentMetaList.get(ComponentType.Module.toString()));
        verifyDefaultFlowMeta(componentMetaList.get(ComponentType.Flow.toString()));
        verifyDefaultExceptionResolverMeta((ExceptionResolutionMeta)componentMetaList.get("Exception Resolver"));
    }

    protected void verifyDefaultFlowMeta(ComponentMeta flow) {
        assertThat(flow.getName(), is(FLOW_NAME));
        assertThat(flow.getHelpText(), is("The flow is the container for components and generally represents an atomic action."));
        assertThat(flow.getComponentType(), is(ComponentType.Flow.classType));
        assertThat(flow.getWebHelpURL(), is("Readme.md"));
        assertThat(flow.getSmallIcon().getDescription(), is("Small Flow icon"));
        assertThat(flow.getCanvasIcon().getDescription(), is("Medium Flow icon"));
        assertThat(flow.getProperties().size(), is(2));
    }
    protected void verifyDefaultModuleMeta(ComponentMeta module) {
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(ComponentType.Module.toString(), module.getName()),
            () -> assertEquals("The module is the container for all flows", module.getHelpText()),
            () -> assertEquals(ComponentType.Module.classType, module.getComponentType()),
            () -> assertEquals("Readme.md", module.getWebHelpURL()),
            () -> assertEquals("Small Module icon", module.getSmallIcon().getDescription()),
            () -> assertEquals("Medium Module icon", module.getCanvasIcon().getDescription()),
            () -> assertEquals(7, module.getProperties().size())
        );
    }

    protected void verifyDefaultExceptionResolverMeta(ExceptionResolutionMeta exceptionResolver) {
        assertThat(exceptionResolver.getName(), is("Exception Resolver"));
        assertThat(exceptionResolver.getHelpText(), is("Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow."));
        assertThat(exceptionResolver.getComponentType(), is(ComponentType.ExceptionResolver.classType));
        assertThat(exceptionResolver.getWebHelpURL(), is("Readme.md"));
        assertThat(exceptionResolver.getSmallIcon().getDescription(), is("Small Exception Resolver icon"));
        assertThat(exceptionResolver.getCanvasIcon().getDescription(), is("Medium Exception Resolver icon"));
        assertThat(exceptionResolver.getProperties().size(), is(2));
        assertThat(exceptionResolver.getActionList().size(), is(5));
        assertThat(exceptionResolver.getExceptionsCaught().size(), is(7));
    }
}