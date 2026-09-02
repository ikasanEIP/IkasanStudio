package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;

import javax.swing.*;
import java.awt.*;

public class IkasanModuleViewHandler extends AbstractViewHandlerIntellij {
    private static final Logger LOG = Logger.getInstance("#IkasanModuleViewHandler");
    // Wide enough to fit DesignerCanvas's per-flow transport-control buttons and status label (up to
    // "Stopped in Error") to the left of every flow, clear of the endpoint's own icon - was 150, too tight once
    // those were added, so the status label was overlapping/clipping the flow's leftmost endpoint.
    public static final int FLOW_X_START_POINT = 260;
    public static final int FLOW_X_RIGHT_BUFFER = 150;
    public static final int FLOW_Y_START_POINT = 100;
    public static final int FLOW_Y_BOTTTOM_BUFFER = 100;
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
        Flow previousFlow = null;
        LOG.debug("STUDIO: paintComponent invoked");
        // Module name
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, getText(),10,10, StudioUIUtils.getBoldFont());
        for (Flow ikasanFlow : module.getFlows()) {
            // remember initialise has already set TestV1,y, but we may be dealing with component move
            IkasanFlowViewHandler flowViewViewHandler = getOrCreateFlowViewViewHandler(project, ikasanFlow);
            if (flowViewViewHandler != null) {

                if (currentY == 0) {
                    currentY = flowViewViewHandler.getTopY();
                } else {
                    currentY += gapAfterFlow(previousFlow, g);
                }
                currentY = flowViewViewHandler.paintComponent(canvas, g, -1, currentY);
                previousFlow = ikasanFlow;
            }
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
        return FLOW_X_START_POINT;
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
        int minimumTopY = FLOW_Y_START_POINT;
        int maxWidth = 0;
        int maxHeight;
        int lastFlowHeight = topy;
        for(Flow ikasanFlow : module.getFlows()) {
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
    public void dispose() {

    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
    }

}
