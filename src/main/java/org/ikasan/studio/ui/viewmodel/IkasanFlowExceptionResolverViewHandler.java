package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.model.ikasan.instance.FlowElement;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowExceptionResolverViewHandler extends ViewHandler {
    private static final Logger LOG = Logger.getInstance("#IkasanFlowExceptionResolverViewHandler");
    public static final int VERTICAL_PAD = 5;
    public static final int HORIZONTAL_PAD = 5;
    public static final int FLOWCHART_SYMBOL_DEFAULT_HEIGHT = 15;
    public static final int FLOWCHART_SYMBOL_DEFAULT_WIDTH = 42;
    private final int flowchartSymbolHeight = FLOWCHART_SYMBOL_DEFAULT_HEIGHT;
    private final int flowchartSymbolWidth = FLOWCHART_SYMBOL_DEFAULT_WIDTH;

    FlowElement flowElement;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flowElement for view handler
     */
    public IkasanFlowExceptionResolverViewHandler(FlowElement flowElement) {
        this.flowElement = flowElement;
    }


    /**
     * Return the X that the component could reasonably be displayed from the right side of its flow container
     * @param xOffsetFromRight to be set
     * @return at least 0, or the X coord that the resolver could be displayed at
     */
    public static int getXOffsetFromRight(int xOffsetFromRight) {
        int newX = xOffsetFromRight - FLOWCHART_SYMBOL_DEFAULT_WIDTH - HORIZONTAL_PAD;
        return Math.max(newX, 0);
    }

    public static int getYOffsetFromTop(int yOffsetFromTop) {
        int newY = yOffsetFromTop + VERTICAL_PAD;
        return Math.max(newY, 0);
    }

    /**
     * Paint the flow icon and the text underneath it
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return topY + symbole height
     */
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        LOG.debug("paintComponent invoked");
        // here we ket the components decide x,y
        paintFlowchartSymbol(canvas, g);
        return minimumTopY + flowchartSymbolHeight;
    }

    private void paintFlowchartSymbol(JPanel canvas, Graphics g) {
        getCanvasIcon().paintIcon(canvas, g, getLeftX(), getTopY());
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
        setLeftX(x);
        setTopY(y);
        setWidth(getCanvasIcon().getIconWidth());
    }

    public ImageIcon getCanvasIcon() {
        return flowElement.getComponentMeta().getCanvasIcon();
    }
}
