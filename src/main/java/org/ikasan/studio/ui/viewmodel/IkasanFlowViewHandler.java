package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowElement;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.UIUtils;

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
     * The model can be null e.g. for a pallette item, once dragged onto a canvas, the model would be populated.
     * @param model
     */
    public IkasanFlowViewHandler(IkasanFlow model) {
        this.model = model;
    }

    public String getText() {
        return model.getName() != null ? model.getName() : model.getDescription();
    }

    private int paintFlowTitle(Graphics g, PaintMode paintMode) {
        return UIUtils.drawCenteredStringFromTopCentre
                (g, paintMode, getText(), getLeftX() + (getWidth() / 2), getTopY() + FLOW_CONTAINER_BORDER, getWidth(), UIUtils.getBoldFont( g));
    }

    private void paintIkasanFlowContainer(Graphics g, int x, int y, int width, int height) {
        log.debug("paintIkasanFlowContainer invoked");
//    private void paintIkasanFlowContainer(Graphics g) {

        log.info("paintIkasanFlowContainer x [" + x + "] y [" + y + "] width [" + width + "] height [" + height + "]");
        g.setColor(UIUtils.IKASAN_GREY);
        g.fillRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(Color.BLACK);
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.drawRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
    }


    private int getNewCoord(int reset, int current) {
        if (reset > -1 && reset != current) {
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
            initialiseDimensionsNotChildren(g, newLeftX, newTopY,-1, -1);
        }
        paintIkasanFlowContainer(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowTitle(g, PaintMode.PAINT);

        List<IkasanFlowElement> flowElementList = model.getFlowElementList();
        int flowSize = flowElementList.size();
        UIUtils.setLine(g, 2f);
        for (int index=0; index < flowSize; index ++) {
            IkasanFlowElement flowElement = flowElementList.get(index);

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


        UIUtils.setLine(g,1f);
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

    public void initialiseDimensions(Graphics g, int newLeftx, int newTopY, int width, int height) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        if (width != -1) {
            setWidth(width); // We initialise the width twice, first to prevent constrains
        }
        int currentX = newLeftx + FLOW_CONTAINER_BORDER;
        int topYForElements = getYAfterPaintingFlowTitle(g);
        for (IkasanFlowElement ikasanFlowElement : model.getFlowElementList()) {
            ikasanFlowElement.getViewHandler().initialiseDimensions(g, currentX, topYForElements, -1, -1);
            currentX += ikasanFlowElement.getViewHandler().getWidth() + FLOW_X_SPACING;
        }
        setWidth(getFlowElementsWidth() + (2 * FLOW_CONTAINER_BORDER));
        setHeight(getFlowElementsBottomY() + FLOW_CONTAINER_BORDER - newTopY);
    }

    public void initialiseDimensionsNotChildren(Graphics g, int newLeftx, int newTopY, int width, int height) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        setWidth(getFlowElementsWidth() + (2 * FLOW_CONTAINER_BORDER));
        setHeight(getFlowElementsBottomY() + FLOW_CONTAINER_BORDER - newTopY);
    }


    public int getFlowElementsWidth() {
        return getFlowElementsRightX() - getFlowElementsLeftX();
    }

    public int getFlowElementsTopY() {
        int minX = model.getFlowElementList().stream().mapToInt(x -> x.getViewHandler().getTopY()).min().orElse(0);
        return minX;
    }

    public int getFlowElementsLeftX() {
        int minX = model.getFlowElementList().stream().mapToInt(x -> x.getViewHandler().getLeftX()).min().orElse(0);
        return minX;
    }

    public int getFlowElementsRightX() {
        int maxX = model.getFlowElementList().stream().mapToInt(x -> x.getViewHandler().getRightX()).max().orElse(0);
        return maxX;
    }
    public int getFlowElementsBottomY() {
        int maxX = model.getFlowElementList().stream().mapToInt(x -> x.getViewHandler().getBottomY()).max().orElse(0);
        return maxX;
    }
}
