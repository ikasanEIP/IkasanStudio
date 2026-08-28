package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.ImageUtil;
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
 * Regression test: a router must always be the LAST element of the FlowRoute it lives in (see the comment in
 * ModuleDeserializer#buildRouteTree: "the router always marks the end of a route") - both rendering
 * (IkasanFlowRouteViewHandler) and code generation assume this. Dropping a router in the MIDDLE of an existing
 * chain (e.g. just before an already-placed Producer) previously only spliced the router into the flat list at
 * the drop point, leaving the Producer sitting after it in that SAME list - the Producer looked "hard
 * connected" to the router, and the router's own new branches appeared to hang off the Producer's right side
 * instead of the router's. The fix (DesignerCanvas#moveTrailingElementsIntoRoutersFirstBranch) relocates
 * whatever already followed the router into its own first new branch instead.
 */
public class RouterInsertedBeforeExistingProducerHarnessTest extends ComponentTestHarness {

    @Test
    void routerDroppedJustBeforeAnExistingProducerAutoAttachesItToTheFirstBranch() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;

        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(metapackVersion);
        FlowElement producer = TestFixtures.getDevNullProducer(metapackVersion);
        producer.setComponentName("lp2");

        Flow flow = TestFixtures.getUnbuiltFlow(metapackVersion).consumer(consumer).build();
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(producer)))
                .build();
        producer.setContainingFlowRoute(rootRoute);
        flow.setFlowRoute(rootRoute);

        Module module = TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);

        DesignerCanvas designerCanvas = new DesignerCanvas(project);

        BufferedImage img = ImageUtil.createImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            flowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            IkasanFlowComponentViewHandler producerVH = ViewHandlerCache.getFlowComponentViewHandler(project, producer);
            Point producerCentre = producerVH.getCentrePoint();
            // A few pixels left of the producer's own centre - close enough to be its only proximity match,
            // resolving it as the RIGHT neighbour (see Proximity#getRelativeProximity), exactly like dropping
            // "just before" the producer would.
            Point dropPoint = new Point(producerCentre.x - 3, producerCentre.y);

            // Pre-populated (no unset mandatory properties), so the real production path's "Add new component"
            // popup dialog never triggers - this test exercises exactly what that path does AFTER the popup is
            // dismissed (insertNewComponentBetweenSurroundingPair, then the router-branch sync it was missing).
            FlowElement router = TestFixtures.getSingleRecipientRouter(metapackVersion);
            router.setContainingFlowRoute(rootRoute);

            Method insertMethod = DesignerCanvas.class.getDeclaredMethod(
                    "insertNewComponentBetweenSurroundingPair", Flow.class, FlowRoute.class, FlowElement.class, int.class, int.class);
            insertMethod.setAccessible(true);
            insertMethod.invoke(designerCanvas, flow, rootRoute, router, dropPoint.x, dropPoint.y);

            Method syncMethod = DesignerCanvas.class.getDeclaredMethod("syncChildRoutesForRouter", FlowElement.class);
            syncMethod.setAccessible(true);
            syncMethod.invoke(designerCanvas, router);

            assertThat(rootRoute.getFlowElements())
                    .as("The router must end up as the LAST (only) element of its own route")
                    .containsExactly(router);

            assertThat(rootRoute.getChildRoutes()).hasSize(2);
            FlowRoute route1 = rootRoute.getChildRoutes().stream().filter(r -> "route1".equals(r.getRouteName())).findFirst().orElseThrow();
            FlowRoute route2 = rootRoute.getChildRoutes().stream().filter(r -> "route2".equals(r.getRouteName())).findFirst().orElseThrow();

            assertThat(route1.getFlowElements()).hasSize(2);
            assertThat(route1.getFlowElements().get(0).getComponentMeta().isInternalEndpoint()).isTrue();
            assertThat(route1.getFlowElements().get(1))
                    .as("The producer that was already there must auto-attach to the router's first route")
                    .isSameAs(producer);
            assertThat(producer.getContainingFlowRoute()).isSameAs(route1);

            assertThat(route2.getFlowElements())
                    .as("The second route must be left dangling (just its endpoint) for the user to complete")
                    .hasSize(1);
            assertThat(route2.getFlowElements().get(0).getComponentMeta().isInternalEndpoint()).isTrue();
        } finally {
            g.dispose();
        }
    }
}
