package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.ikasan.studio.ui.StudioUIUtils.getBoldFont;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanFlowViewHandler extends AbstractViewHandlerIntellij {
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
        int newLeftX = getNewCoord(minimumLeftX, getLeftX());
        int newTopY = getNewCoord(minimumTopY, getTopY());

        initialiseDimensions(g, newLeftX, newTopY,-1, -1);

        paintFlowBox(g);

        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getAbstracttViewHandler(projectKey, flow.getExceptionResolver());
            if (viewHandler != null) {
                viewHandler.paintComponent(canvas, g, -1, -1);
            }
        }
        paintRoute(canvas, g, flow.getFlowRoute());

        // The warning must always have the highest z order.
        StudioUIUtils.paintWarningPopup(g, warningX, warningY, canvas.getX() + canvas.getWidth(), canvas.getY() + canvas.getHeight(), warningText);
        return getBottomY();
    }

    private void paintRoute(JPanel canvas, Graphics g, FlowRoute flowRoute) {
        List<FlowElement> flowAndConsumerElementList = flowRoute.ftlGetConsumerAndFlowElements();
        int flowSize = flowAndConsumerElementList.size();
        StudioUIUtils.setLine(g, 2f);

        // Paint any components between the first and the last
        for (int index=0; index < flowSize; index ++) {
            FlowElement flowElement = flowAndConsumerElementList.get(index);

            IkasanFlowComponentViewHandler flowComponentViewHandler = getFlowComponentViewHandler(projectKey, flowElement);
            if (flowComponentViewHandler != null) {
                flowComponentViewHandler.paintComponent(canvas, g, -1, -1);
            }
            if (index < flowSize-1) {
                IkasanFlowComponentViewHandler nextFlowComponentViewHandler = getFlowComponentViewHandler(projectKey, flowAndConsumerElementList.get(index + 1));
                if (flowComponentViewHandler != null && nextFlowComponentViewHandler != null) {
                    if (index < flowSize - 1) {
                        drawConnector(g, flowComponentViewHandler, nextFlowComponentViewHandler);
                    }
                }
            }
        }

        // This section draws the symbols before the start and after the end of the flow to represent the input/output boxes.
        if (flowSize > 1) {
            // The input symbol, typically a queue gets painted before the flow start
            displayEndpointIfExists(canvas, g, flow.getConsumer());
            // A flow might have multiple producers
            List<FlowElement> flowElementList = flow.getFlowRoute().getFlowElements();
            if (flowElementList != null && !flowElementList.isEmpty()) {
                List <FlowElement> producers = flowElementList.stream().filter(f->f.getComponentMeta().isProducer()).toList();
                for (FlowElement producer : producers) {
                    displayEndpointIfExists(canvas, g, producer);
                }
            }
        }
        StudioUIUtils.setLine(g,1f);
        if (flowRoute.getChildRoutes() != null) {
            for(FlowRoute flowRoute1 : flowRoute.getChildRoutes()) {
                paintRoute(canvas, g, flowRoute1);
            }
        }
    }

    private void displayEndpointIfExists(JPanel canvas, Graphics g, FlowElement targetFlowElement) {

        if (targetFlowElement != null) {
            String endpointComponentName = targetFlowElement.getComponentMeta().getEndpointKey();
            if (endpointComponentName != null) {
                // Get the text to be displayed under the endpoint symbol
                String endpointTextKey = targetFlowElement.getComponentMeta().getEndpointTextKey();
                ComponentProperty propertyValueToDisplay = targetFlowElement.getComponentProperties().get(endpointTextKey);
                String endpointText = "";
                if (propertyValueToDisplay != null) {
                    endpointText = propertyValueToDisplay.getValueString();
                }
                ComponentMeta endpointComponentMeta = null;
                FlowElement endpointFlowElement = null;
                try {
                    // Create the endpoint symbol instance
                    endpointComponentMeta = IkasanComponentLibrary.getIkasanComponentByKey(UiContext.getIkasanModule(projectKey).getMetaVersion(), endpointComponentName);
                    endpointFlowElement = FlowElementFactory.createFlowElement(UiContext.getIkasanModule(projectKey).getMetaVersion(), endpointComponentMeta, flow, endpointText);
                } catch (StudioBuildException se) {
                    LOG.warn("A studio exception was raised, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
                }

                if (endpointComponentMeta == null || endpointFlowElement == null) {
                    LOG.warn("Expected to find endpoint named " + endpointComponentName + " but endpointComponentMeta was " + endpointComponentMeta + " and endpointFlowElement was " + endpointFlowElement);
                } else {
                    // Position and draw the endpoint
                    IkasanFlowComponentViewHandler targetFlowElementViewHandler = getFlowComponentViewHandler(projectKey, targetFlowElement);
                    IkasanFlowComponentViewHandler endpointViewHandler = getFlowComponentViewHandler(projectKey, endpointFlowElement);
                    if (targetFlowElementViewHandler != null && endpointViewHandler!= null) {

                        endpointViewHandler.setWidth(targetFlowElementViewHandler.getWidth());
                        endpointViewHandler.setTopY(targetFlowElementViewHandler.getTopY());
                        if (targetFlowElement.getComponentMeta().isConsumer()) {
                            endpointViewHandler.setLeftX(targetFlowElementViewHandler.getLeftX() - FLOW_X_SPACING - FLOW_CONTAINER_BORDER - endpointViewHandler.getWidth());
                            endpointViewHandler.paintComponent(canvas, g, -1, -1);
                            drawConnector(g, endpointViewHandler, targetFlowElementViewHandler);
                        } else {
                            endpointViewHandler.setLeftX(targetFlowElementViewHandler.getLeftX() + endpointViewHandler.getWidth() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
                            endpointViewHandler.paintComponent(canvas, g, -1, -1);
                            drawConnector(g, targetFlowElementViewHandler, endpointViewHandler);
                        }
                    }
                }
            }
        }
    }

    private void drawConnector(Graphics g, AbstractViewHandlerIntellij start, AbstractViewHandlerIntellij end) {
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
            setWidth(width); // We initialise the width twice, first time is to prevent constraints
        }
        int currentX = newLeftx + FLOW_CONTAINER_BORDER;
        int topYForElements = getYAfterPaintingFlowTitle(graphics);

        initilaiseDimensionsForRoute(graphics, currentX, topYForElements, newTopY, flow.getFlowRoute());

        // Needs to be after all routes of flow so its rightmost
        if (flow.hasExceptionResolver()) {
            AbstractViewHandlerIntellij viewHandler = getAbstracttViewHandler(projectKey, flow.getExceptionResolver());
            if (viewHandler != null) {
                viewHandler.initialiseDimensions(
                    graphics,
                    IkasanFlowExceptionResolverViewHandler.getXOffsetFromRight(getRightX()),
                    IkasanFlowExceptionResolverViewHandler.getYOffsetFromTop(getTopY()),
                    -1, -1);
            }
        }
    }

    private void initilaiseDimensionsForRoute(Graphics graphics, int currentX, int topYForElements, int newTopY, FlowRoute flowRoute) {
        List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (!flowElementList.isEmpty()) {
            for (FlowElement ikasanFlowComponent : flowElementList) {
                AbstractViewHandlerIntellij viewHandler = getAbstracttViewHandler(projectKey, ikasanFlowComponent);
                if (viewHandler != null) {
                    viewHandler.initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                    currentX += viewHandler.getWidth() + FLOW_X_SPACING;
                }
            }
        }
        setWidthAndHeights(graphics, newTopY, flowRoute);

        if (flowRoute.getChildRoutes() != null) {
            for(FlowRoute flowRoute1 : flowRoute.getChildRoutes()) {
                initilaiseDimensionsForRoute(graphics, currentX, topYForElements, newTopY, flowRoute1);
            }
        }
    }

//    public void initialiseDimensionsNotChildren(Graphics graphics, int newLeftx, int newTopY) {
//        setLeftX(newLeftx);
//        setTopY(newTopY);
//        setWidthAndHeights(graphics, newTopY, null);
//    }

    private void setWidthAndHeights(Graphics graphics, int newTopY, FlowRoute flowRoute)  {
        if (flowRoute != null && ! flowRoute.ftlGetConsumerAndFlowElements().isEmpty()) {
            setWidth(getFlowElementsWidth(flowRoute) + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getFlowElementsBottomY(flowRoute) + FLOW_CONTAINER_BORDER - newTopY);
        } else {
            setWidth(getFlowTitleWidth(graphics) + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getFlowTitleHeight(graphics) + (2 * FLOW_Y_TITLE_SPACING));
        }
    }

    public int getFlowElementsWidth(FlowRoute flowRoute)  {
        return getFlowElementsRightX(flowRoute) - getFlowElementsLeftX(flowRoute);
    }

    /**
     * For a given flowRoute, the first element will be the leftmost
     * @return the smallest (most lext) x value for the route
     */
    public int getFlowElementsLeftX(FlowRoute flowRoute) {
        List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            return getAbstracttViewHandler(projectKey, flowElementList.get(0)).getLeftX();
        }
    }

    public int getFlowElementsRightX(FlowRoute flowRoute) {
        List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            return getAbstracttViewHandler(projectKey, flowElementList.get(flowElementList.size()-1)).getRightX();
        }
    }

    /**
     * For the given route find the one with the largest Y valye
     * @return the larget Y
     */
    public int getFlowElementsBottomY(FlowRoute flowRoute)  {
        int bottomY = 0 ;
        List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            for (FlowElement flowElement : flowElementList) {
                int flowY = getAbstracttViewHandler(projectKey, flowElement).getBottomY();
                bottomY = Math.max(flowY, bottomY);
            }
        }
        return bottomY;
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
