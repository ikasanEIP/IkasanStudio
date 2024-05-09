package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlowRouteTest {
    Flow testFlow;

    // write before each method

    @BeforeEach
    public void setUp() throws StudioBuildException {
        testFlow = TestFixtures.getUnbuiltFlow().build();
    }


    @Test
    public void test_flow_route_instantiation() throws StudioBuildException {
        String routeName = "route1";
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = new ArrayList<>();

        FlowRoute flowRoute = new FlowRoute(testFlow, routeName, childRoutes, flowElements);

        assertEquals(testFlow, flowRoute.getFlow());
        assertEquals(routeName, flowRoute.getRouteName());
        assertEquals(childRoutes, flowRoute.getChildRoutes());
        assertEquals(flowElements, flowRoute.getFlowElements());
    }

    @Test
    public void test_default_route_name() throws StudioBuildException {
        String routeName = null;
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = new ArrayList<>();

        FlowRoute flowRoute = new FlowRoute(testFlow, routeName, childRoutes, flowElements);

        assertEquals(Transition.DEFAULT_TRANSITION_NAME, flowRoute.getRouteName());
    }

    @Test
    public void test_default_child_routes() throws StudioBuildException {
        String routeName = null;
        List<FlowRoute> childRoutes = null;
        List<FlowElement> flowElements = new ArrayList<>();

        FlowRoute flowRoute = new FlowRoute(testFlow, routeName, childRoutes, flowElements);

        assertNotNull(flowRoute.getChildRoutes());
        assertTrue(flowRoute.getChildRoutes().isEmpty());
    }

    @Test
    public void test_null_flow_exception() {
        Flow flow = null;
        String routeName = "route1";
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = new ArrayList<>();

        StudioBuildException exception = assertThrows(StudioBuildException.class, () -> new FlowRoute(flow, routeName, childRoutes, flowElements));
        assertEquals("Flow can not be null", exception.getMessage());
    }

    @Test
    public void test_default_flow_elements() throws StudioBuildException {
        String routeName = "route1";
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = null;

        FlowRoute flowRoute = new FlowRoute(testFlow, routeName, childRoutes, flowElements);

        assertNotNull(flowRoute.getFlowElements());
        assertTrue(flowRoute.getFlowElements().isEmpty());
    }

    @Test
    public void test_add_consumer_to_flow() throws StudioBuildException {
        String routeName = "route1";
        List<FlowRoute> childRoutes = new ArrayList<>();
        List<FlowElement> flowElements = new ArrayList<>();
        ComponentMeta consumerComponentMeta = ComponentMeta.builder()
                .componentShortType(ComponentMeta.COMSUMER_TYPE)
                .name("test")
                .implementingClass("implementngClass")
                .build();
        FlowElement consumerFlowElement = FlowElement.flowElementBuilder()
                .componentMeta(consumerComponentMeta)
                .build();
        flowElements.add(consumerFlowElement);

        FlowRoute flowRoute = new FlowRoute(testFlow, routeName, childRoutes, flowElements);

        assertEquals(consumerFlowElement, testFlow.getConsumer());
    }

    @Test
    public void test_returns_true_if_childRoutes_and_flowElements_are_both_null_or_empty() throws StudioBuildException {
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(null)
                .flowElements(null)
                .build();

        boolean result = flowRoute.isEmpty();

        assertTrue(result);
    }


    @Test
    public void test_returns_true_if_childRoutes_exist_bust_are_empty() throws StudioBuildException {
        FlowRoute childFlowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(null)
                .flowElements(null)
                .build();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .childRoutes(Collections.singletonList(childFlowRoute))
                .flowElements(null)
                .build();

        boolean result = flowRoute.isEmpty();

        assertTrue(result);
    }



    @Test
    public void test_returns_child_route_with_matching_routeName() throws StudioBuildException {
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
    public void test_returns_null_if_no_child_route_with_matching_routeName_exists() throws StudioBuildException {
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
    public void test_returns_null_if_routeName_is_null() throws StudioBuildException {
        FlowRoute childRoute1 = FlowRoute.flowRouteBuilder().routeName("route1").flow(testFlow).build();
        FlowRoute childRoute2 = FlowRoute.flowRouteBuilder().routeName("route2").flow(testFlow).build();
        List<FlowRoute> childRoutes = new ArrayList<>();
        childRoutes.add(childRoute1);
        childRoutes.add(childRoute2);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .childRoutes(childRoutes)
                .flow(testFlow)
                .build();


        FlowRoute result = flowRoute.findRouteOfName(null);

  
        assertNull(result);
    }

    // Removes a flow element from the flow route's flow elements list if it exists
    @Test
    public void test_remove_flow_element_from_flow_elements_list() throws StudioBuildException {
        FlowElement broker = TestFixtures.getBroker();
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(broker);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flowElements(flowElements)
                .flow(testFlow)
                .build();

        flowRoute.removeFlowElement(broker);

        assertTrue(flowRoute.getFlowElements().isEmpty());
    }

    // Remove a router flow element and observe the related child flow routes are removed.
    @Test
    public void test_remove_child_route_if_no_flow_elements_and_router() throws StudioBuildException {
        // test routes are route1, route2
        FlowElement router = TestFixtures.getMultiRecipientRouter();

        FlowRoute childFlowRoute1 = new FlowRoute.FlowRouteBuilder()
                .routeName("route1")
                .flow(testFlow)
                .build();
        FlowRoute childFlowRoute2 = new FlowRoute.FlowRouteBuilder()
                .routeName("route2")
                .flow(testFlow)
                .build();
        List<FlowRoute> childRoutes = new ArrayList<>();
        childRoutes.add(childFlowRoute1);
        childRoutes.add(childFlowRoute2);

        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(router);

        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flowElements(flowElements)
                .flow(testFlow)
                .build();

        flowRoute.removeFlowElement(router);

        assertTrue(flowRoute.getChildRoutes().isEmpty());
    }


        // Returns a list of FlowElements containing all FlowElements in the FlowRoute, including the consumer if present in the default route
        @Test
        public void test_returns_all_flow_elements_including_consumer_in_default_route() throws StudioBuildException {
            // Create a FlowRoute object
            FlowElement consumer = TestFixtures.getEventGeneratingConsumer();
            FlowElement broker = TestFixtures.getBroker();
            List<FlowElement> flowElements = new ArrayList<>();
            flowElements.add(broker);
            Flow flow = TestFixtures.getUnbuiltFlow()
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



    // Returns a list of FlowElements containing all FlowElements in the FlowRoute, including the consumer if present in the default route
    @Test
    public void test_hasProducer_returns_true_when_producer_exists() throws StudioBuildException {
        FlowElement producer = TestFixtures.getLoggingProducer();
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(producer);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        boolean result = flowRoute.hasProducer();

        assertTrue(result);
    }

    @Test
    public void test_hasProducer_returns_false_when_no_producer_exists() throws StudioBuildException {
        List<FlowElement> flowElements = new ArrayList<>();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        boolean result = flowRoute.hasProducer();

        assertFalse(result);
    }

    // Returns a list of FlowElements containing all FlowElements in the FlowRoute, including the consumer if present in the default route
    @Test
    public void test_hasRouter_returns_true_when_producer_exists() throws StudioBuildException {
        FlowElement router = TestFixtures.getMultiRecipientRouter();
        List<FlowElement> flowElements = new ArrayList<>();
        flowElements.add(router);
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        boolean result = flowRoute.hasRouter();

        assertTrue(result);
    }

    @Test
    public void test_hasRouter_returns_false_when_no_producer_exists() throws StudioBuildException {
        List<FlowElement> flowElements = new ArrayList<>();
        FlowRoute flowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .flowElements(flowElements)
                .build();

        boolean result = flowRoute.hasRouter();

        assertFalse(result);
    }


}