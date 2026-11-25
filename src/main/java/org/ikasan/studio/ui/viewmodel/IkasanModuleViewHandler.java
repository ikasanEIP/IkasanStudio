package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.project.Project;
import java.awt.*;
import javax.swing.*;
import org.ikasan.studio.core.model.analysis.TestFtpServerLinks.Link;
import org.ikasan.studio.core.model.analysis.TestFtpServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.runtime.HarnessEndpointSettings;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

public class IkasanModuleViewHandler extends AbstractViewHandlerIntellij {
    // A file-server card and connector need their own column beyond the fixed
    // transport buttons and status hover area at the left of the canvas.
    private static final int FILE_SERVER_LEFT_RESERVE = 110;
    public static final int FLOW_X_RIGHT_BUFFER = 260;
    public static final int FLOW_Y_BOTTTOM_BUFFER = 180;
    public static final int SCROLL_BAR_HEIGHT = 10;
    private final Module module;
    private final Project project;

    /**
     * @param module for the view handler
     */
    public IkasanModuleViewHandler(Project project, Module module) {
        this.project = project;
        this.module = module;
    }

    @Override
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        int currentY = 0;
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, getText(), 10, 10, StudioUIUtils.getBoldFont());
        for (Flow flow : module.getFlows()) {
            if (isTestHarnessFlow(flow)) continue;
            IkasanFlowViewHandler handler = getOrCreateFlowViewViewHandler(project, flow);
            if (handler == null) continue;
            // Painting also positions external endpoints used by cross-flow connectors.
            handler.paintLaidOutComponent(canvas, g);
            currentY = handler.getBottomY();
        }
        return currentY;
    }

    /**
     * The vertical gap to leave below the given (previous) flow before the next one starts - normally the
     * configured flow distance, but widened to also fit that flow's own getting-started hint block when
     * it's incomplete and hints are enabled (see DesignerCanvas#paintGettingStartedHint, which positions each
     * flow's own hint starting right at that flow's getBottomY(), independently of this gap). Without this, a
     * flow needing "Add a Producer"/"Add a Consumer" would have its hint text drawn straight over the top of
     * whatever flow is stacked immediately below it.
     */
    private int gapAfterFlow(Flow previousFlow, Graphics graphics) {
        int flowDistance = IkasanStudioSettings.getFlowDistance();
        if (previousFlow != null && IkasanStudioSettings.areGettingStartedHintsEnabled()) {
            DesignerCanvas.GettingStartedHint flowHint = DesignerCanvas.getFlowHint(previousFlow);
            if (flowHint != null) {
                return Math.max(flowDistance,
                        DesignerCanvas.measureHintBlockHeight(graphics, graphics.getFont(), flowHint, true));
            }
        }
        return flowDistance;
    }

    // Might revert to centralised model but that will require double initialise.
    private int getFlowXStartPoint() {
        return flowXStartPoint(flowXStartPoint(), hasVisibleLeftFileServer(project, module));
    }

    /**
     * User-configurable (see IkasanStudioSettingsConfigurable's Canvas Layout section) - defaults to wide enough
     * to fit DesignerCanvas's per-flow transport-control buttons and status label (up to "Stopped in Error") to
     * the left of every flow, clear of the endpoint's own icon. The default was 150 before those controls were
     * added, too tight once they were, so the status label overlapped/clipped the flow's leftmost endpoint - a
     * user dialling this setting down too far can reintroduce that same clipping.
     */
    private static int flowXStartPoint() {
        return IkasanStudioSettings.getFlowXStartPoint();
    }

    /**
     * A JMS test-consumer harness flow (see {@code CreateTestJmsConsumerFlowAction}/{@code TestJmsHarnessLinks})
     * is never given its own box/route on the canvas - it's real, and still generates real code, but visually
     * it's replaced by a single compact node against the Producer it inspects (see
     * {@code DesignerCanvas#paintTestJmsHarnessNode}), the same treatment already given to the Test FTP/Mail
     * Server nodes. Skipping it here, rather than after painting/measuring it, is what keeps it from both being
     * drawn as an ordinary flow AND reserving vertical space for one.
     */
    private static boolean isTestHarnessFlow(Flow flow) {
        Object owner = flow.getPropertyValue("testHarnessOwner");
        return owner != null && !owner.toString().isBlank();
    }

    /** Reserve space only for a running server actually drawn to the left of a consumer. */
    static boolean hasVisibleLeftFileServer(Project project, Module module) {
        TestFtpServerService ftp = project.getService(TestFtpServerService.class);
        if (ftp.isRunning()) {
            for (Link link : TestFtpServerLinks.findLinks(module)) {
                for (FlowElement consumer : link.consumers()) {
                    if (ftp.isRunningAt(HarnessEndpointSettings.effectiveFtp(project, consumer)))
                        return true;
                }
            }
        }
        TestSftpServerService sftp = project.getService(TestSftpServerService.class);
        return sftp.isRunning() && module.getFlows().stream()
                .flatMap(flow -> flow.getFlowElementsNoExternalEndPoints().stream())
                .anyMatch(element -> element.getComponentMeta().isConsumer()
                        && "SFTP Endpoint".equals(element.getComponentMeta().getEndpointKey())
                        && HarnessEndpointSettings.effective(project, element));
    }

    static int flowXStartPoint(int configured, boolean leftFileServer) {
        return leftFileServer
                ? Math.max(configured, IkasanStudioSettings.DEFAULT_FLOW_X_START_POINT) + FILE_SERVER_LEFT_RESERVE
                : configured;
    }

    /**
     * Look at the current components and work out the required TestV1, y , width and height of this container
     * @param graphics object
     * @param xx to use
     * @param topy to use
     * @param width of container which may be ignored if it is set by the component
     * @param height of container which may be ignored if it is set by the component
     */
    @Override
    public void initialiseDimensions(Graphics graphics, int xx, int topy, int width, int height) {
        setLeftX(xx);
        setTopY(topy);
        setWidth(width);
        setHeight(height);
        // Module title
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(graphics, module.getIdentity(),10,10, StudioUIUtils.getBoldFont());
        int minimumTopY = IkasanStudioSettings.getFlowYStartPoint();
        int maxWidth = 0;
        int maxHeight;
        int lastFlowHeight = topy;
        for(Flow ikasanFlow : module.getFlows()) {
            if (isTestHarnessFlow(ikasanFlow)) continue;
            IkasanFlowViewHandler flowViewHandler = getOrCreateFlowViewViewHandler(project, ikasanFlow);
            if (flowViewHandler != null) {
                // initialise width/height to maximum, it will be adjusted down after reset
                flowViewHandler.initialiseDimensions(graphics, getFlowXStartPoint(), minimumTopY, width, height);
                minimumTopY = flowViewHandler.getBottomY();
                minimumTopY += gapAfterFlow(ikasanFlow, graphics);

                // Real width of Module = Max flow + left + right
                maxWidth = Math.max(maxWidth, flowViewHandler.getWidth());
                lastFlowHeight = flowViewHandler.getHeight();
            }
        }
        maxWidth += getFlowXStartPoint() + FLOW_X_RIGHT_BUFFER;
        maxHeight = minimumTopY + lastFlowHeight + FLOW_Y_BOTTTOM_BUFFER;
        setWidth(Math.max(getWidth(), maxWidth));
        setHeight(Math.max(getHeight()-SCROLL_BAR_HEIGHT, maxHeight));
    }

    @Override
    public String getText() {
        return module.getIdentity();
    }

    /**
     * Perform any tidy up during deletion of this element
     */
    @Override
    public void dispose() { }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
    }

}
