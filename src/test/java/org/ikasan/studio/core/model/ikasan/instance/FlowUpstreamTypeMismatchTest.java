package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.TO_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers Flow#findPayloadSourceElement (the upstream-neighbour traversal, including crossing router branch
 * boundaries) and FlowElement#getUpstreamTypeMismatchWarning (the best-effort Default List Splitter check built
 * on top of it) - see DefaultListSplitter/component-meta_en_GB.json's "expectedInputType".
 */
@ExtendWith(SharedResourceExtension.class)
class FlowUpstreamTypeMismatchTest {

    @Test
    public void findPayloadSourceElement_returns_previous_sibling_in_same_route() throws StudioBuildException {
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        Flow flow = buildFlowWithTopLevelElements(converter, splitter);

        assertEquals(converter, flow.findPayloadSourceElement(splitter));
    }

    @Test
    public void findPayloadSourceElement_returns_consumer_when_element_is_first_in_top_level_route() throws StudioBuildException {
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        Flow flow = buildFlowWithTopLevelElements(splitter);

        assertEquals(flow.getConsumer(), flow.findPayloadSourceElement(splitter));
    }

    @Test
    public void findPayloadSourceElement_skips_over_router_to_find_the_real_payload_source() throws StudioBuildException {
        // Consumer -> Converter(toType=Integer) -> MultiRecipientRouter -> [route1: DefaultListSplitter -> DevNullProducer1]
        FlowElement eventGeneratingConsumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement router = TestFixtures.getMultiRecipientRouter(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        FlowElement devNullProducer = TestFixtures.getDevNullProducer(BASE_META_PACK);

        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK).consumer(eventGeneratingConsumer).build();

        FlowRoute branch = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .routeName("route1")
                .flowElements(new ArrayList<>(Arrays.asList(splitter, devNullProducer)))
                .build();
        FlowRoute topLevelRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(Arrays.asList(converter, router)))
                .childRoutes(Collections.singletonList(branch))
                .build();
        flow.setFlowRoute(topLevelRoute);
        wireContainingFlowRoute(topLevelRoute);

        // The router itself is skipped - its own 'toType' is the routing decision, not the payload.
        assertEquals(converter, flow.findPayloadSourceElement(splitter));
    }

    @Test
    public void getUpstreamTypeMismatchWarning_fires_when_upstream_toType_does_not_look_like_a_list() throws StudioBuildException {
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, splitter);

        String warning = splitter.getUpstreamTypeMismatchWarning();

        assertTrue(warning != null && warning.contains("java.lang.Integer"), "Expected a warning naming the mismatched upstream type, got: " + warning);
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_upstream_toType_looks_like_a_list() throws StudioBuildException {
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "java.util.List<java.lang.Integer>");
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, splitter);

        assertNull(splitter.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_there_is_no_upstream_type_information() throws StudioBuildException {
        // Default List Splitter directly after the Consumer - Consumers never declare a 'toType' in Studio's
        // metadata, so there is nothing to check against; silence is the correct (safe) answer here.
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        buildFlowWithTopLevelElements(splitter);

        assertNull(splitter.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_for_components_with_no_expectedInputType() throws StudioBuildException {
        // The (custom) Splitter component declares no expectedInputType - only Default List Splitter does.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getCustomSplitter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, splitter);

        assertNull(splitter.getUpstreamTypeMismatchWarning());
    }

    /**
     * Builds a flow with an Event Generating Consumer followed by the given elements, in order, in the flow's
     * single top-level route - wiring each element's containingFlowRoute the way real canvas/deserialization
     * code paths do (see DesignerCanvas / ModuleDeserializer), which the plain FlowRoute builder used by most
     * other fixtures in this codebase does not do on its own.
     */
    private Flow buildFlowWithTopLevelElements(FlowElement... elements) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK).consumer(eventGeneratingConsumer).build();
        FlowRoute topLevelRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(elements)))
                .build();
        flow.setFlowRoute(topLevelRoute);
        wireContainingFlowRoute(topLevelRoute);
        return flow;
    }

    private void wireContainingFlowRoute(FlowRoute route) {
        for (FlowElement flowElement : route.getFlowElements()) {
            flowElement.setContainingFlowRoute(route);
        }
        for (FlowRoute childRoute : route.getChildRoutes()) {
            wireContainingFlowRoute(childRoute);
        }
    }
}
