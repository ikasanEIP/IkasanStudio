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
    public static final int FLOW_X_RIGHT_BUFFER = 150;
    public static final int FLOW_Y_START_POINT = 100;
    public static final int FLOW_Y_BOTTTOM_BUFFER = 100;
    public static final int SCROLL_BAR_HEIGHT = 10;
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
        LOG.debug("STUDIO: paintComponent invoked");
        // Module name
        StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, getText(),10,10, StudioUIUtils.getBoldFont(g));
        for (Flow ikasanFlow : module.getFlows()) {
            // remember initialise has already set x,y, but we may be dealing with component move
            IkasanFlowViewHandler flowViewViewHandler = getOrCreateFlowViewViewHandler(projectKey, ikasanFlow);
            if (flowViewViewHandler != null) {

                if (currentY == 0) {
                    currentY = flowViewViewHandler.getTopY();
                } else {
                    currentY += FLOW_VERTICAL_SPACING;
                }
                currentY = flowViewViewHandler.paintComponent(canvas, g, -1, currentY);
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
        int maxWidth = 0;
        int maxHeight = 0;
        int lastFlowHeight = topy;
        for(Flow ikasanFlow : module.getFlows()) {
            IkasanFlowViewHandler flowViewViewHandler = getOrCreateFlowViewViewHandler(projectKey, ikasanFlow);
            if (flowViewViewHandler != null) {
                // initialise width/height to maximum, it will be adjusted down after reset
                flowViewViewHandler.initialiseDimensions(graphics, getFlowXStartPoint(), minimumTopY, width, height);
                minimumTopY = flowViewViewHandler.getBottomY();
                minimumTopY += FLOW_VERTICAL_SPACING;

                // Real width of Module = Max flow + left + right
                maxWidth = Math.max(maxWidth, flowViewViewHandler.getWidth());
                lastFlowHeight = flowViewViewHandler.getHeight();
            }
        }
//LOG.info("StudioXX ModuleViewHandler Max Flow Width " + maxWidth + " current getFlowXStartPoint(): " + getFlowXStartPoint() + " BUFFFER " + FLOW_X_RIGHT_BUFFER);
        maxWidth += getFlowXStartPoint() + FLOW_X_RIGHT_BUFFER;
        maxHeight = minimumTopY + lastFlowHeight + FLOW_Y_BOTTTOM_BUFFER;
//LOG.info("StudioXX ModuleViewHandler width and height was " + getWidth() + ", " + getHeight());
        setWidth(Math.max(getWidth(), maxWidth));
        setHeight(Math.max(getHeight()-SCROLL_BAR_HEIGHT, maxHeight));
//LOG.info("StudioXX ModuleViewHandler width and height now " + getWidth() + ", " + getHeight());
    }

    @Override
    public String getText() {
        return module.getName();
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
        Thread thread = Thread.currentThread();
//        LOG.info("StudioXX: module view handler setWidth() invoked " + width + " trace:" + Arrays.toString(thread.getStackTrace()));
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        Thread thread = Thread.currentThread();
//        LOG.info("StudioXX: module view handler setHeight() invoked " + " trace:" + Arrays.toString(thread.getStackTrace()));
    }

}
