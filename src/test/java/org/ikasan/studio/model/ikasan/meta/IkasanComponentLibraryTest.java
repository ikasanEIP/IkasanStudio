package org.ikasan.studio.model.ikasan.meta;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary.*;
import static org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta.*;

class IkasanComponentLibraryTest {
    public static final String TEST_IKASAN_PACK = "Vtest.x";
    @Test
    void testThatDeserializationPopulatesTheIkasanComponentLibrary() {
        IkasanComponentLibrary.refreshComponentLibrary(TEST_IKASAN_PACK);

        assertThat(
                IkasanComponentLibrary.getNumberOfComponents(TEST_IKASAN_PACK),
                is(6));

        assertThat(
                new TreeSet<>(IkasanComponentLibrary.getIkasanComponentNames(TEST_IKASAN_PACK)),
                is(new TreeSet<>(Arrays.asList("CUSTOM_CONVERTER", EXCEPTION_RESOLVER, "EVENT_GENERATING_CONSUMER", FLOW, MODULE, "DEV_NULL_PRODUCER"))));

        Map<String, IkasanComponentMeta> componentMetaList = IkasanComponentLibrary.getIkasanComponents(TEST_IKASAN_PACK);
        verifyDefaultModuleMeta(componentMetaList.get(MODULE));
        verifyDefaultFlowMeta(componentMetaList.get(FLOW));
        verifyDefaultExceptionResolverMeta((IkasanExceptionResolutionMeta)componentMetaList.get(EXCEPTION_RESOLVER));
    }

    protected void verifyDefaultFlowMeta(IkasanComponentMeta flow) {
        assertThat(flow.getName(), is(FLOW_NAME));
        assertThat(flow.getHelpText(), is("The flow is the container for components and generally represents an atomic action."));
        assertThat(flow.getComponentType(), is(FLOW_TYPE));
        assertThat(flow.getWebHelpURL(), is("Readme.md"));
        assertThat(flow.getSmallIcon().getDescription(), is("Small FLOW icon"));
        assertThat(flow.getCanvasIcon().getDescription(), is("Medium FLOW icon"));
        assertThat(flow.getProperties().size(), is(2));
    }
    protected void verifyDefaultModuleMeta(IkasanComponentMeta module) {
        assertThat(module.getName(), is(MODULE_NAME));
        assertThat(module.getHelpText(), is("The module is the container for all flows"));
        assertThat(module.getComponentType(), is(MODULE_TYPE));
        assertThat(module.getWebHelpURL(), is("Readme.md"));
        assertThat(module.getSmallIcon().getDescription(), is("Small MODULE icon"));
        assertThat(module.getCanvasIcon().getDescription(), is("Medium MODULE icon"));
        assertThat(module.getProperties().size(), is(7));
    }

    protected void verifyDefaultExceptionResolverMeta(IkasanExceptionResolutionMeta exceptionResolver) {
        assertThat(exceptionResolver.getName(), is(EXCEPTION_RESOLVER_NAME));
        assertThat(exceptionResolver.getHelpText(), is("Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow."));
        assertThat(exceptionResolver.getComponentType(), is(EXCEPTION_RESOLVER_TYPE));
        assertThat(exceptionResolver.getWebHelpURL(), is("Readme.md"));
        assertThat(exceptionResolver.getSmallIcon().getDescription(), is("Small EXCEPTION_RESOLVER icon"));
        assertThat(exceptionResolver.getCanvasIcon().getDescription(), is("Medium EXCEPTION_RESOLVER icon"));
        assertThat(exceptionResolver.getProperties().size(), is(2));
        assertThat(exceptionResolver.getActionList().size(), is(5));
        assertThat(exceptionResolver.getExceptionsCaught().size(), is(7));
    }
}