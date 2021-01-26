package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowViewHandler extends ViewHandler {
    private static final Logger log = Logger.getLogger(IkasanFlowViewHandler.class);
    public static final int FLOW_X_SPACING = 30;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    IkasanFlow model;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param model
     */
    public IkasanFlowViewHandler(IkasanFlow model) {
        this.model = model;
    }

    public String getText() {
        return model.getName() != null ? model.getName() : model.getDescription();
    }

    private int paintFlowTitle(Graphics g, PaintMode paintMode) {
        return StudioUIUtils.drawCenteredStringFromTopCentre
                (g, paintMode, getText(), getLeftX() + (getWidth() / 2), getTopY() + FLOW_CONTAINER_BORDER, getWidth(), StudioUIUtils.getBoldFont(g));
    }

    private int getFlowTitleWidth(Graphics g) {
        return StudioUIUtils.getTextWidth(g, getText());
    }

    private int getFlowTitleHeight(Graphics g) {
        return StudioUIUtils.getTextHeight(g);
    }


    private void paintIkasanFlowContainer(Graphics g, int x, int y, int width, int height) {
//        log.debug("paintIkasanFlowContainer invoked");
//    private void paintIkasanFlowContainer(Graphics g) {

//        log.info("paintIkasanFlowContainer x [" + x + "] y [" + y + "] width [" + width + "] height [" + height + "]");
        g.setColor(StudioUIUtils.IKASAN_GREY);
        g.fillRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(Color.BLACK);
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.drawRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
    }


    /**
     * Use the value of reset if it is greater than -1
     * @param reset set to -1 if we don't yet know if we need to override the current value
     * @param current value that might be overriden
     * @return reset unless it was -1
     */
    private int getNewCoord(int reset, int current) {
        if (reset > -1) {
            return reset;
        } else {
            return current;
        }
    }

    public int paintComponent(JPanel canvas, Graphics g, int minimumLeftX, int minimumTopY) {
        log.debug("paintComponent invoked");
        int newLeftX = getNewCoord(minimumLeftX, getLeftX());
        int newTopY = getNewCoord(minimumTopY, getTopY());

        if (newLeftX != getLeftX() || newTopY != getTopY()) {
            initialiseDimensions(g, newLeftX, newTopY,-1, -1);
        } else {
//            initialiseDimensionsNotChildren(g, newLeftX, newTopY,-1, -1);
            initialiseDimensionsNotChildren(g, newLeftX, newTopY);
        }
        paintIkasanFlowContainer(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowTitle(g, PaintMode.PAINT);

        List<IkasanFlowComponent> flowElementList = model.getFlowComponentList();
        int flowSize = flowElementList.size();
        StudioUIUtils.setLine(g, 2f);
        for (int index=0; index < flowSize; index ++) {
            IkasanFlowComponent flowElement = flowElementList.get(index);

            flowElement.getViewHandler().paintComponent(canvas, g, -1, -1);

            if (index < flowSize-1) {
                connectComponents(g, flowElement.getViewHandler(), flowElementList.get(index+1).getViewHandler());
            }
        }

        if (flowSize > 1) {
            ViewHandler first = flowElementList.get(0).getViewHandler();
            ViewHandler last = flowElementList.get(flowSize-1).getViewHandler();
            if (model.getInput() != null) {
                ViewHandler vh = model.getInput().getViewHandler();
                vh.setWidth(first.getWidth());
                vh.setTopY(first.getTopY());
                vh.setLeftX(first.getLeftX() - FLOW_X_SPACING - FLOW_CONTAINER_BORDER - vh.getWidth());
                vh.paintComponent(canvas, g, -1, -1);
                connectComponents(g, vh, first);
            }
            if (model.getOutput() != null) {
                ViewHandler vh = model.getOutput().getViewHandler();
                vh.setWidth(last.getWidth());
                vh.setTopY(last.getTopY());
                vh.setLeftX(last.getLeftX() + vh.getWidth() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
                vh.paintComponent(canvas, g, -1, -1);
                connectComponents(g, last, vh);
            }
        }
        StudioUIUtils.setLine(g,1f);
        return getBottomY();
    }

    private void connectComponents(Graphics g, ViewHandler start, ViewHandler end) {
        g.drawLine(
                start.getRightConnectorPoint().x,
                start.getRightConnectorPoint().y,
                end.getLeftConnectorPoint().x,
                end.getLeftConnectorPoint().y);
    }

    private int getYAfterPaintingFlowTitle(Graphics g) {
        return paintFlowTitle(g, PaintMode.DIMENSION_ONLY) + FLOW_Y_TITLE_SPACING;
    }

    /**
     * Look at the current components and work out the required x, y , width and height of this container
     * @param graphics object
     * @param newLeftx to use
     * @param newTopY to use
     * @param width of container which may be ignored if it is set by the component
     * @param height of container which may be ignored if it is set by the component
     */
    public void initialiseDimensions(Graphics graphics, int newLeftx, int newTopY, int width, int height) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        if (width != -1) {
            setWidth(width); // We initialise the width twice, first to prevent constraints
        }
        int currentX = newLeftx + FLOW_CONTAINER_BORDER;
        int topYForElements = getYAfterPaintingFlowTitle(graphics);
        if (model.getFlowComponentList().size() > 0) {
            for (IkasanFlowComponent ikasanFlowComponent : model.getFlowComponentList()) {
                ikasanFlowComponent.getViewHandler().initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                currentX += ikasanFlowComponent.getViewHandler().getWidth() + FLOW_X_SPACING;
            }
        }
        setWidthHeights(graphics, newTopY);
    }

//    public void initialiseDimensionsNotChildren(Graphics g, int newLeftx, int newTopY, int width, int height) {
    public void initialiseDimensionsNotChildren(Graphics graphics, int newLeftx, int newTopY) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        setWidthHeights(graphics, newTopY);
    }

    private void setWidthHeights(Graphics graphics, int newTopY) {
        if (model.getFlowComponentList().size() > 0) {
            setWidth(getFlowElementsWidth() + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getFlowElementsBottomY() + FLOW_CONTAINER_BORDER - newTopY);
        } else {
            setWidth(getFlowTitleWidth(graphics) + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getFlowTitleHeight(graphics) + (2 * FLOW_Y_TITLE_SPACING));
        }
    }

    public int getFlowElementsWidth() {
        return getFlowElementsRightX() - getFlowElementsLeftX();
    }

    public int getFlowElementsTopY() {
        int minX = model.getFlowComponentList().stream().mapToInt(x -> x.getViewHandler().getTopY()).min().orElse(0);
        return minX;
    }

    public int getFlowElementsLeftX() {
        // if there are no elements, should we use title

        int minX = model.getFlowComponentList().stream().mapToInt(x -> x.getViewHandler().getLeftX()).min().orElse(0);
        return minX;
    }

    public int getFlowElementsRightX() {
        return model.getFlowComponentList().stream().mapToInt(x -> x.getViewHandler().getRightX()).max().orElse(0);
    }
    public int getFlowElementsBottomY() {
        return model.getFlowComponentList().stream().mapToInt(x -> x.getViewHandler().getBottomY()).max().orElse(0);
    }
}
