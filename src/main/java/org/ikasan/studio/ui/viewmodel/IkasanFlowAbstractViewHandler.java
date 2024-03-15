package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import org.ikasan.studio.build.model.ikasan.instance.Flow;
import org.ikasan.studio.build.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.ikasan.studio.ui.StudioUIUtils.getBoldFont;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowAbstractViewHandler extends AbstractViewHandler {
    public static final int FLOW_X_SPACING = 30;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    private static final Logger LOG = Logger.getInstance("#IkasanFlowAbstractViewHandler");

    private Color borderColor = JBColor.BLACK;
    private String warningText =  "";
    private int warningX = 0;
    private int warningY = 0;
    private final Flow flow;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flow for view handler
     */
    public IkasanFlowAbstractViewHandler(Flow flow) {
        this.flow = flow;
    }

    public String getText() {
        return flow.getName() != null ? flow.getName() : flow.getDescription();
    }

    private int getFlowTitleWidth(Graphics g) {
        return StudioUIUtils.getTextWidth(g, getText());
    }

    private int getFlowTitleHeight(Graphics g) {
        return StudioUIUtils.getTextHeight(g);
    }

    private void paintFlowRectangle(Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        // Central rectangle
        g.setColor(StudioUIUtils.IKASAN_GREY);
        g.fillRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(oldColor);
    }

    private void paintFlowBorder(Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        g.setColor(borderColor);
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.drawRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(oldColor);
    }

    private int paintFlowTitle(Graphics g, PaintMode paintMode) {
        return StudioUIUtils.drawCenteredStringFromTopCentre
                (g, paintMode, getText(), getLeftX() + (getWidth() / 2), getTopY() + FLOW_CONTAINER_BORDER, getWidth(), getBoldFont(g));
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

    private void paintFlowBox(Graphics g) {
        paintFlowRectangle(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowBorder(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowTitle(g, PaintMode.PAINT);
    }

    public int paintComponent(JPanel canvas, Graphics g, int minimumLeftX, int minimumTopY) {
        LOG.debug("paintComponent invoked");
        int newLeftX = getNewCoord(minimumLeftX, getLeftX());
        int newTopY = getNewCoord(minimumTopY, getTopY());

        if (newLeftX != getLeftX() || newTopY != getTopY()) {
            initialiseDimensions(g, newLeftX, newTopY,-1, -1);
        } else {
            initialiseDimensionsNotChildren(g, newLeftX, newTopY);
        }
        paintFlowBox(g);
        if (flow.hasExceptionResolver()) {
            StudioUIUtils.getViewHandler(flow.getExceptionResolver()).paintComponent(canvas, g, -1, -1);
        }
        List<FlowElement> flowElementList = flow.ftlGetConsumerAndFlowElements();
        int flowSize = flowElementList.size();
        StudioUIUtils.setLine(g, 2f);

        // Paint any components between the first and the last
        for (int index=0; index < flowSize; index ++) {
            FlowElement flowElement = flowElementList.get(index);
            StudioUIUtils.getViewHandler(flowElement).paintComponent(canvas, g, -1, -1);
            if (index < flowSize-1) {
                drawConnector(g, StudioUIUtils.getViewHandler(flowElement), StudioUIUtils.getViewHandler(flowElementList.get(index+1)));
            }
        }

        // This section draws the symbold before the start and after the end of the flow to represent the input/output boxes.
        if (flowSize > 1) {
            // The input symbol, typically a queue gets painted before the flow start
            // @TDOD use a UI utils to determine this each time of render.
//            if (flow.getInputEndPoint() != null) {
//                AbstractViewHandler first = flowElementList.get(0).getViewHandler();
//                AbstractViewHandler inputSymbolvh = flow.getInputEndPoint().getViewHandler();
//                inputSymbolvh.setWidth(first.getWidth());
//                inputSymbolvh.setTopY(first.getTopY());
//                inputSymbolvh.setLeftX(first.getLeftX() - FLOW_X_SPACING - FLOW_CONTAINER_BORDER - inputSymbolvh.getWidth());
//                inputSymbolvh.paintComponent(canvas, g, -1, -1);
//                drawConnector(g, inputSymbolvh, first);
//            }
//            // The output symbol, typically gets painted after the flow right edge.
//            if (flow.getOutputEndPoint() != null) {
//                AbstractViewHandler last = flowElementList.get(flowSize-1).getViewHandler();
//                AbstractViewHandler vh = flow.getOutputEndPoint().getViewHandler();
//                vh.setWidth(last.getWidth());
//                vh.setTopY(last.getTopY());
//                vh.setLeftX(last.getLeftX() + vh.getWidth() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
//                vh.paintComponent(canvas, g, -1, -1);
//                drawConnector(g, last, vh);
//            }
        }
        StudioUIUtils.setLine(g,1f);

        // The warning must always have the highest z order.
        StudioUIUtils.paintWarningPopup(g, warningX, warningY, canvas.getX() + canvas.getWidth(), canvas.getY() + canvas.getHeight(), warningText);
        return getBottomY();
    }

    private void drawConnector(Graphics g, AbstractViewHandler start, AbstractViewHandler end) {
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
    @Override
    public void initialiseDimensions(Graphics graphics, int newLeftx, int newTopY, int width, int height) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        if (width != -1) {
            setWidth(width); // We initialise the width twice, first to prevent constraints
        }
        int currentX = newLeftx + FLOW_CONTAINER_BORDER;
        int topYForElements = getYAfterPaintingFlowTitle(graphics);
        List<FlowElement> flowElementList = flow.ftlGetConsumerAndFlowElements();
        if (!flowElementList.isEmpty()) {
            for (FlowElement ikasanFlowComponent : flowElementList) {
                StudioUIUtils.getViewHandler(ikasanFlowComponent).initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                currentX += StudioUIUtils.getViewHandler(ikasanFlowComponent).getWidth() + FLOW_X_SPACING;
            }
        }
        setWidthHeights(graphics, newTopY);
        if (flow.hasExceptionResolver()) {
            StudioUIUtils.getViewHandler(flow.getExceptionResolver()).initialiseDimensions(graphics,
                    IkasanFlowExceptionResolverAbstractViewHandler.getXOffsetFromRight(getRightX()),
                    IkasanFlowExceptionResolverAbstractViewHandler.getYOffsetFromTop(getTopY()),
                    -1, -1);
        }
    }

    public void initialiseDimensionsNotChildren(Graphics graphics, int newLeftx, int newTopY) {
        setLeftX(newLeftx);
        setTopY(newTopY);
        setWidthHeights(graphics, newTopY);
    }

    private void setWidthHeights(Graphics graphics, int newTopY) {
        if (!flow.ftlGetConsumerAndFlowElements().isEmpty()) {
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
        return flow.ftlGetConsumerAndFlowElements().stream().mapToInt(x -> StudioUIUtils.getViewHandler(x).getTopY()).min().orElse(0);
    }

    public int getFlowElementsLeftX() {
        return flow.ftlGetConsumerAndFlowElements().stream().mapToInt(x -> StudioUIUtils.getViewHandler(x).getLeftX()).min().orElse(0);
    }

    public int getFlowElementsRightX() {
        return flow.ftlGetConsumerAndFlowElements().stream().mapToInt(x -> StudioUIUtils.getViewHandler(x).getRightX()).max().orElse(0);
    }
    public int getFlowElementsBottomY() {
        return flow.ftlGetConsumerAndFlowElements().stream().mapToInt(x -> StudioUIUtils.getViewHandler(x).getBottomY()).max().orElse(0);
    }

    public void setFlowReceptiveMode() {
        this.warningText = "";
        this.borderColor = JBColor.GREEN;
    }

    public boolean isFlowReceptiveMode() {
        return Color.GREEN.equals(this.borderColor);
    }

    public void setFlowlWarningMode(int mouseX, int mouseY, String message) {
        this.warningText = message;
        this.warningX = mouseX;
        this.warningY = mouseY;
        this.borderColor = JBColor.RED;
    }

    public boolean isFlowWarningMode() {
        return Color.RED.equals(this.borderColor);
    }
    public void setFlowNormalMode() {
        this.warningText = "";
        this.borderColor = JBColor.BLACK;
    }

    public boolean isFlowNormalMode() {
        return Color.BLACK.equals(this.borderColor);
    }
}
