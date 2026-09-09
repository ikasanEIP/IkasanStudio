package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live repro for a reported bug: "Save image" on the canvas always produced an image as tall as the canvas's
 * live on-screen size (whatever the IDE window happened to be), not the actual diagram content - a short
 * diagram left a large blank area at the bottom. Root cause: CanvasPanel adds this (non-Scrollable) canvas to
 * a JViewport, whose default ViewportLayout stretches the view to at least fill the scroll pane's visible
 * area, and DesignerCanvas#saveAsImage used to size its exported BufferedImage from that live on-screen
 * getWidth()/getHeight() rather than the diagram's own tight content bounds.
 */
public class SaveAsImageTightContentSizeHarnessTest extends ComponentTestHarness {

    @Test
    void savedImageSizeTracksContentNotTheInflatedOnScreenViewportSize() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;

        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(metapackVersion);
        FlowElement producer = TestFixtures.getDevNullProducer(metapackVersion);

        Flow flow = TestFixtures.getUnbuiltFlow(metapackVersion).consumer(consumer).build();
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(producer)))
                .build();
        consumer.setContainingFlowRoute(rootRoute);
        producer.setContainingFlowRoute(rootRoute);
        flow.setFlowRoute(rootRoute);

        Module module = TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));

        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);
        // saveAsImage's own success/failure notification reads this back via StudioUIUtils - normally wired
        // up by CanvasPanel, which this minimal test doesn't construct.
        uiContext.setCanvasTextArea(new javax.swing.JTextArea());

        DesignerCanvas designerCanvas = new DesignerCanvas(project);
        // Simulate a tall IDE window / scroll-pane viewport - far larger than one small flow needs - the same
        // way a plain (non-Scrollable) view gets stretched by JViewport's default ViewportLayout.
        designerCanvas.setSize(3000, 3000);

        File savedFile = File.createTempFile("save-as-image-tight-content-size-test", ".png");
        savedFile.deleteOnExit();
        try {
            designerCanvas.saveAsImage(savedFile, "png", false);

            BufferedImage saved = ImageIO.read(savedFile);
            assertThat(saved).as("saveAsImage should have written a readable PNG").isNotNull();

            // The exported diagram must be sized to its own content (one small flow, comfortably under
            // 1200x1000 including the existing FLOW_X_RIGHT_BUFFER/FLOW_Y_BOTTTOM_BUFFER margin) rather than
            // the 3000x3000 on-screen size simulated above.
            assertThat(saved.getWidth())
                    .as("exported width should track the diagram's content, not the inflated on-screen width")
                    .isLessThan(1200);
            assertThat(saved.getHeight())
                    .as("exported height should track the diagram's content, not the inflated on-screen height")
                    .isLessThan(1000);

            // The canvas itself must be restored to its normal (viewport-tracking) on-screen size afterward,
            // not left pinned at the tight export size.
            assertThat(designerCanvas.getWidth()).isEqualTo(3000);
            assertThat(designerCanvas.getHeight()).isEqualTo(3000);
        } finally {
            savedFile.delete();
            // saveAsImage's paint pass can start DesignerCanvas's flow-error-flash Timer; without disposal it
            // leaks past this test and trips the harness's disposed-Swing-Timer check on later tests.
            designerCanvas.disposeCanvas();
        }
    }
}
