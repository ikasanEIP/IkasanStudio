package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanFlowElement;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.UIUtils;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowElementViewHandler extends ViewHandler {
    private static final Logger log = Logger.getLogger(IkasanFlowElementViewHandler.class);
    public static final int TEXT_VERTICAL_SPACE = 5;
    public static final int FLOWCHART_SYMBOL_DEFAULT_HEIGHT = 60;
    public static final int FLOWCHART_SYMBOL_DEFAULT_WIDTH = 90;
    int flowchartSymbolHeight = FLOWCHART_SYMBOL_DEFAULT_HEIGHT;
    int flowchartSymbolWidth = FLOWCHART_SYMBOL_DEFAULT_WIDTH;

    IkasanFlowUIComponent ikasanFlowUIComponent;
    IkasanFlowElement model;

    /**
     * The model can be null e.g. for a pallette item, once dragged onto a canvas, the model would be populated.
     * @param model
     */
    public IkasanFlowElementViewHandler(IkasanFlowElement model) {
        this.model = model;
        if (model != null) {
            ikasanFlowUIComponent = IkasanFlowUIComponentFactory.getInstance().getIkasanFlowUIComponentFromType(model.getType());
        } else {
            ikasanFlowUIComponent = IkasanFlowUIComponentFactory.getInstance().getUNKNOWN();
        }
    }

    public String getPropertiesAsString() {
        return model.getProperties().toString();
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
        return paintSymbolText(g, PaintMode.PAINT);
    }

    private void paintFlowchartSymbol(JPanel canvas, Graphics g) {
        getCanvasIcon().paintIcon(canvas, g, getLeftX(), getTopY());
    }

    private int paintSymbolText(Graphics g, PaintMode paintMode) {
        flowchartSymbolHeight = getCanvasIcon().getIconHeight();
        flowchartSymbolWidth = getCanvasIcon().getIconWidth();
        int bottomY = UIUtils.drawCenteredStringFromTopCentre(g, paintMode, getText(),
                getBottomConnectorPoint().x, getBottomConnectorPoint().y + TEXT_VERTICAL_SPACE, flowchartSymbolWidth, null);
        setHeight(bottomY - getTopY());
        return bottomY;
    }

    public void initialiseDimensions(Graphics g, int x, int y, int width, int height) {
        setLeftX(x);
        setTopY(y);
        setWidth(getCanvasIcon().getIconWidth());
        // this has the side effect of setting the correct height.
        paintSymbolText(g, PaintMode.DIMENSION_ONLY);
    }

    public String getText() {
        return model.getDescription();
    }

    public ImageIcon getCanvasIcon() {
        return ikasanFlowUIComponent.getCanvasIcon();
    }

    @Override
    public Point getLeftConnectorPoint() {
        return new Point(getLeftX(), getTopY() + (flowchartSymbolHeight/2));
    }

    @Override
    public Point getRightConnectorPoint() {
        return new Point(getRightX(), getTopY() + (flowchartSymbolHeight/2));
    }

    public Point getTopConnectorPoint() {
        return new Point(getLeftX() + (flowchartSymbolWidth/2), getTopY());
    }

    public Point getBottomConnectorPoint() {
        return new Point(getLeftX() + (flowchartSymbolWidth/2), getTopY() + flowchartSymbolHeight);
    }

    public IkasanFlowUIComponent getIkasanFlowUIComponent() {
        return ikasanFlowUIComponent;
    }

    @Override
    public String toString() {
        return "IkasanFlowElementViewHandler{" +
                "ikasanFlowUIComponent=" + ikasanFlowUIComponent +
                '}';
    }
}
