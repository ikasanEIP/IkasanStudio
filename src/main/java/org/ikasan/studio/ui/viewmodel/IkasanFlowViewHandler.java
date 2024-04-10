package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;

import static org.ikasan.studio.ui.StudioUIUtils.getBoldFont;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
@Getter
public class IkasanFlowViewHandler extends AbstractViewHandlerIntellij {
    IkasanFlowRouteViewHandler flowRouteViewHandler;
    private final String projectKey;
    public static final int FLOW_X_SPACING = 30;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    private static final Logger LOG = Logger.getInstance("#IkasanFlowViewHandler");

    private Color borderColor = JBColor.BLACK;
    private String warningText =  "";
    private int warningX = 0;
    private int warningY = 0;
    private final Flow flow;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flow for view handler
     */
    public IkasanFlowViewHandler(String projectKey, Flow flow) {
        this.projectKey = projectKey;
        this.flow = flow;
        this.flowRouteViewHandler = new IkasanFlowRouteViewHandler(projectKey, flow, flow.getFlowRoute());
    }

    @Override
    public String getText() {
        return flow.getName() != null ? flow.getName() : flow.getDescription();
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

    private void paintFlowBox(Graphics g) {
        paintFlowRectangle(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowBorder(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowTitle(g, PaintMode.PAINT);
    }

    /**
     * Paint the flow itself and all the components within (technically, the view handler of each component will paint the
     * component itseld
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumLeftX of the flow
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return the bottom Y co-ordinate of this flow.
     */
    public int paintComponent(JPanel canvas, Graphics g, int minimumLeftX, int minimumTopY) {
        LOG.debug("paintComponent invoked");
        int newLeftX = checkForReset(minimumLeftX, getLeftX());
        int newTopY = checkForReset(minimumTopY, getTopY());

        // This will also call initialise for route(s)
        initialiseDimensions(g, newLeftX, newTopY,-1, -1);

        paintFlowBox(g);

        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getOrCreateAbstractViewHandler(projectKey, flow.getExceptionResolver());
            if (viewHandler != null) {
                viewHandler.paintComponent(canvas, g, -1, -1);
            }
        }
        flowRouteViewHandler.paintRoute(canvas, g, flow.getFlowRoute(), null);

        // The warning must always have the highest z order.
        StudioUIUtils.paintWarningPopup(g, warningX, warningY, canvas.getX() + canvas.getWidth(), canvas.getY() + canvas.getHeight(), warningText);
        return getBottomY();
    }


//    private void drawConnector(Graphics g, AbstractViewHandlerIntellij start, AbstractViewHandlerIntellij end) {
//        g.drawLine(
//                start.getRightConnectorPoint().x,
//                start.getRightConnectorPoint().y,
//                end.getLeftConnectorPoint().x,
//                end.getLeftConnectorPoint().y);
//    }

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
            setWidth(width); // We initialise the width twice, first time is to prevent constraints
        }
        int currentX = newLeftx + FLOW_CONTAINER_BORDER;
        int topYForElements = getYAfterPaintingFlowTitle(graphics);

        flowRouteViewHandler.initialiseDimensions(graphics, currentX, topYForElements);

        setWidthAndHeights(graphics);

        // Needs to be after all routes of flow so its rightmost
        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getOrCreateAbstractViewHandler(projectKey, flow.getExceptionResolver());
            if (viewHandler != null) {
                viewHandler.initialiseDimensions(
                    graphics,
                    IkasanFlowExceptionResolverViewHandler.getXOffsetFromRight(getRightX()),
                    IkasanFlowExceptionResolverViewHandler.getYOffsetFromTop(getTopY()),
                    -1, -1);
            }
        }
    }

    private void setWidthAndHeights(Graphics graphics)  {
        if (flowRouteViewHandler != null) {
            setWidth(flowRouteViewHandler.getAllRouteMaxX(0) - flowRouteViewHandler.getFlowElementsMinX() + (2 * FLOW_CONTAINER_BORDER));
            setHeight(flowRouteViewHandler.getAllRouteMaxY(graphics, 0) - flowRouteViewHandler.getFlowElementsMinY() + (2 * FLOW_CONTAINER_BORDER) + getTextHeight(graphics));
        } else {
            setWidth(getTextWidth(graphics) + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getTextHeight(graphics) + (2 * FLOW_Y_TITLE_SPACING));
        }
    }

    /**
     * The flow is willing to accept a component that has been dragged to it
     */
    public void setFlowReceptiveMode() {
        this.warningText = "";
        this.borderColor = JBColor.GREEN;
    }

    public boolean isFlowReceptiveMode() {
        return Color.GREEN.equals(this.borderColor);
    }

    /***
     * Typically this appears when attempting to drag in a component into a flow where it is not allowed
     * @param mouseX to display the warning
     * @param mouseY to display the warning
     * @param message to appear
     */
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
