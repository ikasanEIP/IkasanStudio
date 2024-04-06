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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ikasan.studio.ui.StudioUIUtils.getBoldFont;


/**
 * A flow route and a flow are similar for non Router flows i.e. the flow contains 1 flow route
 * When dealing with Multi-Recipient Routers, the flow will contain the same (default) flowRoute and that
 * Route will contain a list of components but will also contain 1..n childFlowRoutes (which themselves contain elements)
 */
public class IkasanFlowRouteViewHandler extends AbstractViewHandlerIntellij {
    private final String projectKey;
    public static final int FLOW_X_SPACING = 30;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    private static final Logger LOG = Logger.getInstance("#IkasanFlowViewHandler");

    private Color borderColor = JBColor.BLACK;
    private String warningText = "";
    private int warningX = 0;
    private int warningY = 0;
    private final Flow flow;
    private final FlowRoute flowRoute;
    private List<IkasanFlowRouteViewHandler> flowRouteViewHandlers = new ArrayList<>();

    /**
     * This view handler deals with a flowRoute or a child route (i.e. one of the branches off a multi-recipient router)
     *
     * @param projectKey to tie the scope
     * @param flow       that contains this flow route
     * @param flowRoute  which could be the default route or a sub-route (one of the branches off a multi-recipient router)
     */
    public IkasanFlowRouteViewHandler(String projectKey, Flow flow, FlowRoute flowRoute) {
        this.projectKey = projectKey;
        this.flow = flow;
        this.flowRoute = flowRoute;
        if (flowRoute.getChildRoutes() != null && flowRoute.getChildRoutes().size() > 0) {
            for (FlowRoute childRoute : flowRoute.getChildRoutes()) {
                flowRouteViewHandlers.add(new IkasanFlowRouteViewHandler(projectKey, flow, childRoute));
            }
        }
    }

    public String getText() {
        return flowRoute != null ? flowRoute.getRouteName() : "";
    }

    private int getFlowRouteTitleWidth(Graphics g) {
        return StudioUIUtils.getTextWidth(g, getText());
    }

    private int getFlowRouteTitleHeight(Graphics g) {
        return StudioUIUtils.getTextHeight(g);
    }

    private int paintRouteFlowTitle(Graphics g, PaintMode paintMode) {
        return StudioUIUtils.drawCenteredStringFromTopCentre
                (g, paintMode, getText(), getLeftX() + (getWidth() / 2), getTopY() + FLOW_CONTAINER_BORDER, getWidth(), getBoldFont(g));
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
        return minimumTopY;
    }

