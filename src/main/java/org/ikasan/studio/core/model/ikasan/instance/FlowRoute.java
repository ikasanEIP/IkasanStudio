package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.instance.Transition.DEFAULT_TRANSITION_NAME;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.ROUTE_NAMES;

/**
 * Most of the time, a flow contains a single flow route. The flow route itself
 * can have multiple child routes when a router is used.
 */
@Getter
@Setter
public class FlowRoute  implements IkasanComponent {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRoute.class);
    private List<FlowRoute> childRoutes;
    private List<FlowElement> flowElements;

    Flow flow;  // A convenience link to get back to the containing flow
    String routeName;

    /**
     * Used primarily during deserialization.
     */
    private FlowRoute() throws StudioBuildException {
        LOG.warn("STUDIO: SERIOUS: Parameterless version of flowRoute called");
    }

    @Builder(builderMethodName = "flowRouteBuilder")
    public FlowRoute(
            Flow flow,
            String routeName,
            List<FlowRoute> childRoutes,
            List<FlowElement> flowElements) throws StudioBuildException {
        this.flow = flow;
        this.routeName = routeName != null ? routeName : DEFAULT_TRANSITION_NAME;
        this.childRoutes = childRoutes != null ? childRoutes : new ArrayList<>();

        if (flowElements != null) {
            this.flowElements = new ArrayList<>();
            for(FlowElement flowElement : flowElements) {
                if (flowElement.getComponentMeta().isConsumer()) {
                    Thread thread = Thread.currentThread();
                    LOG.warn("STUDIO: SERIOUS: Attempt made to add a consumer " + flowElement + " to a route, will try to add to flow. Trace: " + Arrays.toString(thread.getStackTrace()));
                    if (!flow.hasConsumer()) {
                        flow.setConsumer(flowElement);
                    } else {
                        LOG.warn("STUDIO: SERIOUS: could not add to flow consumer, a consumer already exists " + flow.getConsumer());
                    }
                } else {
                    this.flowElements.add(flowElement);
                }
            }
        } else {
            this.flowElements = new ArrayList<>();
        }
        if (flow == null) {
            throw new StudioBuildException("Flow can not be null");
        }
    }

    /**
     * Is the route devoid of children and elements
     * @return true if there are no children and no elements.
     */
    public boolean isEmpty() {
        if (!childRoutes.isEmpty()) {
            return flowElements.isEmpty() && childRoutes.stream().allMatch(FlowRoute::isEmpty);
        } else {
            return flowElements.isEmpty();
        }
    }

    protected FlowRoute findRouteOfName(String routeName) {
        if (routeName != null && childRoutes != null && !childRoutes.isEmpty()) {
            return childRoutes.stream()
                    .filter(childRoute -> childRoute.getRouteName().equals(routeName))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Ensure this route has a child FlowRoute (with its Router Endpoint marker already in place, matching what
     * {@link org.ikasan.studio.core.model.ikasan.instance.serialization.ModuleDeserializer#addNewRoutesForRouter}
     * builds on a full model.json reload) for every name currently in the given router's routeNames property.
     * Live add/edit of a router never keeps childRoutes in sync with routeNames on its own - only a full reload
     * does that - so without this, a freshly added or just-edited router has no branches to actually drop
     * components into, and the canvas wrongly reports "cannot have a router AND a producer" against this
     * (the router's own containing) route instead. Existing child routes (and anything already inside them)
     * are left untouched; only routeNames not yet represented get a new (empty, endpoint-only) child route.
     * @param metapackVersion of the router
     * @param router whose routeNames should be reflected in this route's children
     */
    public void syncChildRoutesForRouter(String metapackVersion, FlowElement router) throws StudioBuildException {
        Object rawRouteNames = router.getPropertyValue(ROUTE_NAMES);
        if (!(rawRouteNames instanceof List<?> routeNames)) {
            return;
        }
        String endpointComponentName = router.getComponentMeta().getEndpointKey();
        if (endpointComponentName == null) {
            LOG.warn("STUDIO: SERIOUS: syncChildRoutesForRouter could not find an endpoint key for router " + router);
            return;
        }
        for (Object routeNameObj : routeNames) {
            if (routeNameObj instanceof String routeName && !routeName.isBlank() && findRouteOfName(routeName) == null) {
                FlowRoute newChild = FlowRoute.flowRouteBuilder().flow(flow).routeName(routeName).build();
                childRoutes.add(newChild);
                ComponentMeta endpointMeta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metapackVersion, endpointComponentName);
                FlowElement endpoint = FlowElementFactory.createFlowElement(metapackVersion, endpointMeta, flow, newChild, routeName);
                endpoint.setContainingFlowRoute(newChild);
                newChild.getFlowElements().add(endpoint);
            }
        }
    }


    /**
     * Attempt to remove the element from the flow. Note that the UI threads can sometimes call this multiple times so
     * Extra checks are required.
     * @param ikasanFlowComponentToBeRemoved from this route
     * @return a record of what was removed and from where, so the removal can be reversed (e.g. to support
     * IDE Undo); null if there was nothing to remove.
     */
    @SuppressWarnings("unchecked")
    public FlowElementRemoval removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null) {
            List<FlowElementRemoval.ChildRouteRemoval> removedChildRoutes = new ArrayList<>();
            if (ikasanFlowComponentToBeRemoved.componentMeta.isRouter()) {
                for (String routeName : (List<String>) ikasanFlowComponentToBeRemoved.getPropertyValue(ROUTE_NAMES)) {
                    FlowRoute deleteTarget = findRouteOfName(routeName);
                    if (deleteTarget != null && this != deleteTarget) {
                        int childIndex = childRoutes.indexOf(deleteTarget);
                        if (childRoutes.remove(deleteTarget)) {
                            removedChildRoutes.add(new FlowElementRemoval.ChildRouteRemoval(this, deleteTarget, childIndex));
                        }
                    }
                }
            }
            int elementIndex = -1;
            if (flowElements != null) {
                elementIndex = flowElements.indexOf(ikasanFlowComponentToBeRemoved);
                flowElements.remove(ikasanFlowComponentToBeRemoved);
            }
            return new FlowElementRemoval(ikasanFlowComponentToBeRemoved, flow, this, elementIndex, false, false, removedChildRoutes);
        }
        return null;
    }


    public boolean hasProducer() {
        return flowElements.stream()
                .anyMatch(e->e.getComponentMeta().isProducer());
    }

    public boolean hasRouter() {
        return flowElements.stream()
                .anyMatch(e->e.getComponentMeta().isRouter());
    }

    /**
     * Determine the current state of the flow for completeness
     * @return A status string
     */
    @JsonIgnore
    public String getFlowIntegrityStatus() {
        String status = "";
        if (childRoutes != null && !childRoutes.isEmpty()) {
            // A router always marks the end of a route (see ModuleDeserializer#buildRouteTree) - this route's
            // own flowElements never include a producer once it ends in one, completeness is entirely
            // delegated to the branches below. Checking hasProducer() on this route too (as previously
            // happened unconditionally) meant a router-terminated route was reported as needing a producer
            // even when every one of its branches already had one.
            status = childRoutes.stream()
                    .map(FlowRoute::getFlowIntegrityStatus)
                    .filter(childStatus -> !childStatus.isBlank())
                    .collect(Collectors.joining (","));
        } else {
            if (!flow.hasConsumer()) {
                status += "The flow needs a consumer. ";
            }
            if (! hasProducer()) {
                status += "The flow needs a producer. ";
            }
        }
        return status;
    }

    /**
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> getConsumerAndFlowRouteElements() {
        List<FlowElement> allElements = new LinkedList<>();
        // Only the default (primary) flowRoute includes the consumer
        if (flow != null && flow.hasConsumer() && DEFAULT_TRANSITION_NAME.equals(getRouteName())) {
            allElements.add(flow.getConsumer());
        }
        allElements.addAll(getFlowElementsNoExternalEndPoints());
        return Collections.unmodifiableList(allElements);
    }


    /**
     * @return A list of all non-null flow elements, including the consumer and router endpoints
     */
    public List<FlowElement> getFlowElementsNoExternalEndPoints() {
        if (getFlowElements() == null) {
            return new ArrayList<>();
        } else {
            return getFlowElements().stream()
                    .filter(x -> !x.componentMeta.isEndpoint() || x.componentMeta.isInternalEndpoint())
                    .toList();
        }
    }
    /**
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElementsNoEndPoints() {
        if (getConsumerAndFlowRouteElements() == null) {
            return new ArrayList<>();
        } else {
            return getConsumerAndFlowRouteElements().stream()
                    .filter(x -> !x.componentMeta.isEndpoint())
                    .toList();
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent to br added
     * @return true if component valid to be added
     */
    public boolean isValidToAdd(ComponentMeta newComponent) {
        return  newComponent != null && (
            (newComponent.isProducer() && !hasProducer()) ||
            (newComponent.isConsumer() && !flow.hasConsumer()) ||
            (!newComponent.isFlow() && !newComponent.isProducer() && !newComponent.isConsumer())
        );
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
     * Note this is a route, so a consumer is of no concern, that will be dealt with at the flow level
     * @param newComponent to be added
     * @return reason why the component can not be added or empty string if there is no problem.
     */
    public String issueCausedByAdding(ComponentMeta newComponent) {
        String reason = "";
        if (hasProducer() && newComponent.isProducer()) {
            reason += "The flow route cannot have more then one producer. ";
        } else if (hasRouter() && newComponent.isProducer()) {
            reason += "The flow route cannot have a router AND a producer. ";
        } else if (hasRouter() && newComponent.isRouter()) {
            reason += "The flow route cannot have more then one router. ";
        }
        return reason;
    }

    @Override
    public String getIdentity() {
        return routeName;
    }

    /**
     * The intent is to clone the existing FlowRoute but to a different meta-pack metapackVersion.
     * @param metapackVersion of the cloned FlowRoute
     * @return the cloned module with the new meta pack version
     * @throws StudioBuildException when cloning is not possible.
     */
    public FlowRoute cloneToVersion(String metapackVersion, Flow newContainingFlow) throws StudioBuildException {
        if (metapackVersion == null || metapackVersion.isBlank()) {
            LOG.error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
            return null;
        }
        if (this.getChildRoutes() != null && !this.getChildRoutes().isEmpty()) {
            LOG.warn("STUDIO: SERIOUS: Attempt to clone a FlowRoute with no child routes, this is not expected. " + this);
        }
        FlowRoute clonedFlowRoute = new FlowRoute(newContainingFlow, this.getRouteName(), new ArrayList<>(), new  ArrayList<>());
        for (FlowRoute childRoute : this.getChildRoutes()) {
            clonedFlowRoute.getChildRoutes().add(childRoute.cloneToVersion(metapackVersion, newContainingFlow));
        }
        for (FlowElement flowElement : this.getFlowElements()) {
            clonedFlowRoute.getFlowElements().add(flowElement.cloneToVersion(metapackVersion, newContainingFlow, clonedFlowRoute));
        }
        return clonedFlowRoute;
    }


    @Override
    public String toString() {
        return "FlowRoute{" +
                "childRoutes=" + childRoutes +
                ", flowElements=" + flowElements +
                ", routeName='" + routeName + '\'' +
                '}';
    }

    public String toSimpleString() {
        StringBuilder flowElementsBuilder = new StringBuilder();
        if (flowElements != null && !flowElements.isEmpty()) {
            for(FlowElement flowElement : flowElements) {
                flowElementsBuilder.append(flowElement.getIdentity()).append(",");
            }
        }

        StringBuilder childRoutesBuilder = new StringBuilder();
        if (childRoutes != null && !childRoutes.isEmpty()) {
            for(FlowRoute childFlowRoute : childRoutes) {
                childRoutesBuilder.append(childFlowRoute.toSimpleString()).append(",");
            }
        }

        return "FlowRouteName='" + routeName + '\'' +
                ",parentFlow='" + (flow!=null ? flow.getIdentity() : null) + '\'' +
                "[flowElements [" + flowElementsBuilder + "]\n" +
                "childRoutes [" + childRoutesBuilder + "]" +
                ']';
    }
}
