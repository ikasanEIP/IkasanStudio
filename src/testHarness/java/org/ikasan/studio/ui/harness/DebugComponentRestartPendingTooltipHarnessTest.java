package org.ikasan.studio.ui.harness;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Live repro for a reported bug: dropping a Debug component onto a flow while its module is running correctly
 * flashes the new component (DesignerCanvas#isRestartPendingFor + #isAttentionFlashOn), but hovering it was
 * reported to not also show the "restart required" tooltip that DesignerCanvas#mouseMoveAction is supposed to
 * append (see #appendRestartWarning). Reproduces the exact drag-and-drop entry point
 * (DesignerCanvas#requestToAddComponent, the only caller of which is the real drag/drop transfer handler) with a
 * simulated running module, then drives the same private mouseMoveAction the live mouse listener calls.
 */
public class DebugComponentRestartPendingTooltipHarnessTest extends ComponentTestHarness {

    @Test
    void hoveringADebugComponentDroppedWhileRunningShowsRestartWarning() throws Exception {
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

        // Simulate a running module process the same way a real launch would populate it, so
        // IkasanDebugSessionService#isModuleStopped() returns false without spawning a real process.
        IkasanDebugSessionService debugSessionService = project.getService(IkasanDebugSessionService.class);
        Field moduleProcessesField = IkasanDebugSessionService.class.getDeclaredField("moduleProcesses");
        moduleProcessesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<ProcessHandler> moduleProcesses = (Set<ProcessHandler>) moduleProcessesField.get(debugSessionService);
        moduleProcesses.add(mock(ProcessHandler.class));

        BufferedImage img = com.intellij.util.ui.ImageUtil.createImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            flowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            IkasanFlowComponentViewHandler producerVH = ViewHandlerCache.getFlowComponentViewHandler(project, producer);
            Point producerCentre = producerVH.getCentrePoint();

            DesignerCanvas designerCanvas = new DesignerCanvas(project);

            // Drive the same two calls DesignerCanvas#requestToAddComponent's isDebug() branch makes, via
            // reflection - bypassing the surrounding GenerationRequest/PSI file sync (StudioProjectFiles
            // .refreshCodeFromModel), which needs a full generated-project PSI association this headless test
            // doesn't set up and which is irrelevant to the flash/tooltip mechanism under test.
            FlowElement debugComponent = TestFixtures.getDebugTransition(metapackVersion);

            Method insertMethod = DesignerCanvas.class.getDeclaredMethod(
                    "insertNewComponentBetweenSurroundingPair", Flow.class, FlowRoute.class, FlowElement.class, int.class, int.class);
            insertMethod.setAccessible(true);
            insertMethod.invoke(designerCanvas, flow, rootRoute, debugComponent, producerCentre.x, producerCentre.y);

            Method markRestartPendingMethod = DesignerCanvas.class.getDeclaredMethod(
                    "markRestartPendingIfModuleRunning", FlowElement.class);
            markRestartPendingMethod.setAccessible(true);
            markRestartPendingMethod.invoke(designerCanvas, debugComponent);

            // The new component's layout (used by getCentrePoint() below) is only computed during a paint /
            // initialiseDimensions pass - re-run it now that the debug component has actually been inserted.
            flowViewHandler.initialiseDimensions(g, 50, 50, -1, -1);

            assertThat(rootRoute.getFlowElementsNoExternalEndPoints()).contains(debugComponent);

            // Sanity check: this is the same flag paintRestartPendingOutlineIfNeeded reads to decide whether to
            // flash the component, so if this is false the flash itself would already be broken.
            assertThat(designerCanvas.isRestartPendingFor(debugComponent))
                    .as("Debug component dropped while the module is running must be marked restart-pending")
                    .isTrue();

            IkasanFlowComponentViewHandler debugVH = ViewHandlerCache.getFlowComponentViewHandler(project, debugComponent);
            Point debugCentre = debugVH.getCentrePoint();

            Method mouseMoveMethod = DesignerCanvas.class.getDeclaredMethod("mouseMoveAction", int.class, int.class);
            mouseMoveMethod.setAccessible(true);
            mouseMoveMethod.invoke(designerCanvas, debugCentre.x, debugCentre.y);

            assertThat(designerCanvas.getToolTipText())
                    .as("Hovering a debug component added while the module is running should warn a restart is needed")
                    .contains(StudioBundle.message("tooltip.ModuleRestartRequiredForChange"));
        } finally {
            g.dispose();
        }
    }
}
