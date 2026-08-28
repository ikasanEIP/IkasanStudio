package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioComparitors;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
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

    /**
     * Regression test: a router-terminated route's own flowElements never include a producer - that's always
     * on one of its childRoutes instead - so checking hasProducer() on the router's own containing route (as
     * well as recursing into the children) meant a fully valid router flow was always wrongly reported as
     * "needs a producer", even when every branch already had one.
     */
    @Test
    public void test_getFlowIntegrityStatus_returns_true_when_every_router_branch_has_a_producer() throws StudioBuildException {
        testFlow.setConsumer(TestFixtures.getEventGeneratingConsumer(BASE_META_PACK));
        FlowElement router = TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(testFlow)
                .flowElements(new ArrayList<>(List.of(router)))
                .build();
        router.setContainingFlowRoute(rootRoute);

        FlowRoute route1 = FlowRoute.flowRouteBuilder().flow(testFlow).routeName("route1")
                .flowElements(new ArrayList<>(List.of(TestFixtures.getLoggingProducer(BASE_META_PACK)))).build();
        FlowRoute route2 = FlowRoute.flowRouteBuilder().flow(testFlow).routeName("route2")
                .flowElements(new ArrayList<>(List.of(TestFixtures.getLoggingProducer(BASE_META_PACK)))).build();
        rootRoute.getChildRoutes().add(route1);
        rootRoute.getChildRoutes().add(route2);

        assertThat(rootRoute.getFlowIntegrityStatus()).isBlank();
    }

    /**
     * Companion to the above: a router branch that is still missing its own producer must still be reported,
     * even though the router-terminated root route itself no longer performs that check directly.
     */
    @Test
    public void test_getFlowIntegrityStatus_stillReportsAMissingProducerOnOneRouterBranch() throws StudioBuildException {
        testFlow.setConsumer(TestFixtures.getEventGeneratingConsumer(BASE_META_PACK));
        FlowElement router = TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(testFlow)
                .flowElements(new ArrayList<>(List.of(router)))
                .build();
        router.setContainingFlowRoute(rootRoute);

        FlowRoute route1 = FlowRoute.flowRouteBuilder().flow(testFlow).routeName("route1")
                .flowElements(new ArrayList<>(List.of(TestFixtures.getLoggingProducer(BASE_META_PACK)))).build();
        FlowRoute route2 = FlowRoute.flowRouteBuilder().flow(testFlow).routeName("route2").build();
        rootRoute.getChildRoutes().add(route1);
        rootRoute.getChildRoutes().add(route2);

        assertThat(rootRoute.getFlowIntegrityStatus()).contains("The flow needs a producer.");
    }

    /**
     * Regression test: PIPSIIkasanModel#generateAndSaveUserImplementClassStubsForFlow used to iterate
     * FlowRoute#getConsumerAndFlowRouteElements(), which only returns the ROOT route's own elements - for a
     * router flow, that's the consumer and whatever leads up to the router itself, never anything inside a
     * branch. That meant a Debug (or any other user-implemented component, e.g. Broker/Converter) dropped
     * into a router branch never got its stub class generated. The fix switched to
     * Flow#getFlowElementsNoExternalEndPoints(), which this test confirms actually walks every route
     * recursively (see Flow#getAllFlowElementsInAnyRoute) - a Debug placed inside a branch must be present.
     */
    @Test
    public void test_getFlowElementsNoExternalEndPoints_includesElementsInsideRouterBranches() throws StudioBuildException {
        testFlow.setConsumer(TestFixtures.getEventGeneratingConsumer(BASE_META_PACK));
        FlowElement router = TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(testFlow)
                .flowElements(new ArrayList<>(List.of(router)))
                .build();
        router.setContainingFlowRoute(rootRoute);

        FlowElement route1Debug = TestFixtures.getDebugTransition(BASE_META_PACK);
        FlowElement route1Producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        FlowRoute route1 = FlowRoute.flowRouteBuilder().flow(testFlow).routeName("route1")
                .flowElements(new ArrayList<>(List.of(route1Debug, route1Producer))).build();
        rootRoute.getChildRoutes().add(route1);
        testFlow.setFlowRoute(rootRoute);

        assertThat(testFlow.getFlowElementsNoExternalEndPoints())
                .contains(router, route1Debug, route1Producer);
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
        assertEquals(broker, result.get(0));
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

    @Test
    public void test_cloneToVersion_clones_route_and_elements() throws StudioBuildException {
        FlowElement xProducerComponent = TestFixtures.getXProducerComponent("TestV1");
        Flow newFlow = TestFixtures.getUnbuiltFlow("TestV1").build();
        FlowRoute originalFlowRoute = new FlowRoute.FlowRouteBuilder()
                .flow(testFlow)
                .routeName("route1")
                .flowElements(Collections.singletonList(xProducerComponent))
                .build();
        originalFlowRoute.setFlowElements(Collections.singletonList(xProducerComponent));

        FlowRoute clonedFlowRoute = originalFlowRoute.cloneToVersion("TestV2", newFlow);

        assertNotNull(clonedFlowRoute);
        // ignore anything thats mocked.

        FlowElement clonedXProducerComponent = clonedFlowRoute.getFlowElements().get(0);
        // *** COMPONENT META ***
        // Should be same except for jar dependencies
        assertEquals("3.1.0", ((Dependency) xProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
        assertEquals("3.2.0", ((Dependency) clonedXProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
        // same except for the above

        assertThat(clonedXProducerComponent.getComponentMeta())
                .usingRecursiveComparison()
                .withEqualsForType(StudioComparitors::imageIconsEqual, Icon.class)
                .ignoringFields(
                        "jarDependencies",              // These are set in test fixture as different
                        "allowableProperties")          // These are different, the differences being tested in Component Properties below, all allowables are used for componentProperties.
                .isEqualTo(xProducerComponent.getComponentMeta());

        ComponentProperty originalSimpleStringProperty = xProducerComponent.getProperty(TestFixtures.SIMPLE_STRING_PROPERTY);
        ComponentProperty clonedSimpleStringProperty = clonedXProducerComponent.getProperty(TestFixtures.SIMPLE_STRING_PROPERTY);
        // *** COMPONENT PROPERTIES ***
        // For the property named SIMPLE_STRING_PROPERTY the meta should be the same except for tUserImplementClassFtlTemplate, SetterMethod, Validatio
        assertEquals("org/ikasan/spec/component/endpoint/Producer.ftl", originalSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
        assertEquals("org/ikasan/spec/component/endpoint/ProducerV2.ftl", clonedSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
        assertEquals("setCronExpression", originalSimpleStringProperty.getMeta().getSetterMethod());
        assertEquals("setCronExpression2", clonedSimpleStringProperty.getMeta().getSetterMethod());
        assertEquals("^v1[A-Z_$][a-zA-Z\\d_$£]*$", originalSimpleStringProperty.getMeta().getValidation());
        assertEquals("^v2[A-Z_$][a-zA-Z\\d_$£]*$", clonedSimpleStringProperty.getMeta().getValidation());
        // same except for the above
        assertThat(clonedSimpleStringProperty.getMeta())
                .usingRecursiveComparison()
                .ignoringFields("cronExpression", "validation", "validationPattern", "userImplementClassFtlTemplate", "setterMethod")
                .isEqualTo(originalSimpleStringProperty.getMeta());
        // expected to be the same

        assertEquals(xProducerComponent.getComponentProperties().entrySet().toArray()[0], clonedXProducerComponent.getComponentProperties().entrySet().toArray()[0]);

        assertThat(clonedXProducerComponent.getComponentProperties())
                .usingRecursiveComparison()
                .ignoringFields(TestFixtures.SIMPLE_STRING_PROPERTY)
                .withComparatorForType(
                        Comparator.nullsFirst(
                                Comparator.comparing(Pattern::pattern)), Pattern.class)
                .isEqualTo(xProducerComponent.getComponentProperties());
    }

    /**
     * Regression test: a router's childRoutes previously only ever got built from its routeNames property
     * during a full model.json reload (ModuleDeserializer#addNewRoutesForRouter) - a live drag-and-drop add
     * or property edit never created them, so a freshly added router had no branches to drop a Producer into,
     * and the canvas wrongly reported "cannot have a router AND a producer" against the router's own
     * containing route. syncChildRoutesForRouter is the live-editing equivalent of that reload-time logic.
     */
    @Test
    public void test_syncChildRoutesForRouter_createsAChildRouteWithRouterEndpointForEachRouteName() throws StudioBuildException {
        FlowElement router = TestFixtures.getMultiRecipientRouter(BASE_META_PACK);
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(testFlow)
                .flowElements(new ArrayList<>(List.of(router)))
                .build();
        router.setContainingFlowRoute(rootRoute);
        assertThat(rootRoute.getChildRoutes()).isEmpty();

        rootRoute.syncChildRoutesForRouter(BASE_META_PACK, router);

        assertThat(rootRoute.getChildRoutes()).hasSize(2);
        assertThat(rootRoute.getChildRoutes().stream().map(FlowRoute::getRouteName).toList())
                .containsExactlyInAnyOrder("route1", "route2");
        for (FlowRoute childRoute : rootRoute.getChildRoutes()) {
            assertThat(childRoute.getFlowElements()).hasSize(1);
            assertThat(childRoute.getFlowElements().get(0).getComponentMeta().isInternalEndpoint()).isTrue();
        }

        // A route that was subsequently added to routeNames (e.g. a live edit) gets a new child route...
        router.setPropertyValue(ComponentPropertyMeta.ROUTE_NAMES, List.of("route1", "route2", "route3"));
        rootRoute.syncChildRoutesForRouter(BASE_META_PACK, router);
        assertThat(rootRoute.getChildRoutes().stream().map(FlowRoute::getRouteName).toList())
                .containsExactlyInAnyOrder("route1", "route2", "route3");

        // ...but existing routes (and anything already placed inside them) are left untouched.
        FlowRoute route1 = rootRoute.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowElement producer1 = TestFixtures.getDevNullProducer(BASE_META_PACK);
        route1.getFlowElements().add(producer1);
        rootRoute.syncChildRoutesForRouter(BASE_META_PACK, router);
        assertThat(route1.getFlowElements()).contains(producer1);
    }
}