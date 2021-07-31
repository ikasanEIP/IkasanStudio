package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowExceptionResolverViewHandler extends ViewHandler {
    private static final Logger log = Logger.getLogger(IkasanFlowExceptionResolverViewHandler.class);
    public static final int VERTICAL_PAD = 5;
    public static final int HORIZONTAL_PAD = 5;
    public static final int FLOWCHART_SYMBOL_DEFAULT_HEIGHT = 15;
    public static final int FLOWCHART_SYMBOL_DEFAULT_WIDTH = 42;
    int flowchartSymbolHeight = FLOWCHART_SYMBOL_DEFAULT_HEIGHT;
    int flowchartSymbolWidth = FLOWCHART_SYMBOL_DEFAULT_WIDTH;

    IkasanFlowUIComponent ikasanFlowUIComponent;
    IkasanFlowComponent model;

    /**
     * The model can be null e.g. for a pallette item, once dragged onto a canvas, the model would be populated.
     * @param model
     */
    public IkasanFlowExceptionResolverViewHandler(IkasanFlowComponent model) {
        this.model = model;
        if (model != null) {
            ikasanFlowUIComponent = IkasanFlowUIComponentFactory.getInstance().getIkasanFlowUIComponentFromType(model.getType());
        } else {
            ikasanFlowUIComponent = IkasanFlowUIComponentFactory.getInstance().getUNKNOWN();
        }
    }


    /**
     * Return the X that the component could reasonable be displayed from the right side of its flow container
     * @param xx
     * @return at least 0, or the X coord that the resolver could be displayed at
     */
    public static int getXOffsetFromRight(int xx) {
        int newX = xx - FLOWCHART_SYMBOL_DEFAULT_WIDTH - HORIZONTAL_PAD;
        if (newX > 0) {
            return newX;
        } else {
            return 0;
        }
    }

    public static int getYOffsetFromTop(int yy) {
        int newY = yy + VERTICAL_PAD;
        if (newY > 0) {
            return newY;
        } else {
            return 0;
        }
    }

    /**
     * Paint the flow icon and the text underneath it
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return
     */
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        log.debug("paintComponent invoked");
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
        return ikasanFlowUIComponent.getCanvasIcon();
    }

    public IkasanFlowUIComponent getIkasanFlowUIComponent() {
        return ikasanFlowUIComponent;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponentViewHandler{" +
                "ikasanFlowUIComponent=" + ikasanFlowUIComponent +
                '}';
    }
}
