package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;

public class IkasanModuleViewHandler extends ViewHandler {
    private static final Logger LOG = Logger.getInstance("#IkasanModuleViewHandler");
    public static final int FLOW_VERTICAL_SPACING = 20;
    public static final int FLOW_X_START_POINT = 150;
    public static final int FLOW_Y_START_POINT = 100;
    Module model;

    /**
     * @param model for the view handler
     */
    public IkasanModuleViewHandler(Module model) {
        this.model = model;
    }

    @Override
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        int currentY = 0;
        LOG.debug("paintComponent invoked");

        StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, model.getComponentName(),10,10, StudioUIUtils.getBoldFont(g));

        for (Flow ikasanFlow : model.getFlows()) {
            // remember initialise has already set x,y but we may be dealing with component move
            if (currentY == 0 ) {
                currentY = ikasanFlow.getViewHandler().getTopY();
            } else {
                currentY += FLOW_VERTICAL_SPACING;
            }
            currentY = ikasanFlow.getViewHandler().paintComponent(canvas, g, -1, currentY);
        }

        return currentY;
    }

    // Might revert to cetralised model but that will require double initialise.
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
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(graphics, model.getComponentName(),10,10, StudioUIUtils.getBoldFont(graphics));
        int minimumTopY = FLOW_Y_START_POINT;
        for(Flow ikasanFlow : model.getFlows()) {
            // intialise width/height to maximum, it will be adjusted down after reset
            ikasanFlow.getViewHandler().initialiseDimensions(graphics, getFlowXStartPoint(), minimumTopY, width, height);
            minimumTopY = ikasanFlow.getViewHandler().getBottomY();
            minimumTopY += FLOW_VERTICAL_SPACING;
        }
    }
}
