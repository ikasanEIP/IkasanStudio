package org.ikasan.studio.ui.viewmodel;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.StudioUIUtils;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class CanvasCachedLayoutHeavyTest extends HeavyPlatformTestCase {
    static {
        try { TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    public void testDefaultRouterBranchStaysInsideItsFlowAndClearOfTheNextFlow() throws Exception {
        var context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        var branched = TestFixtures.getEventGeneratingConsumerRouterFlow(TestFixtures.BASE_META_PACK);
        branched.getFlowRoute().getChildRoutes().get(1).setRouteName("default");
        var router = branched.getFlowRoute().getFlowElements().get(1);
        var endpointMeta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(
                TestFixtures.BASE_META_PACK, router.getComponentMeta().getEndpointKey());
        for (var route : branched.getFlowRoute().getChildRoutes()) {
            var endpoint = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(
                    TestFixtures.BASE_META_PACK, endpointMeta, branched, route, route.getRouteName());
            route.getFlowElements().add(0, endpoint);
        }
        var next = TestFixtures.getEventGeneratingConsumerSingleRecipientRouterFlow(TestFixtures.BASE_META_PACK);
        var module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(branched, next));
        context.setIkasanModule(module);
        var graphics = new BufferedImage(1800, 1200, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            var handler = (IkasanModuleViewHandler) ViewHandlerCache.getAbstractViewHandler(myProject, module);
            handler.initialiseDimensions(graphics, 0, 0, 1800, 1200);
            var box = ViewHandlerCache.getFlowViewHandler(myProject, branched);
            var consumer = ViewHandlerCache.getFlowComponentViewHandler(myProject, branched.getConsumer());
            assertTrue("Consumer must stay at the start of its flow", consumer.getLeftX() <
                    ViewHandlerCache.getFlowComponentViewHandler(myProject, branched.getFlowRoute().getFlowElements().get(0)).getLeftX());
            var routes = box.getFlowRouteViewHandler().getAllFlowRouteViewHandlers(new ArrayList<>(), box.getFlowRouteViewHandler());
            for (var route : routes) {
                for (var element : route.getFlowRoute().getConsumerAndFlowRouteElements()) {
                    var view = ViewHandlerCache.getFlowComponentViewHandler(myProject, element);
                    assertTrue("Component outside flow horizontally", view.getLeftX() >= box.getLeftX() && view.getRightX() <= box.getRightX());
                    assertTrue("Component outside flow vertically", view.getTopY() >= box.getTopY() && view.getBottomY() <= box.getBottomY());
                }
            }
            for (var route : branched.getFlowRoute().getChildRoutes()) {
                var endpoint = ViewHandlerCache.getFlowComponentViewHandler(myProject, route.getFlowElements().get(0));
                var nextComponent = ViewHandlerCache.getFlowComponentViewHandler(myProject, route.getFlowElements().get(1));
                int labelOverhang = Math.max(0, (IkasanFlowComponentViewHandler.FLOWCHART_SYMBOL_DEFAULT_WIDTH - endpoint.getWidth() + 1) / 2);
                assertTrue("Branch label must have leading clearance", endpoint.getRawLeadingGap() >= labelOverhang);
                assertTrue("Branch label must clear next component", nextComponent.getLeftX() > endpoint.getRightX() + labelOverhang);
            }
            assertTrue("Next flow must be below all branches", ViewHandlerCache.getFlowViewHandler(myProject, next).getTopY() > box.getBottomY());
        } finally { graphics.dispose(); context.getViewHandlerFactory().clear(); }
    }

    public void testClippedCachedPaintingMatchesFullPaintingAtTopAndAfterScroll() throws Exception {
        var context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        List<Flow> flows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            var consumer = TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
            var flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("Flow " + i).consumer(consumer).build();
            consumer.setContainingFlow(flow);
            var producer = TestFixtures.getLoggingProducer(TestFixtures.BASE_META_PACK);
            producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().setFlowElements(new ArrayList<>(List.of(producer)));
            flows.add(flow);
        }
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, flows);
        context.setIkasanModule(module);
        var handler = (IkasanModuleViewHandler) ViewHandlerCache.getAbstractViewHandler(myProject, module);
        var canvas = new JPanel(); canvas.setSize(1200, 700);
        try {
            for (int scroll : new int[] {0, 1100}) {
                var expected = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
                var actual = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
                var referenceGraphics = expected.createGraphics(); var cachedGraphics = actual.createGraphics();
                try {
                    var layoutGraphics = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB).createGraphics();
                    try { handler.initialiseDimensions(layoutGraphics, 0, 0, 1200, 700); }
                    finally { layoutGraphics.dispose(); }
                    referenceGraphics.translate(0, -scroll); referenceGraphics.setClip(0, scroll, 1200, 700);
                    cachedGraphics.translate(0, -scroll); cachedGraphics.setClip(0, scroll, 1200, 700);
                    StudioUIUtils.drawStringLeftAlignedFromTopLeft(referenceGraphics, handler.getText(), 10, 10, StudioUIUtils.getBoldFont());
                    for (Flow flow : flows) {
                        var flowHandler = ViewHandlerCache.getFlowViewHandler(myProject, flow);
                        flowHandler.paintComponent(canvas, referenceGraphics, -1, flowHandler.getTopY());
                    }
                    handler.paintComponent(canvas, cachedGraphics, -1, -1);
                    assertTrue("Paint differs at scroll " + scroll, java.util.Arrays.equals(
                            expected.getRGB(0, 0, 1200, 700, null, 0, 1200), actual.getRGB(0, 0, 1200, 700, null, 0, 1200)));
                } finally { referenceGraphics.dispose(); cachedGraphics.dispose(); }
            }
        } finally { context.getViewHandlerFactory().clear(); }
    }
}