    protected void paintRoute(JPanel canvas, Graphics g, FlowRoute flowRoute) {
        java.util.List<FlowElement> flowAndConsumerElementList = flowRoute.ftlGetConsumerAndFlowElements();
        int flowSize = flowAndConsumerElementList.size();
        StudioUIUtils.setLine(g, 2f);

        // Paint any components between the first and the last
        for (int index = 0; index < flowSize; index++) {
            FlowElement flowElement = flowAndConsumerElementList.get(index);

            IkasanFlowComponentViewHandler flowComponentViewHandler = getOrCreateFlowComponentViewHandler(projectKey, flowElement);
            if (flowComponentViewHandler != null) {
                flowComponentViewHandler.paintComponent(canvas, g, -1, -1);
            }
            if (index < flowSize - 1) {
                IkasanFlowComponentViewHandler nextFlowComponentViewHandler = getOrCreateFlowComponentViewHandler(projectKey, flowAndConsumerElementList.get(index + 1));
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
            java.util.List<FlowElement> flowElementList = flow.getFlowRoute().getFlowElements();
            if (flowElementList != null && !flowElementList.isEmpty()) {
                java.util.List<FlowElement> producers = flowElementList.stream().filter(f -> f.getComponentMeta().isProducer()).toList();
                for (FlowElement producer : producers) {
                    displayEndpointIfExists(canvas, g, producer);
                }
            }
        }
        StudioUIUtils.setLine(g, 1f);
        if (flowRoute.getChildRoutes() != null) {
            for (FlowRoute flowRoute1 : flowRoute.getChildRoutes()) {
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
                    IkasanFlowComponentViewHandler targetFlowElementViewHandler = getOrCreateFlowComponentViewHandler(projectKey, targetFlowElement);
                    IkasanFlowComponentViewHandler endpointViewHandler = getOrCreateFlowComponentViewHandler(projectKey, endpointFlowElement);
                    if (targetFlowElementViewHandler != null && endpointViewHandler != null) {

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
        return paintRouteFlowTitle(g, PaintMode.DIMENSION_ONLY) + FLOW_Y_TITLE_SPACING;
    }

    @Override
    public void initialiseDimensions(Graphics graphics, int newLeftx, int newTopY, int width, int height) {
        Thread thread = Thread.currentThread();
        LOG.warn("SERIOUS: incorrect initialiseDimensions called for FlowRouteViewHandler :" + Arrays.asList(thread.getStackTrace()));
    }

    protected void initialiseDimensions(Graphics graphics, int currentX, int topYForElements, int newTopY) {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (!flowElementList.isEmpty()) {
            for (FlowElement ikasanFlowComponent : flowElementList) {
                AbstractViewHandlerIntellij viewHandler = getOrCreateAbstractViewHandler(projectKey, ikasanFlowComponent);
                if (viewHandler != null) {
                    viewHandler.initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                    currentX += viewHandler.getWidth() + FLOW_X_SPACING;
                }
            }
        }
        setWidthAndHeights(graphics);

        for(IkasanFlowRouteViewHandler flowRouteViewHandler : flowRouteViewHandlers) {
            flowRouteViewHandler.initialiseDimensions(graphics, currentX, topYForElements, newTopY);
        }
    }

    private void setWidthAndHeights(Graphics graphics)  {
        if (flowRoute != null && ! flowRoute.ftlGetConsumerAndFlowElements().isEmpty()) {
            setWidth(getFlowElementsWidth() + (2 * FLOW_CONTAINER_BORDER));
            setHeight(getFlowElementsHeight(graphics) + FLOW_CONTAINER_BORDER);
        } else {
            setWidth(0);
            setHeight(0);
        }
    }

    public int getFlowElementsWidth()  {
        return getFlowElementsRightX() - getFlowElementsLeftX();
    }
    public int getFlowElementsHeight(Graphics g)  {
        return getFlowElementsBottomY(g) - getFlowElementsTopY();
    }

    /**
     * For a given flowRoute, the first element will be the leftmost i.e. min x
     * @return the smallest (most left) x value for the route
     */
    public int getFlowElementsMinX() {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            return getOrCreateAbstractViewHandler(projectKey, flowElementList.get(0)).getLeftX();
        }
    }

    public int getFlowElementsTopY() {
        return getFlowElementsMinY();
    }
    /**
     * For a given flowRoute, the first element will be good enough to get the min Y
     * @return the smallest y value for the route
     */
    public int getFlowElementsMinY() {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            return getOrCreateAbstractViewHandler(projectKey, flowElementList.get(0)).getTopY();
        }
    }


    /**
     * For a given flowRoute, the first element will be the leftmost
     * @return the smallest (most lext) x value for the route
     */
    public int getFlowElementsLeftX() {
        return getFlowElementsMinX();
    }

    public int getFlowElementsRightX() {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            return getOrCreateAbstractViewHandler(projectKey, flowElementList.get(flowElementList.size()-1)).getRightX();
        }
    }

    /**
     * For the given route find the one with the largest Y valye
     * @return the larget Y
     */
    public int getFlowElementsBottomY(Graphics g)  {
        int bottomY = 0 ;
        List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            for (FlowElement flowElement : flowElementList) {
                int flowY = getOrCreateAbstractViewHandler(projectKey, flowElement).getBottomYPlusText(g);
                bottomY = Math.max(flowY, bottomY);
            }
        }
        return bottomY;
    }

    /**
     * Get the
     * @return maximum (right most) X of any element in any contained route
     */
    public int getAllRouteMaxX(int currentMax) {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (!flowElementList.isEmpty()) {
            int thisRouteMaxX = getFlowElementsRightX();
            if (thisRouteMaxX > currentMax) {
                currentMax = thisRouteMaxX;
            }
            for (IkasanFlowRouteViewHandler flowRouteViewHandler : flowRouteViewHandlers) {
                currentMax = flowRouteViewHandler.getAllRouteMaxX(currentMax);
            }
        }
        return currentMax;
    }
    /**
     * Get the
     * @return maximum (bottom most) Y of any element in any contained route
     */
    public int getAllRouteMaxY(Graphics g, int currentMax) {
        java.util.List<FlowElement> flowElementList = flowRoute.ftlGetConsumerAndFlowElements();
        if (!flowElementList.isEmpty()) {
            int thisRouteMaxY = getFlowElementsBottomY(g);
            if (thisRouteMaxY > currentMax) {
                currentMax = thisRouteMaxY;
            }
            for (IkasanFlowRouteViewHandler flowRouteViewHandler : flowRouteViewHandlers) {
                currentMax = flowRouteViewHandler.getAllRouteMaxY(g, currentMax);
            }
        }
        return currentMax;
    }


//    /**
//     * The flow is willing to accept a component that has been dragged to it
//     */
//    public void setFlowReceptiveMode() {
//        this.warningText = "";
//        this.borderColor = JBColor.GREEN;
//    }
//
//    public boolean isFlowReceptiveMode() {
//        return Color.GREEN.equals(this.borderColor);
//    }
//
//    /***
//     * Typically this appears when attempting to drag in a component into a flow where it is not allowed
//     * @param mouseX to display the warning
//     * @param mouseY to display the warning
//     * @param message to appear
//     */
//    public void setFlowlWarningMode(int mouseX, int mouseY, String message) {
//        this.warningText = message;
//        this.warningX = mouseX;
//        this.warningY = mouseY;
//        this.borderColor = JBColor.RED;
//    }
//
//    public boolean isFlowWarningMode() {
//        return Color.RED.equals(this.borderColor);
//    }
//    public void setFlowNormalMode() {
//        this.warningText = "";
//        this.borderColor = JBColor.BLACK;
//    }
//
//    public boolean isFlowNormalMode() {
//        return Color.BLACK.equals(this.borderColor);
//    }
}
