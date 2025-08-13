package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.*;


class FlowRouteTest {
    Flow testFlow;

    // write before each method

    @BeforeEach
    public void setUp() throws StudioBuildException {
        testFlow = TestFixtures.getUnbuiltFlow(BASE_META_PACK).build();
    }

    @Test
    public void test_flow_route_instantiation() throws StudioBuildException {
        Flow testFlow = TestFixtures.getUnbuiltFlow(BASE_META_PACK).build();
        String routeName = "route1";
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = new ArrayList<>();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .routeName(routeName)
                .childRoutes(childRoutes)
                .flowElements(flowElements)
                .build();

        assertEquals(testFlow, flowRoute.getFlow());
        assertEquals(routeName, flowRoute.getRouteName());
        assertEquals(childRoutes, flowRoute.getChildRoutes());
        assertEquals(flowElements, flowRoute.getFlowElements());
    }

    @Test
    public void test_default_route_name_and_default_child_routes_and_default_flow_elements() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        assertEquals(Transition.DEFAULT_TRANSITION_NAME, flowRoute.getRouteName());
        assertNotNull(flowRoute.getChildRoutes());
        assertTrue(flowRoute.getChildRoutes().isEmpty());
        assertNotNull(flowRoute.getFlowElements());
        assertTrue(flowRoute.getFlowElements().isEmpty());
    }

    @Test
    public void test_null_flow_exception() {
        StudioBuildException exception = assertThrows(StudioBuildException.class, () -> new FlowRoute.FlowRouteBuilder().build());
        assertEquals("Flow can not be null", exception.getMessage());
    }

    @Test
    public void test_isEmpty_returns_true_if_childRoutes_exist_but_are_empty_and_top_level_route_is_empty() throws StudioBuildException {
        FlowRoute childFlowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(Collections.singletonList(childFlowRoute))
                .build();

        boolean result = flowRoute.isEmpty();

        assertTrue(result);
    }

    @Test
    public void test_isEmpty_returns_false_if_childRoutes_exist_and_is_not_empty_and_top_level_route_is_empty() throws StudioBuildException {
        FlowRoute childFlowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(Collections.singletonList(TestFixtures.getBroker(BASE_META_PACK)))
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(Collections.singletonList(childFlowRoute))
                .build();

        boolean result = flowRoute.isEmpty();

        assertFalse(result);
    }
        @Test
    public void test_isEmpty_returns_false_if_childRoutes_empty_but_top_level_route_is_not_empty() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
            .flow(testFlow)
            .flowElements(Collections.singletonList(TestFixtures.getBroker(BASE_META_PACK)))
            .build();

        boolean result = flowRoute.isEmpty();

        assertFalse(result);
    }

    @Test
    public void test_findRouteOfName_returns_child_route_with_matching_routeName() throws StudioBuildException {
        List<FlowRoute> childRoutes = new ArrayList<>();
        FlowRoute childRoute1 = FlowRoute.flowRouteBuilder().routeName("route1").flow(testFlow).build();
        FlowRoute childRoute2 = FlowRoute.flowRouteBuilder().routeName("route2").flow(testFlow).build();
        childRoutes.add(childRoute1);
        childRoutes.add(childRoute2);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(childRoutes)
                .flowElements(null)
                .build();

        FlowRoute result = flowRoute.findRouteOfName("route1");
  
        assertEquals(childRoute1, result);
    }

    @Test
    public void test_findRouteOfName_returns_null_if_no_child_route_with_matching_routeName_exists() throws StudioBuildException {
        FlowRoute childRoute1 = FlowRoute.flowRouteBuilder().routeName("route1").flow(testFlow).build();
        FlowRoute childRoute2 = FlowRoute.flowRouteBuilder().routeName("route2").flow(testFlow).build();
        List<FlowRoute> childRoutes = new ArrayList<>();
        childRoutes.add(childRoute1);
        childRoutes.add(childRoute2);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .childRoutes(childRoutes)
                .flow(testFlow)
                .build();

        FlowRoute result = flowRoute.findRouteOfName("route3");

        assertNull(result);
    }

    @Test
    public void test_findRouteOfName_returns_null_if_routeName_is_null() throws StudioBuildException {
        FlowRoute childRoute1 = FlowRoute.flowRouteBuilder().routeName("route1").flow(testFlow).build();
        FlowRoute childRoute2 = FlowRoute.flowRouteBuilder().routeName("route2").flow(testFlow).build();
        List<FlowRoute> childRoutes = new ArrayList<>();
        childRoutes.add(childRoute1);
        childRoutes.add(childRoute2);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(childRoutes)
                .build();

        FlowRoute result = flowRoute.findRouteOfName(null);
  
        assertNull(result);
    }

    @Test
    public void test_removeFlowElement_where_we_remove_flow_element_from_flow_elements_list() throws StudioBuildException {
        FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(broker);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        flowRoute.removeFlowElement(broker);

        assertTrue(flowRoute.getFlowElements().isEmpty());
    }

    @Test
    public void test_removeFlowElement_where_we_remove_child_route_if_no_flow_elements_and_router() throws StudioBuildException {
        FlowElement router = TestFixtures.getMultiRecipientRouter(BASE_META_PACK);
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(router);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flowElements(flowElements)
                .flow(testFlow)
                .build();

        flowRoute.removeFlowElement(router);

        assertTrue(flowRoute.getChildRoutes().isEmpty());
    }

    @Test
    public void test_hasProducer_returns_true_when_producer_exists() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(Collections.singletonList(TestFixtures.getLoggingProducer(BASE_META_PACK)))
                .build();

        boolean result = flowRoute.hasProducer();

        assertTrue(result);
    }

    @Test
    public void test_hasProducer_returns_false_when_no_producer_exists() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        boolean result = flowRoute.hasProducer();

        assertFalse(result);
    }

    @Test
    public void test_hasRouter_returns_true_when_producer_exists() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(Collections.singletonList(TestFixtures.getMultiRecipientRouter(BASE_META_PACK)))
                .build();

        boolean result = flowRoute.hasRouter();

        assertTrue(result);
    }

    @Test
    public void test_hasRouter_returns_false_when_no_producer_exists() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        boolean result = flowRoute.hasRouter();

        assertFalse(result);
    }

    @Test
    public void test_getFlowIntegrityStatus_returns_true_when_meets_criteria() throws StudioBuildException {
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        testFlow.setConsumer(TestFixtures.getEventGeneratingConsumer(BASE_META_PACK));
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(producer);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        String result = flowRoute.getFlowIntegrityStatus();

        assertNotNull(result);
        assertTrue(result.isBlank());
    }

    @Test
    public void test_returns_all_flow_elements_including_consumer_in_default_route() throws StudioBuildException {
        // Create a FlowRoute object
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(broker);
        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK)
                .consumer(consumer)
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(flow)
                .flowElements(flowElements)
                .build();

        List<FlowElement> result = flowRoute.getConsumerAndFlowRouteElements();

        assertTrue(result.contains(consumer));
        assertTrue(result.contains(broker));
    }

    @Test
    public void test_getFlowElementsNoExternalEndPoints_returns_empty_list_even_when_not_flow_elements() throws StudioBuildException {
        FlowElement endPoint = TestFixtures.getEndpointForLocalFileConsumer(BASE_META_PACK);
        FlowElement localFileConsumer = TestFixtures.getLocalFileConsumer(BASE_META_PACK);
        FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(endPoint);
        flowElements.add(localFileConsumer);
        flowElements.add(broker);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        List<FlowElement> result = flowRoute.getFlowElementsNoExternalEndPoints();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(broker, result.getFirst());
    }

    @Test
    public void test_ftlGetConsumerAndFlowElementsNoEndPoints_returns_empty_list_even_when_not_flow_elements() throws StudioBuildException {
        FlowElement endPoint = TestFixtures.getEndpointForLocalFileConsumer(BASE_META_PACK);
        FlowElement localFileConsumer = TestFixtures.getLocalFileConsumer(BASE_META_PACK);
        FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(endPoint);
        flowElements.add(localFileConsumer);
        flowElements.add(broker);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        List<FlowElement> result = flowRoute.ftlGetConsumerAndFlowElementsNoEndPoints();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(localFileConsumer, result.get(0));
        assertEquals(broker, result.get(1));
    }

    @Test
    public void test_isValidToAdd_returns_true_when_adding_producer_and_no_existing_producer() throws StudioBuildException {
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        boolean result = flowRoute.isValidToAdd(producer.getComponentMeta());

        assertTrue(result);
    }

    @Test
    public void test_isValidToAdd_returns_true_when_adding_consumer_and_no_existing_consumer() throws StudioBuildException {
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        boolean result = flowRoute.isValidToAdd(consumer.getComponentMeta());

        assertTrue(result);
    }

    @Test
    public void test_isValidToAdd_returns_false_when_adding_producer_and_existing_producer() throws StudioBuildException {
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(Collections.singletonList(producer))
                .build();

        boolean result = flowRoute.isValidToAdd(producer.getComponentMeta());

        assertFalse(result);
    }

    @Test
    public void test_isValidToAdd_returns_false_when_adding_consumer_and_existing_consumer() throws StudioBuildException {
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK)
                .consumer(consumer)
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(flow)
                .build();

        boolean result = flowRoute.isValidToAdd(consumer.getComponentMeta());

        assertFalse(result);
    }

    @Test
    public void test_isValidToAdd_returns_true_when_adding_a_broker_where_consumer_and_producer_exist() throws StudioBuildException {
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK)
                .consumer(consumer)
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(flow)
                .flowElements(Collections.singletonList(producer))
                .build();

        boolean result = flowRoute.isValidToAdd(TestFixtures.getBroker(BASE_META_PACK).getComponentMeta());

        assertTrue(result);
    }

    @Test
    public void test_isValidToAdd_returns_true_when_adding_a_broker_where_no_consumer_and_no_producer_exist() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .build();

        boolean result = flowRoute.isValidToAdd(TestFixtures.getBroker(BASE_META_PACK).getComponentMeta());

        assertTrue(result);
    }
}