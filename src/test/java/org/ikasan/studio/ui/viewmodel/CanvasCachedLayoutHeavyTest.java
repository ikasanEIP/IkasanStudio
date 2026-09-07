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
