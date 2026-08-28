package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ikasan core scopes a router's route names to that router instance only (see FlowBuilder#connectElements,
 * which builds a fresh transitions map per router, and each router's own FlowElementImpl#getTransition lookup) -
 * two different routers in the same flow, one nested inside a branch of the other, can legitimately both have a
 * route called "route1". Studio's own uniqueness checks (Debug auto-naming, and component-name validation) must
 * still end up flow-wide unique despite that, since they scope by FULL identity/name, not by which router a
 * route name came from. These tests build exactly that shape - a router nested inside one branch of another
 * router, both with a "route1" branch - and confirm both checks reach all the way into the nested branch.
 */
public class NestedRouterUniquenessHarnessTest extends ComponentTestHarness {

    /**
     * Builds: consumer -> routerA (route1, route2)
     *   route1 -> routerB (route1, route2)   [routerB is NESTED inside routerA's own "route1" branch]
     *     route1 -> (empty, for the test to populate)
     *     route2 -> (empty, for the test to populate)
     *   route2 -> producer "My Shallow Producer"
     * routerA's "route1" endpoint and routerB's "route1" endpoint are two different FlowElements that both
     * legitimately have the identity "route1" - the crux of what these tests exercise.
     */
    private Flow buildFlowWithRouterNestedInsideAnotherRoutersRoute1(String metapackVersion) throws Exception {
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(metapackVersion);
        FlowElement routerA = TestFixtures.getSingleRecipientRouter(metapackVersion);

        Flow flow = TestFixtures.getUnbuiltFlow(metapackVersion).consumer(consumer).build();
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(routerA)))
                .build();
        routerA.setContainingFlowRoute(rootRoute);
        flow.setFlowRoute(rootRoute);
        rootRoute.syncChildRoutesForRouter(metapackVersion, routerA);

        FlowRoute routeA1 = rootRoute.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowRoute routeA2 = rootRoute.getChildRoutes().stream().filter(r -> "route2".equals(r.getRouteName())).findFirst().orElseThrow();

        FlowElement producerA2 = TestFixtures.getDevNullProducer(metapackVersion);
        producerA2.setComponentName("My Shallow Producer");
        producerA2.setContainingFlowRoute(routeA2);
        routeA2.getFlowElements().add(producerA2);

        FlowElement routerB = TestFixtures.getSingleRecipientRouter(metapackVersion);
        routerB.setComponentName("My Nested Router");
        routerB.setContainingFlowRoute(routeA1);
        routeA1.getFlowElements().add(routerB);
        routeA1.syncChildRoutesForRouter(metapackVersion, routerB);

        return flow;
    }

    /**
     * The route1 endpoint FlowElement (identity "route1") belonging to the given router's own route1 branch,
     * found by walking straight from the flow's root route (routerA's branch) or, if routerRoute is not the
     * root, into its own children (used to reach routerB's route1, nested inside routeA1).
     */
    private FlowElement findRoute1Endpoint(FlowRoute containingRoute) {
        FlowRoute route1 = containingRoute.getChildRoutes().stream()
                .filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        return route1.getFlowElements().get(0);
    }

    @Test
    void debugNamesStayUniqueAcrossTwoDifferentRoutersThatBothHaveARoute1Branch() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;
        Flow flow = buildFlowWithRouterNestedInsideAnotherRoutersRoute1(metapackVersion);

        FlowRoute rootRoute = flow.getFlowRoute();
        FlowElement routerA = rootRoute.getFlowElements().get(0);
        FlowRoute routeA1 = rootRoute.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowElement routerB = routeA1.getFlowElements().stream().filter(e -> e.getComponentMeta().isRouter()).findFirst().orElseThrow();
        FlowRoute routeB1 = routeA1.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();

        FlowElement outerRoute1Endpoint = findRoute1Endpoint(rootRoute);
        FlowElement nestedRoute1Endpoint = findRoute1Endpoint(routeA1);
        assertThat(outerRoute1Endpoint.getIdentity()).isEqualTo("route1");
        assertThat(nestedRoute1Endpoint.getIdentity()).isEqualTo("route1");
        assertThat(outerRoute1Endpoint).isNotSameAs(nestedRoute1Endpoint);

        Module module = TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);

        DesignerCanvas designerCanvas = new DesignerCanvas(project);
        Method assignMethod = DesignerCanvas.class.getDeclaredMethod(
                "assignDebugIdentityAndClassName", FlowElement.class, String.class, Flow.class);
        assignMethod.setAccessible(true);

        FlowElement debugOnOuterRoute1 = TestFixtures.getDebugTransition(metapackVersion);
        assignMethod.invoke(designerCanvas, debugOnOuterRoute1, outerRoute1Endpoint.getIdentity(), flow);
        // Place it in the model, exactly as insertNewComponentBetweenSurroundingPair would, so the second
        // assignment below can actually find it during its flow-wide collision scan.
        debugOnOuterRoute1.setContainingFlowRoute(routeA1);
        routeA1.getFlowElements().add(1, debugOnOuterRoute1);

        FlowElement debugOnNestedRoute1 = TestFixtures.getDebugTransition(metapackVersion);
        assignMethod.invoke(designerCanvas, debugOnNestedRoute1, nestedRoute1Endpoint.getIdentity(), flow);
        debugOnNestedRoute1.setContainingFlowRoute(routeB1);
        routeB1.getFlowElements().add(debugOnNestedRoute1);

        assertThat(debugOnOuterRoute1.getIdentity()).isEqualTo("route1Debug");
        assertThat(debugOnNestedRoute1.getIdentity())
                .as("Both anchors are legitimately named 'route1' (one from routerA, one from the nested routerB) - "
                        + "the second Debug must not silently collide with the first")
                .isEqualTo("route1Debug2");
    }

    @Test
    void componentNameUniquenessCheckReachesIntoANestedRouterBranch() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;
        Flow flow = buildFlowWithRouterNestedInsideAnotherRoutersRoute1(metapackVersion);

        FlowRoute rootRoute = flow.getFlowRoute();
        FlowRoute routeA1 = rootRoute.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowElement routerB = routeA1.getFlowElements().stream().filter(e -> e.getComponentMeta().isRouter()).findFirst().orElseThrow();
        FlowRoute routeB1 = routeA1.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowRoute routeA2 = rootRoute.getChildRoutes().stream().filter(r -> "route2".equals(r.getRouteName())).findFirst().orElseThrow();

        // Buried two levels deep: inside routerB's own route1 branch, itself nested inside routerA's route1 branch.
        FlowElement deeplyNestedProducer = TestFixtures.getDevNullProducer(metapackVersion);
        deeplyNestedProducer.setComponentName("My Deeply Nested Producer");
        deeplyNestedProducer.setContainingFlowRoute(routeB1);
        routeB1.getFlowElements().add(deeplyNestedProducer);

        // A completely different, shallow branch (routerA's route2) - not nested under anything - given the
        // SAME name as the one buried inside the nested router's branch above.
        FlowElement shallowDuplicate = TestFixtures.getDevNullProducer(metapackVersion);
        shallowDuplicate.setComponentName("My Deeply Nested Producer");
        shallowDuplicate.setContainingFlowRoute(routeA2);
        routeA2.getFlowElements().add(shallowDuplicate);

        Module module = TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);

        ComponentPropertiesPanel panel = new ComponentPropertiesPanel(project, false);
        panel.updateTargetComponent(shallowDuplicate);

        Method validateMethod = ComponentPropertiesPanel.class.getDeclaredMethod("validateComponentNameIsUniqueInFlow");
        validateMethod.setAccessible(true);
        List<?> duplicateResult = (List<?>) validateMethod.invoke(panel);

        assertThat(duplicateResult)
                .as("A name shared with a component buried inside a NESTED router's branch must still be caught")
                .isNotEmpty();

        // Renaming it to something genuinely unique must clear the validation.
        shallowDuplicate.setComponentName("My Genuinely Unique Producer Name");
        panel.updateTargetComponent(shallowDuplicate);
        List<?> cleanResult = (List<?>) validateMethod.invoke(panel);
        assertThat(cleanResult).isEmpty();
    }
}
