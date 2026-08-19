package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.getEndpointForGivenComponent;

/**
 * A flow route and a flow are similar for non Router flows i.e. the flow contains 1 flow route
 * When dealing with Multi-Recipient Routers, the flow will contain the same (default) flowRoute and that
 * Route will contain a list of components but will also contain 1..n childFlowRoutes (which themselves contain elements)
 */
@Getter
public class IkasanFlowRouteViewHandler extends AbstractViewHandlerIntellij {
    private final Project project;
    public static final int FLOW_CONTAINER_BORDER = 10;
    // Gap between the top of the Send Test Message badge and the top of the EndPoint icon it sits above.
    private static final int SEND_TEST_MESSAGE_VERTICAL_GAP = 4;
    // Consumer component names (see Consumer/components/*/component-meta_en_GB.json "name") that deal in
    // files rather than messages - these get the file-flavoured Send Test Message badge instead of the
    // envelope one.
    private static final Set<String> FILE_BASED_CONSUMER_NAMES = Set.of(
            "FTP Consumer", "SFTP Consumer", "Local File Consumer", "Generic Consumer");

    private static final Logger LOG = Logger.getInstance("#IkasanFlowViewHandler");
    private final Flow flow;
    private final FlowRoute flowRoute;
    private final List<IkasanFlowRouteViewHandler> childFlowRouteViewHandlers = new ArrayList<>();
    // Populated during each paint pass so that click detection can resolve an endpoint click back to its owner.
    private final Map<FlowElement, FlowElement> cachedEndpointToOwner = new HashMap<>();
    // Populated during each paint pass so that click detection can resolve a Send Test Message badge click back to its owner.
    private final Map<FlowElement, Rectangle> cachedSendTestMessageBadge = new HashMap<>();

