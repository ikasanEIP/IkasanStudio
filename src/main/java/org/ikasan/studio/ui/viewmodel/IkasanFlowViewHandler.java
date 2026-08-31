package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.Styling;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
import java.awt.*;

import static org.ikasan.studio.ui.StudioUIUtils.getBoldFont;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
@Getter
public class IkasanFlowViewHandler extends AbstractViewHandlerIntellij {
    IkasanFlowRouteViewHandler flowRouteViewHandler;
    private final Project project;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    private static final Logger LOG = Logger.getInstance("#IkasanFlowViewHandler");

    /**
     * Transient drag-and-drop feedback, overlaid on top of the flow's validity colour while a component is
     * being dragged over the canvas. NORMAL means "no drag feedback active" - the border colour then falls
     * back to reflecting whether the flow is valid (has both a consumer and a producer).
     */
    private enum DragFeedbackMode { NORMAL, RECEPTIVE, WARNING }
    private DragFeedbackMode dragFeedbackMode = DragFeedbackMode.NORMAL;
    private String warningText =  "";
    private int warningX = 0;
    private int warningY = 0;
    private final Flow flow;

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBackgroundColor() {
        return ThemeAwareColors.getBackgroundColor();
    }

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flow for view handler
     */
    public IkasanFlowViewHandler(Project project, Flow flow) {
        this.project = project;
        this.flow = flow;
        this.flowRouteViewHandler = new IkasanFlowRouteViewHandler(project, flow, flow.getFlowRoute());
    }

    @Override
    public String getText() {
        return flow.getIdentity() != null ? flow.getIdentity() : flow.getDescription();
    }

    /**
     * If the click coordinates fall within a rendered external endpoint icon for this flow, return the
     * owning consumer or producer FlowElement so the properties panel can display it. Returns null if
     * the click does not hit any endpoint.
     */
    public FlowElement getOwnerForEndpointAtXY(int x, int y) {
        return flowRouteViewHandler.getOwnerForEndpointAtXY(x, y);
    }

    /**
     * The already-positioned view handler for a consumer/producer's externally-drawn "channel endpoint" pill -
     * see {@link IkasanFlowRouteViewHandler#getEndpointViewHandlerForOwner}. Returns null if owner has no
     * external endpoint, isn't in this flow, or hasn't been painted yet this cycle.
     */
    public IkasanFlowComponentViewHandler getEndpointViewHandlerFor(FlowElement owner) {
        return flowRouteViewHandler.getEndpointViewHandlerForOwner(owner);
    }

    /**
     * If the click coordinates fall within a rendered Send Test Message badge for this flow, return the
     * owning Consumer FlowElement. Returns null if the click does not hit any badge.
     */
    public FlowElement getOwnerForSendTestMessageAtXY(int x, int y) {
        return flowRouteViewHandler.getOwnerForSendTestMessageAtXY(x, y);
    }

    private void paintFlowRectangle(Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        // Central rectangle
        g.setColor(getThemeAwareBackgroundColor());
        g.fillRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(oldColor);
    }

    /**
     * While a component is being dragged over this flow, the border colour reflects whether dropping here is
     * currently allowed (green) or not (red). Otherwise the border is the normal, neutral colour (black in light
     * theme, the Ikasan orange in dark theme, matching the rest of the Ikasan component icons) - colour is
     * otherwise reserved exclusively for drag-and-drop drop-target feedback.
     */
    private Color getFlowBorderColor() {
        return switch (dragFeedbackMode) {
            case RECEPTIVE -> JBColor.GREEN;
            case WARNING -> JBColor.RED;
            case NORMAL -> ThemeAwareColors.isDarkTheme() ? Styling.IKASAN_ORANGE : JBColor.BLACK;
        };
    }

    /**
     * A runnable (valid) flow - one with both a consumer and a producer, see
     * {@link Flow#getFlowIntegrityStatus()} - is drawn with a solid border; an incomplete flow keeps the dashed
     * border to signal it is still a work in progress.
     */
    private Stroke getFlowBorderStroke() {
        return flow.getFlowIntegrityStatus().isBlank()
                ? new BasicStroke(3)
                : new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    }

    private void paintFlowBorder(Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        g.setColor(getFlowBorderColor());
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setStroke(getFlowBorderStroke());
        g2d.drawRoundRect(x, y, width, height, CONTAINER_CORNER_ARC, CONTAINER_CORNER_ARC);
        g.setColor(oldColor);
    }

    private int paintFlowTitle(Graphics g, PaintMode paintMode) {
        return StudioUIUtils.drawCenteredStringFromTopCentre
                (g, paintMode, getText(), getLeftX() + (getWidth() / 2), getTopY() + FLOW_CONTAINER_BORDER, getWidth(), getBoldFont());
    }

