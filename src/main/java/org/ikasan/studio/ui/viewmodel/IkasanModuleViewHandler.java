package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;

public class IkasanModuleViewHandler extends AbstractViewHandlerIntellij {
    private static final Logger LOG = Logger.getInstance("#IkasanModuleViewHandler");
    public static final int FLOW_VERTICAL_SPACING = 20;
    public static final int FLOW_X_START_POINT = 150;
    public static final int FLOW_Y_START_POINT = 100;
    private final Module module;
    private final String projectKey;

    /**
     * @param module for the view handler
     */
    public IkasanModuleViewHandler(String projectKey, Module module) {
        this.projectKey = projectKey;
        this.module = module;
    }

    @Override
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        int currentY = 0;
        LOG.debug("paintComponent invoked");

        StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, module.getName(),10,10, StudioUIUtils.getBoldFont(g));
        for (Flow ikasanFlow : module.getFlows()) {
            // remember initialise has already set x,y, but we may be dealing with component move
            IkasanFlowViewHandler viewHandler = getFlowViewViewHandler(projectKey, ikasanFlow);
            if (viewHandler != null) {

                if (currentY == 0) {
                    currentY = viewHandler.getTopY();
                } else {
                    currentY += FLOW_VERTICAL_SPACING;
                }
                currentY = viewHandler.paintComponent(canvas, g, -1, currentY);
            }
        }
        return currentY;
    }

    // Might revert to centralised model but that will require double initialise.
    private int getFlowXStartPoint() {
        return FLOW_X_START_POINT;
    }

    /**
     * Look at the current components and work out the required x, y , width and height of this container
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
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(graphics, module.getName(),10,10, StudioUIUtils.getBoldFont(graphics));
        int minimumTopY = FLOW_Y_START_POINT;
        for(Flow ikasanFlow : module.getFlows()) {
            IkasanFlowViewHandler viewHandler = getFlowViewViewHandler(projectKey, ikasanFlow);
            if (viewHandler != null) {
                // initialise width/height to maximum, it will be adjusted down after reset
                viewHandler.initialiseDimensions(graphics, getFlowXStartPoint(), minimumTopY, width, height);
                minimumTopY = viewHandler.getBottomY();
                minimumTopY += FLOW_VERTICAL_SPACING;
            }
        }
    }
}
