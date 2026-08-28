package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the "line goes to the top-left corner (0,0)" bug: a router's childRoutes used to only
 * ever get built once, when the Flow's IkasanFlowRouteViewHandler tree was first constructed. If a childRoute
 * was added to the model afterwards (e.g. via FlowRoute#syncChildRoutesForRouter, when a router is dropped or
 * its routeNames edited live), that branch's elements never got an initialiseDimensions pass, so they kept
 * their default (0,0) position.
 */
public class RouterChildRouteLayoutHarnessTest extends ComponentTestHarness {

    @Test
    void routerBranchAddedAfterFlowViewHandlerConstructionStillGetsLaidOut() throws Exception {
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

        BufferedImage img = com.intellij.util.ui.ImageUtil.createImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // First paint pass, matching what happens when a router is dropped with no routes populated yet -
            // this is what constructs and caches the Flow's IkasanFlowRouteViewHandler tree, with zero children.
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            flowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            // Now simulate what FlowRoute#syncChildRoutesForRouter does: a branch is added to the model AFTER
            // the view handler tree already exists (and was already cached on the live Flow object).
            rootRoute.syncChildRoutesForRouter(metapackVersion, router);
            assertThat(rootRoute.getChildRoutes()).hasSize(2);
            FlowElement endpoint = rootRoute.getChildRoutes().get(0).getFlowElements().get(0);

            // Re-paint using the SAME (cached) flow view handler, exactly as a live repaint after a property
            // edit does - it does not get rebuilt from scratch.
            IkasanFlowViewHandler sameFlowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            sameFlowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            IkasanFlowComponentViewHandler endpointVH = ViewHandlerCache.getFlowComponentViewHandler(project, endpoint);

            assertThat(endpointVH.getLeftX()).as("Router Endpoint leftX should be laid out, not left at its default 0").isNotZero();
            assertThat(endpointVH.getTopY()).as("Router Endpoint topY should be laid out, not left at its default 0").isNotZero();
        } finally {
            g.dispose();
        }
    }
}
