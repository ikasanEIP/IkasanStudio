package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import lombok.Getter;
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
@Getter
public class IkasanFlowRouteViewHandler extends AbstractViewHandlerIntellij {
    private final String projectKey;
    public static final int FLOW_X_SPACING = 30;
    public static final int FLOW_Y_TITLE_SPACING = 15;
    public static final int FLOW_CONTAINER_BORDER = 10;
    public static final int CONTAINER_CORNER_ARC = 30;

    private static final Logger LOG = Logger.getInstance("#IkasanFlowViewHandler");
    private final Flow flow;
    private final FlowRoute flowRoute;
    private final List<IkasanFlowRouteViewHandler> childFlowRouteViewHandlers = new ArrayList<>();

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
        if (flowRoute.getChildRoutes() != null && !flowRoute.getChildRoutes().isEmpty()) {
            for (FlowRoute childRoute : flowRoute.getChildRoutes()) {
                childFlowRouteViewHandlers.add(new IkasanFlowRouteViewHandler(projectKey, flow, childRoute));
            }
        }
    }

    public List<IkasanFlowRouteViewHandler> getAllFlowRouteViewHandlers(List<IkasanFlowRouteViewHandler> flowRouteViewHandlers, IkasanFlowRouteViewHandler currentRoot) {
        if (currentRoot != null) {
            flowRouteViewHandlers.add(currentRoot);

            if (currentRoot.getChildFlowRouteViewHandlers() != null && !currentRoot.getChildFlowRouteViewHandlers().isEmpty()) {
                for (IkasanFlowRouteViewHandler childFlowRouteViewHandler : currentRoot.getChildFlowRouteViewHandlers()) {
                    getAllFlowRouteViewHandlers(flowRouteViewHandlers, childFlowRouteViewHandler);
                }
            }
        }
        return flowRouteViewHandlers;
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
     * component itself
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

    protected void paintRoute(JPanel canvas, Graphics g, FlowRoute flowRoute, FlowRoute parent) {
        List<FlowElement> flowAndConsumerElementList = flowRoute.getConsumerAndFlowRouteElements();
        int flowSize = flowAndConsumerElementList.size();
        StudioUIUtils.setLine(g, 2f);

        // Paint any components between the first and the last
        for (int index = 0; index < flowSize; index++) {
            // Draw Element
            FlowElement flowElement = flowAndConsumerElementList.get(index);
            IkasanFlowComponentViewHandler flowComponentViewHandler = getOrCreateFlowComponentViewHandler(projectKey, flowElement);
            if (flowComponentViewHandler != null) {
                flowComponentViewHandler.paintComponent(canvas, g, -1, -1);
                if (flowElement.getComponentMeta().isConsumer() || flowElement.getComponentMeta().isProducer()) {
                    displayExternalEndpointIfExists(canvas, g, flowElement);
                }


                // Draw Connector to previous route
                if (index == 0 && parent != null) {
                    List<FlowElement> previousRouteFlowElements = parent.getFlowElements();
                    if (previousRouteFlowElements != null && ! previousRouteFlowElements.isEmpty()) {
                        FlowElement lastElementPreviousRoute = previousRouteFlowElements.get(previousRouteFlowElements.size()-1);
                        IkasanFlowComponentViewHandler lastElementPreviousRouteViewHandler = getOrCreateFlowComponentViewHandler(projectKey, lastElementPreviousRoute);
                        drawConnector(g, lastElementPreviousRouteViewHandler, flowComponentViewHandler);
                    }
                }
                // Draw Connector to previous flow element
                if (index < flowSize - 1) {
                    IkasanFlowComponentViewHandler nextFlowComponentViewHandler = getOrCreateFlowComponentViewHandler(projectKey, flowAndConsumerElementList.get(index + 1));
                    if (nextFlowComponentViewHandler != null) {
                        if (index < flowSize - 1) {
                            drawConnector(g, flowComponentViewHandler, nextFlowComponentViewHandler);
                        }
                    }
                }
            }
        }

        // Traverse down children to paint
        StudioUIUtils.setLine(g, 1f);
        if (flowRoute.getChildRoutes() != null) {
            for (FlowRoute childFlowRoute : flowRoute.getChildRoutes()) {
                paintRoute(canvas, g, childFlowRoute, flowRoute);
            }
        }
    }

    /**
     * The external endpoint reside outside the flow
     * @param canvas to paint on
     * @param g is the graphics callback
     * @param targetFlowElement that may have an endpoint
     */
    private void displayExternalEndpointIfExists(JPanel canvas, Graphics g, FlowElement targetFlowElement) {

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
                endpointFlowElement = FlowElementFactory.createFlowElement(UiContext.getIkasanModule(projectKey).getMetaVersion(), endpointComponentMeta, flow, targetFlowElement.getContainingFlowRoute(), endpointText);
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
//                        endpointViewHandler.setLeftX(targetFlowElementViewHandler.getLeftX() + endpointViewHandler.getWidth() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
                        endpointViewHandler.setLeftX(ViewHandlerFactoryIntellij.getOrCreateFlowViewHandler(projectKey, flow).getRightX() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
                        endpointViewHandler.paintComponent(canvas, g, -1, -1);
                        drawConnector(g, targetFlowElementViewHandler, endpointViewHandler);
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

    /**
     * Initilise the demonsions of the components
     * @param graphics handle
     * @param currentX for the start of the flow route
     * @param topYForElements top y for flow elements
     * @return the new top Y for flow elements
     */
    protected int initialiseDimensions(Graphics graphics, int currentX, int topYForElements) {
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (!flowElementList.isEmpty()) {
            for (FlowElement ikasanFlowComponent : flowElementList) {
                AbstractViewHandlerIntellij flowComponentViewHandler = getOrCreateAbstractViewHandler(projectKey, ikasanFlowComponent);
                if (flowComponentViewHandler != null) {
                    flowComponentViewHandler.initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                    currentX += flowComponentViewHandler.getWidth() + FLOW_X_SPACING;
                }
            }
        }
        setWidthAndHeights(graphics);
        int nextRowY = topYForElements;

        for(IkasanFlowRouteViewHandler flowRouteViewHandler : childFlowRouteViewHandlers) {
            nextRowY = flowRouteViewHandler.initialiseDimensions(graphics, currentX, nextRowY);
        }
        if (isLeaf()) {
            nextRowY += getHeight();
        }

        return nextRowY;
    }

    /**
     * If this the last route view handler in the chain
     * @return true if there are no more children.
     */
    private boolean isLeaf() {
        return flowRoute.getChildRoutes() == null || flowRoute.getChildRoutes().isEmpty();
    }

    private void setWidthAndHeights(Graphics graphics)  {
        if (flowRoute != null && ! flowRoute.getConsumerAndFlowRouteElements().isEmpty()) {
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
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
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
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
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
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
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
        List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
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
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (!flowElementList.isEmpty()) {
            int thisRouteMaxX = getFlowElementsRightX();
            if (thisRouteMaxX > currentMax) {
                currentMax = thisRouteMaxX;
            }
            for (IkasanFlowRouteViewHandler flowRouteViewHandler : childFlowRouteViewHandlers) {
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
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (!flowElementList.isEmpty()) {
            int thisRouteMaxY = getFlowElementsBottomY(g);
            if (thisRouteMaxY > currentMax) {
                currentMax = thisRouteMaxY;
            }
            for (IkasanFlowRouteViewHandler flowRouteViewHandler : childFlowRouteViewHandlers) {
                currentMax = flowRouteViewHandler.getAllRouteMaxY(g, currentMax);
            }
        }
        return currentMax;
    }

    /**
     * Perform any tidy up during deletion of this element
     */
    @Override
    public void dispose() {

    }
}
