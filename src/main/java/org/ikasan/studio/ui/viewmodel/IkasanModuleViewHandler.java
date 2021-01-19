package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.ui.SUIUtils;

import javax.swing.*;
import java.awt.*;

public class IkasanModuleViewHandler extends ViewHandler {
    private static final Logger log = Logger.getLogger(IkasanModuleViewHandler.class);
    public static final int FLOW_VERTICAL_SPACING = 20;
    public static final int FLOW_X_START_POINT = 150;
    public static final int FLOW_Y_START_POINT = 100;
    IkasanModule model;

    /**
     * @param model
     */
    public IkasanModuleViewHandler(IkasanModule model) {
        this.model = model;
    }

    @Override
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        log.debug("paintComponent invoked");
        SUIUtils.drawStringLeftAlignedFromTopLeft(g, model.getDescription(),10,10, SUIUtils.getBoldFont(g));

        int currentY = 0;
        for (IkasanFlow ikasanFlow : model.getFlows()) {
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
        SUIUtils.drawStringLeftAlignedFromTopLeft(graphics, model.getDescription(),10,10, SUIUtils.getBoldFont(graphics));
        int minimumTopY = FLOW_Y_START_POINT;
        for(IkasanFlow ikasanFlow : model.getFlows()) {
            // intialise width/height to maximum, it will be adjusted down after reset
            ikasanFlow.getViewHandler().initialiseDimensions(graphics, getFlowXStartPoint(), minimumTopY, width, height);
            minimumTopY = ikasanFlow.getViewHandler().getBottomY();
            minimumTopY += FLOW_VERTICAL_SPACING;
        }
    }
}