    /**
     * This view handler deals with a flowRoute or a child route (i.e. one of the branches off a multi-recipient router)
     *
     * @param project is the Intellij project instance
     * @param flow       that contains this flow route
     * @param flowRoute  which could be the default route or a sub-route (one of the branches off a multi-recipient router)
     */
    public IkasanFlowRouteViewHandler(Project project, Flow flow, FlowRoute flowRoute) {
        this.project = project;
        this.flow = flow;
        this.flowRoute = flowRoute;
        if (flowRoute.getChildRoutes() != null && !flowRoute.getChildRoutes().isEmpty()) {
            for (FlowRoute childRoute : flowRoute.getChildRoutes()) {
                childFlowRouteViewHandlers.add(new IkasanFlowRouteViewHandler(project, flow, childRoute));
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
        cachedSendTestMessageBadge.clear();
        List<FlowElement> flowAndConsumerElementList = flowRoute.getConsumerAndFlowRouteElements();
        int flowSize = flowAndConsumerElementList.size();
        StudioUIUtils.setLine(g, 2f);

        // Paint any components between the first and the last
        for (int index = 0; index < flowSize; index++) {
            // Draw Element
            FlowElement flowElement = flowAndConsumerElementList.get(index);
            IkasanFlowComponentViewHandler flowComponentViewHandler = getOrCreateFlowComponentViewHandler(project, flowElement);
            if (flowComponentViewHandler != null) {
                flowComponentViewHandler.paintComponent(canvas, g, -1, -1);
                if (flowElement.getComponentMeta().isConsumer() || flowElement.getComponentMeta().isProducer()) {
                    displayExternalEndpointIfExists(canvas, g, flowElement);
                }

                // Draw Connector to previous route
                if (index == 0 && parent != null) {
                    List<FlowElement> previousRouteFlowElements = parent.getFlowElements();
                    if (previousRouteFlowElements != null && ! previousRouteFlowElements.isEmpty()) {
                        FlowElement lastElementPreviousRoute = previousRouteFlowElements.get(previousRouteFlowElements.size() - 1);
                        IkasanFlowComponentViewHandler lastElementPreviousRouteViewHandler = getOrCreateFlowComponentViewHandler(project, lastElementPreviousRoute);
                        drawConnector(g, lastElementPreviousRouteViewHandler, flowComponentViewHandler);
                    }
                }
                // Draw Connector to previous flow element
                if (index < flowSize - 1) {
                    IkasanFlowComponentViewHandler nextFlowComponentViewHandler = getOrCreateFlowComponentViewHandler(project, flowAndConsumerElementList.get(index + 1));
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
        FlowElement endpointFlowElement = getEndpointForGivenComponent(project.getService(UiContext.class).getIkasanModule().getMetaVersion(), targetFlowElement);
        if (endpointFlowElement == null) {
            // No external endpoint (e.g. Generic Consumer, Event Generating Consumer) - Send Test Message is
            // still available for any Consumer (see DesignCanvasContextMenu), so the badge belongs directly
            // above the consumer's own icon instead of an endpoint that doesn't exist.
            if (targetFlowElement.getComponentMeta().isConsumer()
                    && project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
                IkasanFlowComponentViewHandler targetFlowElementViewHandler = getOrCreateFlowComponentViewHandler(project, targetFlowElement);
                if (targetFlowElementViewHandler != null) {
                    paintSendTestMessageBadge(canvas, g, targetFlowElement, targetFlowElementViewHandler);
                }
            }
        } else {
            cachedEndpointToOwner.put(endpointFlowElement, targetFlowElement);
            // Position and draw the endpoint
            IkasanFlowComponentViewHandler targetFlowElementViewHandler = getOrCreateFlowComponentViewHandler(project, targetFlowElement);
            IkasanFlowComponentViewHandler endpointViewHandler = getOrCreateFlowComponentViewHandler(project, endpointFlowElement);
            if (targetFlowElementViewHandler != null && endpointViewHandler != null) {

                endpointViewHandler.setWidth(targetFlowElementViewHandler.getWidth());
                endpointViewHandler.setTopY(targetFlowElementViewHandler.getTopY());
                if (targetFlowElement.getComponentMeta().isConsumer()) {
                    // Anchor to the flow's own border (like the producer branch below) rather than to
                    // targetFlowElementViewHandler's leftX, which shifts when a leading decorator (e.g. Wiretap)
                    // is added - anchoring to the target element made the gap collapse once a decorator was present.
                    int endpointLeftX = ViewHandlerCache.getFlowViewHandler(project, flow).getLeftX() - FLOW_CONTAINER_BORDER - UiContext.getMinimumComponentXSpacing() - endpointViewHandler.getWidth();
                    if (endpointLeftX < -10) {
                        LOG.error("STUDIO: 1 Left X being set to a -ve of " + endpointLeftX);
                    }
                    endpointViewHandler.setLeftX(endpointLeftX);
                    endpointViewHandler.paintComponent(canvas, g, -1, -1);
                    drawConnector(g, endpointViewHandler, targetFlowElementViewHandler);
                    if (project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
                        paintSendTestMessageBadge(canvas, g, targetFlowElement, endpointViewHandler);
                    }
                } else {
//                    endpointViewHandler.setLeftX(ViewHandlerCache.getFlowViewHandler(project, flow).getRightX() + FLOW_CONTAINER_BORDER + FLOW_X_SPACING);
//XXXX
if (ViewHandlerCache.getFlowViewHandler(project, flow).getRightX() + FLOW_CONTAINER_BORDER + UiContext.getMinimumComponentXSpacing() < -10) {
    LOG.error("STUDIO: 2 Left X being set to a -ve of " + (ViewHandlerCache.getFlowViewHandler(project, flow).getRightX() + FLOW_CONTAINER_BORDER + UiContext.getMinimumComponentXSpacing()));
}
                    endpointViewHandler.setLeftX(ViewHandlerCache.getFlowViewHandler(project, flow).getRightX() + FLOW_CONTAINER_BORDER + UiContext.getMinimumComponentXSpacing());
                    endpointViewHandler.paintComponent(canvas, g, -1, -1);
                    drawConnector(g, targetFlowElementViewHandler, endpointViewHandler);
                }
            }
        }
    }


    /**
     * If the click coordinates fall within any cached external endpoint icon, return the owning
     * consumer or producer FlowElement. Returns null if the click does not hit any endpoint.
     * The cache is populated (and refreshed) during each paint pass by displayExternalEndpointIfExists.
     */
    public FlowElement getOwnerForEndpointAtXY(int x, int y) {
        for (Map.Entry<FlowElement, FlowElement> entry : cachedEndpointToOwner.entrySet()) {
            IkasanFlowComponentViewHandler endpointVH = ViewHandlerCache.getFlowComponentViewHandler(project, entry.getKey());
            if (endpointVH != null && endpointVH.isComponentAtXY(x, y)) {
                return entry.getValue();
            }
        }
        for (IkasanFlowRouteViewHandler child : childFlowRouteViewHandlers) {
            FlowElement owner = child.getOwnerForEndpointAtXY(x, y);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    /**
     * Paint the Send Test Message badge centred above the given (already positioned) EndPoint icon, and cache
     * its bounds against the owning Consumer so clicks on it can be resolved back via
     * {@link #getOwnerForSendTestMessageAtXY}.
     * @param canvas to paint on
     * @param g is the graphics callback
     * @param owner the Consumer that the badge belongs to, also used to pick the file vs envelope badge icon
     * @param anchorViewHandler the already-positioned view handler the badge is centred above - the EndPoint
     *                          icon if the consumer has one, otherwise the consumer's own icon
     */
    private void paintSendTestMessageBadge(JPanel canvas, Graphics g, FlowElement owner, IkasanFlowComponentViewHandler anchorViewHandler) {
        Icon sendTestMessageIcon = FILE_BASED_CONSUMER_NAMES.contains(owner.getComponentMeta().getName())
                ? IkasanComponentLibrary.getSendTestMessageFileIcon()
                : IkasanComponentLibrary.getSendTestMessageIcon();
        int anchorIconWidth = anchorViewHandler.getCanvasIcon().getIconWidth();
        int badgeLeftX = anchorViewHandler.getLeftX() + ((anchorIconWidth - sendTestMessageIcon.getIconWidth()) / 2);
        int badgeTopY = anchorViewHandler.getTopY() - sendTestMessageIcon.getIconHeight() - SEND_TEST_MESSAGE_VERTICAL_GAP;
        sendTestMessageIcon.paintIcon(canvas, g, badgeLeftX, badgeTopY);
        cachedSendTestMessageBadge.put(owner, new Rectangle(badgeLeftX, badgeTopY, sendTestMessageIcon.getIconWidth(), sendTestMessageIcon.getIconHeight()));
    }

    /**
     * If the click coordinates fall within any cached Send Test Message badge, return the owning Consumer
     * FlowElement. Returns null if the click does not hit any badge.
     * The cache is populated (and refreshed) during each paint pass by paintSendTestMessageBadge.
     */
    public FlowElement getOwnerForSendTestMessageAtXY(int x, int y) {
        for (Map.Entry<FlowElement, Rectangle> entry : cachedSendTestMessageBadge.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        for (IkasanFlowRouteViewHandler child : childFlowRouteViewHandlers) {
            FlowElement owner = child.getOwnerForSendTestMessageAtXY(x, y);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    private void drawConnector(Graphics g, AbstractViewHandlerIntellij start, AbstractViewHandlerIntellij end) {
        g.drawLine(
                start.getRightConnectorPoint().x,
                start.getRightConnectorPoint().y,
                end.getLeftConnectorPoint().x,
                end.getLeftConnectorPoint().y);
    }


    @Override
    public void initialiseDimensions(Graphics graphics, int newLeftx, int newTopY, int width, int height) {
        Thread thread = Thread.currentThread();
        LOG.warn("STUDIO: SERIOUS: incorrect initialiseDimensions called for FlowRouteViewHandler :" + Arrays.asList(thread.getStackTrace()));
    }

    /**
     * Initilise the dimensions of the components
     * @param graphics handle
     * @param currentX for the start of the flow route
     * @param topYForElements top y for flow elements
     * @return the new top Y for flow elements
     */
    protected int initialiseDimensions(Graphics graphics, int currentX, int topYForElements) {
        this.setTopY( topYForElements);
        this.setLeftX(currentX);
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (!flowElementList.isEmpty()) {
            for (FlowElement ikasanFlowComponent : flowElementList) {
                AbstractViewHandlerIntellij flowComponentViewHandler = getOrCreateAbstractViewHandler(project, ikasanFlowComponent);
                if (flowComponentViewHandler != null) {
                    flowComponentViewHandler.initialiseDimensions(graphics, currentX, topYForElements, -1, -1);
                    // Anchor on the component's actual (post-shift) right edge rather than currentX + width.
                    // A leading decorator (e.g. Wiretap) already shifted the component's real leftX rightward
                    // inside initialiseDimensions above; re-adding getLeadingGap() here (the same shift) on
                    // top of that double-counted it, shrinking the gap to the *next* component.
                    currentX = flowComponentViewHandler.getRightX() + flowComponentViewHandler.getTrailingGap() + UiContext.getMinimumComponentXSpacing();
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
     * For a given flowRoute, the first element will be the leftmost i.e. min TestV1
     * @return the smallest (most left) TestV1 value for the route
     */
    public int getFlowElementsMinX() {
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            AbstractViewHandlerIntellij firstElementViewHandler = getOrCreateAbstractViewHandler(project, flowElementList.get(0));
            // Use the real (unclamped) leading gap here, not getLeadingGap() - this feeds the flow's bounding
            // box width, and the minimum-gap floor (meant for inter-component spacing) would otherwise inflate
            // it asymmetrically versus the fixed FLOW_CONTAINER_BORDER margin used on this same left edge.
            return firstElementViewHandler.getLeftX() - firstElementViewHandler.getRawLeadingGap();
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
            return getOrCreateAbstractViewHandler(project, flowElementList.get(0)).getTopY();
        }
    }


    /**
     * For a given flowRoute, the first element will be the leftmost
     * @return the smallest (most lext) TestV1 value for the route
     */
    public int getFlowElementsLeftX() {
        return getFlowElementsMinX();
    }

    public int getFlowElementsRightX() {
        java.util.List<FlowElement> flowElementList = flowRoute.getConsumerAndFlowRouteElements();
        if (flowElementList.isEmpty()) {
            return 0;
        } else {
            AbstractViewHandlerIntellij lastFlowElementViewHandler = getOrCreateAbstractViewHandler(project, flowElementList.get(flowElementList.size() - 1));
            // See the comment in getFlowElementsMinX() - same reasoning, trailing side.
            return lastFlowElementViewHandler.getRightX() + lastFlowElementViewHandler.getRawTrailingGap();
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
                int flowY = getOrCreateAbstractViewHandler(project, flowElement).getBottomYPlusText(g);
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
