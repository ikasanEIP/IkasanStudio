package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class IkasanRouterFlowHandler extends IkasanFlowComponentViewHandler{
    private static final Logger LOG = Logger.getInstance("#IkasanRouterFlowHandler");
    List<IkasanFlowComponentViewHandler> childRouteViewHandlers;
    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     *
     * @param flowElement for the vie handler
     */
    public IkasanRouterFlowHandler(FlowElement flowElement, List<IkasanFlowComponentViewHandler> childRouteViewHandlers) {
        super(flowElement);
        this.childRouteViewHandlers = childRouteViewHandlers;
    }

    /**
     * Paint the flow icon and the text underneath it
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return the y position of the bottom of the text
     */
    @Override
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        LOG.debug("paintComponent invoked");
        minimumTopY = super.paintComponent(canvas, g, minimumTopX, minimumTopX);
        if (childRouteViewHandlers != null && ! childRouteViewHandlers.isEmpty()) {
            for (IkasanFlowComponentViewHandler childRouteViewHandler : childRouteViewHandlers) {
                minimumTopY = childRouteViewHandler.paintComponent(canvas, g, minimumTopX, minimumTopY);
            }
        }
        return minimumTopY ;
    }

    /**
     * Set the x and y co-ordinates of this component.
     * @param graphics object
     * @param x new x location
     * @param y new y location
     * @param width of container which may be ignored if it is set by the component
     * @param height of container which may be ignored if it is set by the component
     */
    @Override
    public void initialiseDimensions(Graphics graphics, int x, int y, int width, int height) {
        super.initialiseDimensions(graphics, x, y, width, height);

        if (childRouteViewHandlers != null && ! childRouteViewHandlers.isEmpty()) {
            for (IkasanFlowComponentViewHandler childRouteViewHandler : childRouteViewHandlers) {
                childRouteViewHandler.initialiseDimensions(graphics, x, y, width, height);
                y += flowchartSymbolHeight;
            }
        }
    }
}
