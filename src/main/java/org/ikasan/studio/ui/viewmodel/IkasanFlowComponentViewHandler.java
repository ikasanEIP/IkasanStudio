package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.Pair;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowComponentViewHandler extends AbstractViewHandler {
    private static final Logger LOG = Logger.getInstance("#IkasanFlowComponentViewHandler");
    public static final int TEXT_VERTICAL_SPACE = 5;
    public static final int FLOWCHART_SYMBOL_DEFAULT_HEIGHT = 60;
    public static final int FLOWCHART_SYMBOL_DEFAULT_WIDTH = 90;
    int flowchartSymbolHeight = FLOWCHART_SYMBOL_DEFAULT_HEIGHT;
    int flowchartSymbolWidth = FLOWCHART_SYMBOL_DEFAULT_WIDTH;

//    private Flow flow;
    private final FlowElement flowElement;

    public FlowElement getFlowElement() {
        return flowElement;
    }
    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flowElement for the vie handler
     */
    public IkasanFlowComponentViewHandler(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    /**
     * Paint the flow icon and the text underneath it
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return the y position of the bottom of the text
     */
    public int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) {
        LOG.debug("paintComponent invoked");
        // here we get the components decide x,y
        paintFlowchartSymbol(canvas, g);
        return paintSymbolText(g, PaintMode.PAINT);
    }

    private void paintFlowchartSymbol(JPanel canvas, Graphics g) {
        if ( getLeftX() < -10 ) {
            LOG.error("X was negative !!");
        }
        getCanvasIcon().paintIcon(canvas, g, getLeftX(), getTopY());
    }

    /**
     * Draw the text for the symbol on the screen
     * @param g graphics object
     * @param paintMode if DIMENSION_ONLY, don't actually paint, just get dimentions
     * @return the bottom for the painted text
     */
    private int paintSymbolText(Graphics g, PaintMode paintMode) {
        flowchartSymbolHeight = getCanvasIcon().getIconHeight();
        flowchartSymbolWidth = getCanvasIcon().getIconWidth();
        int bottomY = StudioUIUtils.drawCenteredStringFromTopCentre(g, paintMode, getText(),
                getBottomConnectorPoint().x, getBottomConnectorPoint().y + TEXT_VERTICAL_SPACE, flowchartSymbolWidth, null);
        setHeight(bottomY - getTopY());
        return bottomY;
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

        if (getWidth() < -10) {
            LOG.error("Width set to negative " + getWidth());
        }
        // this has the side effect of setting the correct height.

        if (getLeftX() < -10) {
            LOG.error("initialiseDimensions leftX" + getLeftX() + " topY" + getTopY() + " for component " + flowElement + "X was negative !!");
        }
        paintSymbolText(graphics, PaintMode.DIMENSION_ONLY);
    }

    public String getText() {
        return flowElement.getComponentName();
    }

    /**
     * How close (x,y) does a dragged component need to be to the centre of this component so that we consider it attachable.
     * @return the x,y coords
     */
    public static Pair<Integer, Integer> getProximityDetect() {
        return new Pair<>(((FLOWCHART_SYMBOL_DEFAULT_WIDTH) + 5), ((FLOWCHART_SYMBOL_DEFAULT_HEIGHT) + 5));
    }

    public ImageIcon getCanvasIcon() {
        return flowElement.getComponentMeta().getCanvasIcon();
    }

    @Override
    public Point getLeftConnectorPoint() {
        return new Point(getLeftX(), getTopY() + (flowchartSymbolHeight/2));
    }

    @Override
    public Point getRightConnectorPoint() {
        return new Point(getRightX(), getTopY() + (flowchartSymbolHeight/2));
    }

    @Override
    public Point getTopConnectorPoint() {
        return new Point(getLeftX() + (flowchartSymbolWidth/2), getTopY());
    }

    @Override
    public Point getBottomConnectorPoint() {
        return new Point(getLeftX() + (flowchartSymbolWidth/2), getTopY() + flowchartSymbolHeight);
    }
}
