package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.icons.ComponentIconProvider;
import org.ikasan.studio.ui.model.MutablePair;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowComponentViewHandler extends AbstractViewHandlerIntellij {
    private static final Logger LOG = Logger.getInstance("#IkasanFlowComponentViewHandler");
    public static final int TEXT_VERTICAL_SPACE = 5;
    public static final int WIRETAP_HORIZONTAL_SPACE = 5;
    public static final int FLOWCHART_SYMBOL_DEFAULT_HEIGHT = 60;
    public static final int FLOWCHART_SYMBOL_DEFAULT_WIDTH = 90;
    int flowchartSymbolHeight = FLOWCHART_SYMBOL_DEFAULT_HEIGHT;
    int flowchartSymbolWidth = FLOWCHART_SYMBOL_DEFAULT_WIDTH;

    private final FlowElement flowElement;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flowElement for the vie handler
     */
    public IkasanFlowComponentViewHandler(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    public FlowElement getFlowElement() {
        return flowElement;
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
        LOG.debug("STUDIO: paintComponent invoked for component: " + flowElement);
        // here we get the components decide TestV1,y
        paintFlowchartSymbol(canvas, g);
        return paintSymbolText(g, PaintMode.PAINT);
    }

    private void paintFlowchartSymbol(JPanel canvas, Graphics g) {
        if ( getLeftX() < -10 ) {
            LOG.warn("STUDIO: paintFlowchartSymbol leftX " + getLeftX() + " was negative for component " + flowElement);
        }
        getCanvasIcon().paintIcon(canvas, g, getLeftX(), getTopY());
        paintDecorators(canvas, g);
    }

    /**
     * Return true if this component is at the X and Y coordinate
     * Remember 0,0 is top, left i.e. Y increases downwards
     * @param x in question
     * @param y in question
     * @return true if this component (or its wiretaps) are at that location
     */
    @Override
    public boolean isComponentAtXY(int x, int y) {
        boolean result = super.isComponentAtXY(x, y);

        if (!result) {
            result = getFlowElement().hasDecorators() && isDecoratorAtXY(x, y);
        }
        return result;
    }

    /**
     * Return true if this component's decorator is at the X and Y coordinate
     * Remember 0,0 is top, left i.e. Y increases downwards
     * @param x in question
     * @param y in question
     * @return true if this component (or its wiretaps) are at that location
     */
    public boolean isDecoratorAtXY(int x, int y) {
        boolean result = false;
        if (getFlowElement().hasDecorators()) {
            result = ((getLeftX() - getLeadingGap() <= x && x <= getLeftX()) ||
                      (getRightX()                  <= x && x <= getRightX() + getTrailingGap()) ) &&
                    getTopY() <= y && y <= getTopY() + ComponentIconProvider.getDecoratorHeight();
            if (result) {
            LOG.info("Decorator found for " + flowElement.getIdentity());
            }
        }
        return result;
    }

    private void paintDecorators(JPanel canvas, Graphics g) {
        int leftX = getLeftX();
        int rightX = getLeftX() + flowchartSymbolWidth;
        if (flowElement.hasWiretap()) {
            Icon wiretapIcon = ComponentIconProvider.getWiretapIcon();
            List<Decorator> wiretaps = flowElement.getWiretaps();

            if (!wiretaps.isEmpty() && wiretaps.stream().anyMatch(Decorator::isBefore)) {
                leftX -= (WIRETAP_HORIZONTAL_SPACE + wiretapIcon.getIconWidth());
                wiretapIcon.paintIcon(canvas, g, leftX, getTopY());
            }
            if (!wiretaps.isEmpty() && wiretaps.stream().anyMatch(Decorator::isAfter)) {
                rightX += WIRETAP_HORIZONTAL_SPACE;
                wiretapIcon.paintIcon(canvas, g, rightX, getTopY());
                rightX += wiretapIcon.getIconWidth();
            }
        }
        if (flowElement.hasLogWiretap()) {
            Icon logWiretapIcon = ComponentIconProvider.getLogWiretapIcon();
            List<Decorator> logWiretaps = flowElement.getLogWiretaps();
            if (!logWiretaps.isEmpty() && logWiretaps.stream().anyMatch(Decorator::isBefore)) {
                leftX -= (WIRETAP_HORIZONTAL_SPACE + logWiretapIcon.getIconWidth());
                logWiretapIcon.paintIcon(canvas, g, leftX, getTopY());
            }
            if (!logWiretaps.isEmpty() && logWiretaps.stream().anyMatch(Decorator::isAfter)) {
                rightX += WIRETAP_HORIZONTAL_SPACE;
                logWiretapIcon.paintIcon(canvas, g, rightX, getTopY());
            }
        }
    }

    /**
     * Draw the text for the symbol on the screen
     * @param g graphics object
     * @param paintMode if DIMENSION_ONLY don't actually paint, just get dimensions
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
     * Set the TestV1 and y co-ordinates of this component.
     * @param graphics object
     * @param x new TestV1 location
     * @param y new y location
     * @param width of container, which may be ignored if it is set by the component
     * @param height of container, which may be ignored if it is set by the component
     */
    @Override
    public void initialiseDimensions(Graphics graphics, int x, int y, int width, int height) {
        setLeftX(x);
        setTopY(y);
        setWidth(getCanvasIcon().getIconWidth());
        int numberOfBeforeDecorators = flowElement.getBeforeDecorators().size();
        int numberOfAfterDecorators = flowElement.getAfterDecorators().size();

        if (numberOfBeforeDecorators > 0) {
            // The normal inter-component gap is already contributed by the neighbouring components' own
            // getLeadingGap()/getTrailingGap() (floor-clamped to getMinimumGap()) in the layout accumulation -
            // adding getMinimumGap() again here double-counted it, pushing the decorator an extra gap-width
            // away from the preceding component instead of sitting at the normal spacing.
            int beforeWidth = numberOfBeforeDecorators * (ComponentIconProvider.getWiretapIcon().getIconWidth() + WIRETAP_HORIZONTAL_SPACE);
            setLeadingGap(beforeWidth);
            setLeftX(getLeftX() + beforeWidth);
        } else {
            setLeadingGap(0);
        }
        if (numberOfAfterDecorators > 0) {
            int afterWidth = numberOfAfterDecorators * (ComponentIconProvider.getWiretapIcon().getIconWidth() + WIRETAP_HORIZONTAL_SPACE);
            setTrailingGap(afterWidth);
        } else {
            setTrailingGap(0);
        }

        if (getWidth() < -10) {
            LOG.warn("STUDIO: SERIOUS: Width set to negative " + getWidth());
        }
        // this has the side effect of setting the correct height.

        if (getLeftX() < -10) {
            LOG.warn("STUDIO: SERIOUS: initialiseDimensions leftX" + getLeftX() + " topY" + getTopY() + " for component " + flowElement + "X was negative !!");
        }
        paintSymbolText(graphics, PaintMode.DIMENSION_ONLY);
    }

    @Override
    public String getText() {
        return flowElement.getComponentName();
    }

    /**
     * How close (TestV1,y) does a dragged component need to be to the centre of this component so that we consider it attachable.
     * @return the TestV1,y coords
     */
    public static MutablePair<Integer, Integer> getProximityDetect() {
        return new MutablePair<>(((FLOWCHART_SYMBOL_DEFAULT_WIDTH) + 5), ((FLOWCHART_SYMBOL_DEFAULT_HEIGHT) + 5));
    }

    public Icon getCanvasIcon() {
        return ComponentIconProvider.getCanvasIcon(flowElement.getComponentMeta());
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
    public Point getBottomConnectorPoint() {
        return new Point(getLeftX() + (flowchartSymbolWidth/2), getTopY() + flowchartSymbolHeight);
    }

    /**
     * Perform any tidy up during deletion of this element
     */
    @Override
    public void dispose() {
    }
}
