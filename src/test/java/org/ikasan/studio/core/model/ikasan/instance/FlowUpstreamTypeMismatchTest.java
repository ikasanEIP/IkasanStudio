package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers Flow#findPayloadSourceElement (the upstream-neighbour traversal, including crossing router branch
 * boundaries) and FlowElement#getUpstreamTypeMismatchWarning (the best-effort type check built on top of it) -
 * both the fixed expectedInputTypes shape (e.g. Default List Splitter, JMS Object Message To Object Converter,
 * or Basic AMQ JMS Producer's multi-candidate "java.lang.String, byte[], java.util.Map, java.io.Serializable")
 * and the per-instance expectedInputTypeProperty shape (e.g. Converter/Broker/Splitter's own 'fromType',
 * Object To XML String Converter's 'objectClass') - see ComponentMeta#getExpectedInputTypes() and
 * #getExpectedInputTypeProperty().
 */
@ExtendWith(SharedResourceExtension.class)
class FlowUpstreamTypeMismatchTest {

    // ---- getEffectiveInputTypeDescription / getEffectiveOutputTypeDescription (no Flow context needed) ----

    @Test
    public void getEffectiveOutputTypeDescription_returns_producedOutputType_for_a_consumer_with_a_fixed_output() throws StudioBuildException {
        FlowElement localFileConsumer = TestFixtures.getLocalFileConsumer(BASE_META_PACK);

        assertEquals("java.util.List<java.io.File>", localFileConsumer.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveInputTypeDescription_is_always_null_for_a_consumer() throws StudioBuildException {
        FlowElement localFileConsumer = TestFixtures.getLocalFileConsumer(BASE_META_PACK);

        assertNull(localFileConsumer.getEffectiveInputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_is_null_for_a_generic_consumer_with_no_fixed_output() throws StudioBuildException {
        FlowElement genericConsumer = TestFixtures.getGenericConsumer(BASE_META_PACK);

        assertNull(genericConsumer.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_is_null_for_a_producer_since_it_is_terminal() throws StudioBuildException {
        FlowElement devNullProducer = TestFixtures.getDevNullProducer(BASE_META_PACK);

        assertNull(devNullProducer.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_for_a_router_mirrors_its_own_input_not_its_toType() throws StudioBuildException {
        // MultiRecipientRouter's own toType ("java.util.List<java.lang.String>") is the routing decision, not
        // the payload - the payload passes through unchanged, so output must mirror fromType instead.
        FlowElement router = TestFixtures.getMultiRecipientRouter(BASE_META_PACK);

        assertEquals("java.lang.String", router.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_for_a_filter_mirrors_its_own_input() throws StudioBuildException {
        FlowElement filter = TestFixtures.getMessageFilter(BASE_META_PACK);

        assertEquals("java.lang.String", filter.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_for_a_translator_uses_its_own_type_property() throws StudioBuildException {
        // Translator has no 'fromType'/'toType' of its own - both input and output come from its 'type'
        // property (see expectedInputTypeProperty="type" in its component-meta_en_GB.json).
        FlowElement translator = TestFixtures.getCustomTranslator(BASE_META_PACK);

        assertEquals("java.lang.String", translator.getEffectiveInputTypeDescription());
        assertEquals("java.lang.String", translator.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_uses_producedOutputType_for_a_fixed_shape_converter_too() throws StudioBuildException {
        // producedOutputType isn't Consumer-only - Object To XML String Converter has no 'toType' property of
        // its own either, but its real output (a marshalled XML string) is just as fixed and knowable.
        FlowElement xmlConverter = TestFixtures.getObjectMessageToXmlStringtConverter(BASE_META_PACK);

        assertEquals("java.lang.String", xmlConverter.getEffectiveOutputTypeDescription());
    }

    @Test
    public void getEffectiveOutputTypeDescription_is_null_when_no_toType_is_declared_eg_default_list_splitter() throws StudioBuildException {
        // Default List Splitter's real output is whichever type was inside the incoming list - not captured
        // anywhere in Studio's metadata, so null (not a guess) is the honest answer.
        FlowElement defaultListSplitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);

        assertNull(defaultListSplitter.getEffectiveOutputTypeDescription());
    }

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
    public void findPayloadSourceElement_skips_over_a_filter_to_find_the_real_payload_source() throws StudioBuildException {
        // Converter(toType=Integer) -> MessageFilter(fromType=String, no toType - it never changes the payload) ->
        // DefaultListSplitter. The Filter itself must not be mistaken for the payload source.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement filter = TestFixtures.getMessageFilter(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        Flow flow = buildFlowWithTopLevelElements(converter, filter, splitter);

        assertEquals(converter, flow.findPayloadSourceElement(splitter));
    }

    @Test
    public void findPayloadSourceElement_skips_over_a_debug_breakpoint_to_find_the_real_payload_source() throws StudioBuildException {
        // Converter(toType=Integer) -> Debug (always transparent, fromType fixed to java.lang.Object) ->
        // DefaultListSplitter. The Debug breakpoint itself must not be mistaken for the payload source.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement debug = TestFixtures.getDebugTransition(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getDefaultListSplitter(BASE_META_PACK);
        Flow flow = buildFlowWithTopLevelElements(converter, debug, splitter);

        assertEquals(converter, flow.findPayloadSourceElement(splitter));
    }

    @Test
    public void getUpstreamTypeMismatchWarning_still_fires_for_a_real_mismatch_behind_a_debug_breakpoint() throws StudioBuildException {
        // Skipping the Debug breakpoint to find the real payload source must not swallow a genuine mismatch
        // that exists further upstream.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement debug = TestFixtures.getDebugTransition(BASE_META_PACK);
        FlowElement splitter = TestFixtures.getCustomSplitter(BASE_META_PACK);   // fromType = java.lang.String
        buildFlowWithTopLevelElements(converter, debug, splitter);

        String warning = splitter.getUpstreamTypeMismatchWarning();

        assertTrue(warning != null && warning.contains("java.lang.Integer"), "Expected a warning naming the mismatched upstream type, got: " + warning);
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_upstream_declares_its_output_as_Object() throws StudioBuildException {
        // Object means "declares/accepts anything" on either side of the comparison - not just when it's this
        // component's own expected input (already covered above), but also when it's the upstream's declared
        // output, e.g. a component whose toType was explicitly set to java.lang.Object.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "java.lang.Object");
        FlowElement splitter = TestFixtures.getCustomSplitter(BASE_META_PACK); // fromType = java.lang.String
        buildFlowWithTopLevelElements(converter, splitter);

        assertNull(splitter.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void skipNonPayloadBearingElements_is_directly_callable_with_a_known_starting_candidate() throws StudioBuildException {
        // DesignerCanvas#applySuggestedInputTypeFromUpstream resolves its starting candidate from drop
        // coordinates (getSurroundingComponents) rather than from a downstream FlowElement's own position, so
        // this must be usable as a public entry point in its own right, not just via findPayloadSourceElement.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement router = TestFixtures.getMultiRecipientRouter(BASE_META_PACK);
        Flow flow = buildFlowWithTopLevelElements(converter, router);

        assertEquals(converter, flow.skipNonPayloadBearingElements(router));
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
    public void getUpstreamTypeMismatchWarning_also_checks_a_components_own_fromType_when_it_has_no_fixed_expectedInputType() throws StudioBuildException {
        // The (custom) Splitter declares no fixed expectedInputTypes, but does have its own 'fromType' (default
        // convention - see ComponentMeta#getExpectedInputTypeProperty) which should be checked the same way.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement splitter = TestFixtures.getCustomSplitter(BASE_META_PACK);   // fromType = java.lang.String
        buildFlowWithTopLevelElements(converter, splitter);

        String warning = splitter.getUpstreamTypeMismatchWarning();

        assertTrue(warning != null && warning.contains("java.lang.Integer"), "Expected a warning naming the mismatched upstream type, got: " + warning);
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_for_a_component_with_neither_expectedInputType_nor_fromType() throws StudioBuildException {
        // DevNullProducer has no fromType property at all and declares no expectedInputTypes - there is simply
        // nothing on this component to check, regardless of what the upstream declares.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement devNullProducer = TestFixtures.getDevNullProducer(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, devNullProducer);

        assertNull(devNullProducer.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_this_components_own_input_type_is_FlowEvent() throws StudioBuildException {
        // Deliberately opting into the full-event escape hatch (see brokerTemplate_en.ftl / converterTemplate_en.ftl
        // / splitterTemplate_en.ftl) is not a mismatch, even though it will never textually match any upstream toType.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement splitter = TestFixtures.getCustomSplitter(BASE_META_PACK);
        splitter.setPropertyValue(FROM_TYPE, "org.ikasan.spec.flow.FlowEvent");
        buildFlowWithTopLevelElements(converter, splitter);

        assertNull(splitter.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void getUpstreamTypeMismatchWarning_fires_for_a_component_with_a_fixed_expectedInputTypes() throws StudioBuildException {
        // JMS Object Message To Object Converter has no user-configurable input type of its own - it always
        // expects a raw JMS ObjectMessage (see its component-meta_en_GB.json "expectedInputTypes").
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement objectMessageConverter = TestFixtures.getObjectMessageToObjectConverter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, objectMessageConverter);

        String warning = objectMessageConverter.getUpstreamTypeMismatchWarning();

        assertTrue(warning != null && warning.contains("ObjectMessage"), "Expected a warning naming the expected ObjectMessage type, got: " + warning);
        assertTrue(warning.contains("\nSuggested Fix: add a Converter"),
                "Expected the warning to recommend the transformation that can change payload type, got: " + warning);
    }

    @Test
    public void getUpstreamTypeMismatchWarning_uses_expectedInputTypeProperty_when_the_component_names_a_different_property() throws StudioBuildException {
        // Object To XML String Converter has no 'fromType' - its metadata instead points expectedInputTypeProperty
        // at 'objectClass', which TestFixtures sets to java.lang.String.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement xmlConverter = TestFixtures.getObjectMessageToXmlStringtConverter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, xmlConverter);

        String warning = xmlConverter.getUpstreamTypeMismatchWarning();

        assertTrue(warning != null && warning.contains("java.lang.Integer"), "Expected a warning naming the mismatched upstream type, got: " + warning);
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

    // Real component - Basic AMQ JMS Producer declares expectedInputTypes="java.lang.String, byte[],
    // java.util.Map, java.io.Serializable" (Spring JmsTemplate's actual accepted set, verified from its real
    // source this session) - the motivating multi-candidate case for this whole feature.
    private FlowElement getBasicAmqJmsProducer() throws StudioBuildException {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Basic AMQ JMS Producer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Basic AMQ JMS Producer")
                .build();
    }

    // ---- getUpstreamTypeMismatchWarning(Function) - multi-candidate expectedInputTypes ----

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_upstream_matches_any_one_of_several_candidates() throws StudioBuildException {
        // Upstream declares java.util.HashMap - doesn't match "java.lang.String" or "byte[]", but does match
        // "java.util.Map" (the third of four comma-separated candidates) via the existing substring heuristic.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "java.util.HashMap");
        FlowElement producer = getBasicAmqJmsProducer();
        buildFlowWithTopLevelElements(converter, producer);

        assertNull(producer.getUpstreamTypeMismatchWarning(candidate -> null));
    }

    @Test
    public void getUpstreamTypeMismatchWarning_lists_every_candidate_when_none_match_and_serializable_is_confirmed_false() throws StudioBuildException {
        // org.ikasan.filetransfer.Payload matches none of String/byte[]/Map by name, and the checker confirms
        // it does NOT implement Serializable either (the real, verified fact behind this whole feature -
        // Ikasan's DefaultPayload implements Payload, not Serializable) - every candidate is now definitively
        // ruled out, so the warning fires and names all four.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "org.ikasan.filetransfer.Payload");
        FlowElement producer = getBasicAmqJmsProducer();
        buildFlowWithTopLevelElements(converter, producer);

        String warning = producer.getUpstreamTypeMismatchWarning(candidate -> Boolean.FALSE);

        assertTrue(warning != null
                        && warning.contains("java.lang.String") && warning.contains("byte[]")
                        && warning.contains("java.util.Map") && warning.contains("java.io.Serializable"),
                "Expected a warning naming all 4 candidates, got: " + warning);
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_serializable_is_confirmed_true() throws StudioBuildException {
        // Same non-matching-by-name upstream type as above, but this time the checker confirms it DOES
        // implement Serializable - that one candidate is satisfied, so no warning despite the other 3 not
        // matching by name.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "org.ikasan.filetransfer.Payload");
        FlowElement producer = getBasicAmqJmsProducer();
        buildFlowWithTopLevelElements(converter, producer);

        assertNull(producer.getUpstreamTypeMismatchWarning(candidate -> Boolean.TRUE));
    }

    @Test
    public void getUpstreamTypeMismatchWarning_is_silent_when_serializable_cannot_be_resolved_either_way() throws StudioBuildException {
        // The checker returns null (couldn't resolve the class at all, or genuinely doesn't know) - this is the
        // "not enough information" case this method has always stayed silent for elsewhere, not a real mismatch.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "org.ikasan.filetransfer.Payload");
        FlowElement producer = getBasicAmqJmsProducer();
        buildFlowWithTopLevelElements(converter, producer);

        assertNull(producer.getUpstreamTypeMismatchWarning(candidate -> null));
    }

    @Test
    public void getUpstreamTypeMismatchWarning_no_arg_overload_behaves_as_if_serializable_is_always_unresolved() throws StudioBuildException {
        // The plain no-arg overload (used by anything without real PSI access) supplies a checker that always
        // returns null - so on its own it can only ever use a Serializable candidate to suppress a warning it
        // can't rule out, never to confirm a real one; the same upstream type that produces a definite warning
        // with a checker confirming Boolean.FALSE (see the test above) must stay silent here.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        converter.setPropertyValue(TO_TYPE, "org.ikasan.filetransfer.Payload");
        FlowElement producer = getBasicAmqJmsProducer();
        buildFlowWithTopLevelElements(converter, producer);

        assertNull(producer.getUpstreamTypeMismatchWarning());
    }

    @Test
    public void getUpstreamTypeMismatchWarning_single_candidate_backward_compatibility_is_unaffected_by_the_rename() throws StudioBuildException {
        // JMS Object Message To Object Converter's expectedInputTypes has no comma - a single candidate must
        // behave exactly as it did before this field was renamed/reinterpreted (see the equivalent pre-existing
        // test above, getUpstreamTypeMismatchWarning_fires_for_a_component_with_a_fixed_expectedInputTypes) -
        // this one just also exercises the Function-taking overload directly with an always-null checker, to
        // confirm the Serializable special case never fires for a candidate list that doesn't contain it.
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK); // toType = java.lang.Integer
        FlowElement objectMessageConverter = TestFixtures.getObjectMessageToObjectConverter(BASE_META_PACK);
        buildFlowWithTopLevelElements(converter, objectMessageConverter);

        String warning = objectMessageConverter.getUpstreamTypeMismatchWarning(candidate -> {
            throw new AssertionError("serializableChecker should never be invoked - java.io.Serializable is not among this component's candidates");
        });

        assertTrue(warning != null && warning.contains("ObjectMessage"), "Expected a warning naming the expected ObjectMessage type, got: " + warning);
    }
}
