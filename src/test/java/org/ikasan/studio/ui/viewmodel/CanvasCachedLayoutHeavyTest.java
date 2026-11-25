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

import static org.junit.Assert.assertArrayEquals;
import java.awt.Graphics2D;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;

public class CanvasCachedLayoutHeavyTest extends HeavyPlatformTestCase {
    static {
        try { TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    public void testLeftFileServerCardClearsTransportControlsEvenWithSmallConfiguredOffsets() throws Exception {
        UiContext context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        FlowElement consumer = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).consumer(consumer).build();
        consumer.setContainingFlow(flow);
        FlowElement producer = TestFixtures.getLoggingProducer(TestFixtures.BASE_META_PACK);
        producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().setFlowElements(new ArrayList<>(List.of(producer)));
        context.setIkasanModule(TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow)));
        IkasanFlowViewHandler handler = ViewHandlerCache.getFlowViewHandler(myProject, flow);
        Graphics2D graphics = new BufferedImage(1600, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            for (int configured : new int[] {0, 150, 260, 400}) {
                int left = IkasanModuleViewHandler.flowXStartPoint(configured, true);
                handler.initialiseDimensions(graphics, left, 100, 1600, 600);
                handler.paintLaidOutComponent(new JPanel(), graphics);
                IkasanFlowComponentViewHandler endpoint = handler.getEndpointViewHandlerFor(consumer);
                // Actual server geometry: a 90 px card placed 20 px beyond the endpoint connector.
                int serverLeft = endpoint.getLeftConnectorPoint().x - 20 - 90;
                int statusRight = 24 + 14 + 6 + graphics.getFontMetrics(StudioUIUtils.getMainFont()).stringWidth("Recovering");
                assertTrue("Server card must clear the status text and hover area at offset " + configured,
                        serverLeft >= statusRight + 12);
                // Compactness is a layout constraint, independent of the host's font metrics.
                // Keep the font-based clearance check above to catch actual overlap.
                int baseline = Math.max(configured,
                        org.ikasan.studio.intellij.settings.IkasanStudioSettings.DEFAULT_FLOW_X_START_POINT);
                assertTrue("Harness should add at most 110 pixels to the flow offset", left <= baseline + 110);
                assertEquals("Idle layout preserves the configured offset", configured,
                        IkasanModuleViewHandler.flowXStartPoint(configured, false));
            }
        } finally { graphics.dispose(); context.getViewHandlerFactory().clear(); }
    }

    public void testDefaultRouterBranchStaysInsideItsFlowAndClearOfTheNextFlow() throws Exception {
        UiContext context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        Flow branched = TestFixtures.getEventGeneratingConsumerRouterFlow(TestFixtures.BASE_META_PACK);
        branched.getFlowRoute().getChildRoutes().get(1).setRouteName("default");
        FlowElement router = branched.getFlowRoute().getFlowElements().get(1);
        ComponentMeta endpointMeta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(
                TestFixtures.BASE_META_PACK, router.getComponentMeta().getEndpointKey());
        for (FlowRoute route : branched.getFlowRoute().getChildRoutes()) {
            FlowElement endpoint = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(
                    TestFixtures.BASE_META_PACK, endpointMeta, branched, route, route.getRouteName());
            route.getFlowElements().add(0, endpoint);
        }
        Flow next = TestFixtures.getEventGeneratingConsumerSingleRecipientRouterFlow(TestFixtures.BASE_META_PACK);
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(branched, next));
        context.setIkasanModule(module);
        Graphics2D graphics = new BufferedImage(1800, 1200, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            IkasanModuleViewHandler handler = (IkasanModuleViewHandler) ViewHandlerCache.getAbstractViewHandler(myProject, module);
            handler.initialiseDimensions(graphics, 0, 0, 1800, 1200);
            IkasanFlowViewHandler box = ViewHandlerCache.getFlowViewHandler(myProject, branched);
            IkasanFlowComponentViewHandler consumer = ViewHandlerCache.getFlowComponentViewHandler(myProject, branched.getConsumer());
            assertTrue("Consumer must stay at the start of its flow", consumer.getLeftX() <
                    ViewHandlerCache.getFlowComponentViewHandler(myProject, branched.getFlowRoute().getFlowElements().get(0)).getLeftX());
            List<IkasanFlowRouteViewHandler> routes = box.getFlowRouteViewHandler().getAllFlowRouteViewHandlers(new ArrayList<>(), box.getFlowRouteViewHandler());
            for (IkasanFlowRouteViewHandler route : routes) {
                for (FlowElement element : route.getFlowRoute().getConsumerAndFlowRouteElements()) {
                    IkasanFlowComponentViewHandler view = ViewHandlerCache.getFlowComponentViewHandler(myProject, element);
                    assertTrue("Component outside flow horizontally", view.getLeftX() >= box.getLeftX() && view.getRightX() <= box.getRightX());
                    assertTrue("Component outside flow vertically", view.getTopY() >= box.getTopY() && view.getBottomY() <= box.getBottomY());
                }
            }
            for (FlowRoute route : branched.getFlowRoute().getChildRoutes()) {
                IkasanFlowComponentViewHandler endpoint = ViewHandlerCache.getFlowComponentViewHandler(myProject, route.getFlowElements().get(0));
                IkasanFlowComponentViewHandler nextComponent = ViewHandlerCache.getFlowComponentViewHandler(myProject, route.getFlowElements().get(1));
                int labelOverhang = Math.max(0, (IkasanFlowComponentViewHandler.FLOWCHART_SYMBOL_DEFAULT_WIDTH - endpoint.getWidth() + 1) / 2);
                assertTrue("Branch label must have leading clearance", endpoint.getRawLeadingGap() >= labelOverhang);
                assertTrue("Branch label must clear next component", nextComponent.getLeftX() > endpoint.getRightX() + labelOverhang);
            }
            assertTrue("Next flow must be below all branches", ViewHandlerCache.getFlowViewHandler(myProject, next).getTopY() > box.getBottomY());
        } finally { graphics.dispose(); context.getViewHandlerFactory().clear(); }
    }

    public void testClippedCachedPaintingMatchesFullPaintingAtTopAndAfterScroll() throws Exception {
        UiContext context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        List<Flow> flows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            FlowElement consumer = TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
            Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("Flow " + i).consumer(consumer).build();
            consumer.setContainingFlow(flow);
            FlowElement producer = TestFixtures.getLoggingProducer(TestFixtures.BASE_META_PACK);
            producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().setFlowElements(new ArrayList<>(List.of(producer)));
            flows.add(flow);
        }
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, flows);
        context.setIkasanModule(module);
        IkasanModuleViewHandler handler = (IkasanModuleViewHandler) ViewHandlerCache.getAbstractViewHandler(myProject, module);
        JPanel canvas = new JPanel(); canvas.setSize(1200, 700);
        try {
            for (int scroll : new int[] {0, 1100}) {
                BufferedImage expected = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
                BufferedImage actual = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
                Graphics2D referenceGraphics = expected.createGraphics(); Graphics2D cachedGraphics = actual.createGraphics();
                try {
                    Graphics2D layoutGraphics = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB).createGraphics();
                    try { handler.initialiseDimensions(layoutGraphics, 0, 0, 1200, 700); }
                    finally { layoutGraphics.dispose(); }
                    referenceGraphics.translate(0, -scroll); referenceGraphics.setClip(0, scroll, 1200, 700);
                    cachedGraphics.translate(0, -scroll); cachedGraphics.setClip(0, scroll, 1200, 700);
                    StudioUIUtils.drawStringLeftAlignedFromTopLeft(referenceGraphics, handler.getText(), 10, 10, StudioUIUtils.getBoldFont());
                    for (Flow flow : flows) {
                        IkasanFlowViewHandler flowHandler = ViewHandlerCache.getFlowViewHandler(myProject, flow);
                        flowHandler.paintComponent(canvas, referenceGraphics, -1, flowHandler.getTopY());
                    }
                    handler.paintComponent(canvas, cachedGraphics, -1, -1);
                    assertArrayEquals("Paint differs at scroll " + scroll,
                            expected.getRGB(0, 0, 1200, 700, null, 0, 1200),
                            actual.getRGB(0, 0, 1200, 700, null, 0, 1200));
                } finally { referenceGraphics.dispose(); cachedGraphics.dispose(); }
            }
        } finally { context.getViewHandlerFactory().clear(); }
    }
}