    private void paintFlowBox(JPanel canvas, Graphics g) {
        paintFlowRectangle(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowBorder(g, getLeftX(), getTopY(), getWidth(), getHeight());
        paintFlowTitle(g, PaintMode.PAINT);
        if (isRecording(flow)) {
            IkasanComponentLibrary.getReplayServiceIcon().paintIcon(canvas, g,
                    getLeftX() + FLOW_CONTAINER_BORDER,
                    getTopY() + FLOW_CONTAINER_BORDER);
        }
//        LOG.info("StudioXX: planted flow " + this.getFlow().getIdentity() + " flow box TestV1:"  + getLeftX() + " y:" + getTopY() + " width:" + getWidth() + " height:" + getHeight());
    }

    static boolean isRecording(Flow flow) {
        Object value = flow != null ? flow.getPropertyValue("isRecording") : null;
        return value instanceof Boolean booleanValue
                ? booleanValue
                : value != null && Boolean.parseBoolean(value.toString());
    }

    /**
     * Paint the flow itself and all the components within (technically, the view handler of each component will paint the
     * component itself)
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumLeftX of the flow
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return the bottom Y co-ordinate of this flow.
     */
    public int paintComponent(JPanel canvas, Graphics g, int minimumLeftX, int minimumTopY) {
        LOG.debug("Studio: paintComponent invoked");
        int newLeftX = checkForReset(minimumLeftX, getLeftX());
        int newTopY = checkForReset(minimumTopY, getTopY());

        // This will also call initialise for route(s)
        initialiseDimensions(g, newLeftX, newTopY,-1, -1);

        // Draw flow rectangle
        paintFlowBox(canvas, g);

        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getOrCreateAbstractViewHandler(project, flow.getExceptionResolver());
            if (viewHandler != null) {
                viewHandler.paintComponent(canvas, g, -1, -1);
            }
        }
        flowRouteViewHandler.paintRoute(canvas, g, flow.getFlowRoute(), null);

        // The warning must always have the highest z order.
        StudioUIUtils.paintWarningPopup(g, warningX, warningY, canvas.getX() + canvas.getWidth(), canvas.getY() + canvas.getHeight(), warningText);
        return getBottomY();
    }


    private int getYAfterPaintingFlowTitle(Graphics g) {
        return paintFlowTitle(g, PaintMode.DIMENSION_ONLY) + FLOW_Y_TITLE_SPACING;
    }

    /**
     * Look at the current components and work out the required TestV1, y , width and height of this container
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

        // returns topYForElements
        flowRouteViewHandler.initialiseDimensions(graphics, currentX, topYForElements);

        setWidthAndHeights(graphics);

        // Needs to be after all routes of flow so its rightmost
        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getOrCreateAbstractViewHandler(project, flow.getExceptionResolver());
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
        if (flowRouteViewHandler != null && (!flowRouteViewHandler.getFlowRoute().isEmpty() || flow.getConsumer() != null)) {
            setWidth(flowRouteViewHandler.getAllRouteMaxX(0) - flowRouteViewHandler.getFlowElementsMinX() + (2 * FLOW_CONTAINER_BORDER));
            setHeight(flowRouteViewHandler.getAllRouteMaxY(graphics, 0) - flowRouteViewHandler.getFlowElementsMinY() + (2 * FLOW_CONTAINER_BORDER) + getTextHeight(graphics));
        } else {
            // No flow routes, maybe an exception handler
            int width = getTextWidth(graphics) + (2 * FLOW_CONTAINER_BORDER);
            if (flow.hasExceptionResolver()) {
                AbstractViewHandlerIntellij exceptionResolverViewHandler = getOrCreateAbstractViewHandler(project, flow.getExceptionResolver());
                width += (exceptionResolverViewHandler.getWidth() * 2);
            }
            setWidth(width);
            setHeight(getTextHeight(graphics) + (2 * FLOW_Y_TITLE_SPACING));
        }
    }

    /**
     * The flow is willing to accept a component that has been dragged to it.
     * @return true if this changed the flow's drag-feedback state (so a repaint is needed), false if it was
     *         already in this mode
     */
    public boolean setFlowReceptiveMode() {
        if (dragFeedbackMode == DragFeedbackMode.RECEPTIVE) {
            return false;
        }
        this.warningText = "";
        this.dragFeedbackMode = DragFeedbackMode.RECEPTIVE;
        return true;
    }

    /***
     * Typically this appears when attempting to drag in a component into a flow where it is not allowed
     * @param mouseX to display the warning
     * @param mouseY to display the warning
     * @param message to appear
     * @return true if this changed the flow's drag-feedback state (so a repaint is needed), false if it was
     *         already in this mode
     */
    public boolean setFlowlWarningMode(int mouseX, int mouseY, String message) {
        if (dragFeedbackMode == DragFeedbackMode.WARNING) {
            return false;
        }
        this.warningText = message;
        this.warningX = mouseX;
        this.warningY = mouseY;
        this.dragFeedbackMode = DragFeedbackMode.WARNING;
        return true;
    }

    /**
     * A drag is in progress, but the mouse is not currently positioned over this flow (or over any flow at all),
     * so dropping right now would not target it. The border still needs to read as "not a valid drop location",
     * but unlike {@link #setFlowlWarningMode}, there is no specific issue to report, so no message popup is shown.
     * @return true if this changed the flow's drag-feedback state (so a repaint is needed), false if it was
     *         already in this mode
     */
    public boolean setFlowNotDropTargetMode() {
        if (dragFeedbackMode == DragFeedbackMode.WARNING) {
            return false;
        }
        this.warningText = "";
        this.dragFeedbackMode = DragFeedbackMode.WARNING;
        return true;
    }

    /**
     * @return true if this changed the flow's drag-feedback state (so a repaint is needed), false if it was
     *         already in this mode
     */
    public boolean setFlowNormalMode() {
        if (dragFeedbackMode == DragFeedbackMode.NORMAL) {
            return false;
        }
        this.warningText = "";
        this.dragFeedbackMode = DragFeedbackMode.NORMAL;
        return true;
    }

    /**
     * Perform any tidy up during deletion of this element
     */
    @Override
    public void dispose() {

    }
}
