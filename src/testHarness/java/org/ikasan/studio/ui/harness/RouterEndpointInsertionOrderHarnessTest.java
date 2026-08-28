package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: a Router Endpoint (the "ball") must always stay the first element of the branch it
 * anchors - nothing may ever be inserted before it via a live drag-and-drop. Only ModuleDeserializer's full
 * model.json reload guaranteed this (it always builds a branch with its endpoint first); a live drop resolved
 * as landing to the LEFT of a freshly-created branch's endpoint (an easy misjudgement when the endpoint sits
 * right next to the router, before anything else has been added to that branch) used to insert the dropped
 * component before the endpoint, rendering the producer box ahead of the connector ball - reported as "the
 * line goes into the producer, and the ball appears after it" - self-healing only on IDE restart.
 */
public class RouterEndpointInsertionOrderHarnessTest extends ComponentTestHarness {

    @Test
    void componentDroppedLeftOfARouteEndpointStillLandsAfterIt() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;

        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(metapackVersion);
        FlowElement router = TestFixtures.getSingleRecipientRouter(metapackVersion);

        Flow flow = TestFixtures.getUnbuiltFlow(metapackVersion).consumer(consumer).build();
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(router)))
                .build();
        router.setContainingFlowRoute(rootRoute);
        flow.setFlowRoute(rootRoute);

        Module module = TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));

        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);

        // Give the router its routes - this is what FlowRoute#syncChildRoutesForRouter does live, and what a
        // fresh drop of the router already goes through (see DesignerCanvas#requestToAddComponent). The fixture
        // router has two routeNames ("route1", "route2"); only route1 is exercised below.
        rootRoute.syncChildRoutesForRouter(metapackVersion, router);
        assertThat(rootRoute.getChildRoutes()).hasSize(2);
        FlowRoute route1 = rootRoute.getChildRoutes().stream()
                .filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
        FlowElement endpoint = route1.getFlowElements().get(0);
        assertThat(endpoint.getComponentMeta().isInternalEndpoint()).isTrue();

        BufferedImage img = com.intellij.util.ui.ImageUtil.createImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            flowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            IkasanFlowComponentViewHandler endpointVH = ViewHandlerCache.getFlowComponentViewHandler(project, endpoint);
            Point endpointCentre = endpointVH.getCentrePoint();

            FlowElement producer = TestFixtures.getDevNullProducer(metapackVersion);

            DesignerCanvas designerCanvas = new DesignerCanvas(project);
            Method insertMethod = DesignerCanvas.class.getDeclaredMethod(
                    "insertNewComponentBetweenSurroundingPair", Flow.class, FlowRoute.class, FlowElement.class, int.class, int.class);
            insertMethod.setAccessible(true);

            // Drop a few pixels to the LEFT of the endpoint's own centre - close enough to be its only proximity
            // match, but on the side that (before the fix) got misresolved as "endpoint is to the right of the
            // drop", inserting the producer before it.
            insertMethod.invoke(designerCanvas, flow, route1, producer, endpointCentre.x - 3, endpointCentre.y);

            assertThat(route1.getFlowElements())
                    .as("The Router Endpoint must stay first; the dropped producer must land after it")
                    .containsExactly(endpoint, producer);
        } finally {
            g.dispose();
        }
    }
}
