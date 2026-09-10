package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent;

import org.ikasan.studio.core.model.command.FlowElementRemoval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.instance.Transition.DEFAULT_TRANSITION_NAME;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.ROUTE_NAMES;

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
                    LOG.warn(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.CONFIGURATION_INVALID, null, null, null, null));
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
     * Reconcile router branches with their names. Keep exact matches; pair remaining old and new names
     * in order as renames, preserving branch contents. Only empty surplus branches may be removed.
     */
    public void syncChildRoutesForRouter(String metapackVersion, FlowElement router) throws StudioBuildException {
        Object raw = router.getPropertyValue(ROUTE_NAMES);
        if (!(raw instanceof List<?> names)) return;
        List<String> desired = normalizedRouteNames(names);
        String problem = validateChildRouteNames(desired);
        if (problem != null) throw new StudioBuildException(problem);
        String endpointKey = router.getComponentMeta().getEndpointKey();
        if (endpointKey == null) return;
        ComponentMeta endpointMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(metapackVersion, endpointKey);
        var unmatched = childRoutes.stream().filter(r -> !desired.contains(r.getRouteName()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<FlowRoute> reconciled = new ArrayList<>();
        for (String name : desired) {
            FlowRoute child = findRouteOfName(name);
            if (child == null && !unmatched.isEmpty()) {
                child = unmatched.remove(0);
                child.setRouteName(name);
                for (FlowElement endpoint : child.getFlowElements()) {
                    if (endpoint.getComponentMeta().isInternalEndpoint()) {
                        endpoint.setPropertyValue("componentName", name);
                    }
                }
            }
            if (child == null) {
                child = FlowRoute.flowRouteBuilder().flow(flow).routeName(name).build();
                FlowElement endpoint = FlowElementFactory.createFlowElement(metapackVersion, endpointMeta, flow, child, name);
                child.getFlowElements().add(endpoint);
            }
            reconciled.add(child);
        }
        childRoutes.clear();
        childRoutes.addAll(reconciled);
    }

    /** Returns a validation message before edits are committed if they would discard branch contents. */
    public String validateChildRouteNames(List<?> names) {
        List<String> desired = normalizedRouteNames(names);
        List<FlowRoute> unmatched = childRoutes.stream().filter(r -> !desired.contains(r.getRouteName())).toList();
        long replacements = desired.stream().filter(name -> findRouteOfName(name) == null).count();
        for (int i = (int) Math.min(replacements, unmatched.size()); i < unmatched.size(); i++) {
            FlowRoute removed = unmatched.get(i);
            if (!removed.getChildRoutes().isEmpty() || removed.getFlowElements().stream()
                    .anyMatch(element -> !element.getComponentMeta().isInternalEndpoint())) {
                return "Route '" + removed.getRouteName() + "' contains components. Move or remove them before removing this route.";
            }
        }
        return null;
    }

    private static List<String> normalizedRouteNames(List<?> names) {
        return names.stream().filter(String.class::isInstance).map(String.class::cast)
                .map(String::trim).filter(name -> !name.isEmpty()).distinct().toList();
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
            LOG.warn(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.CONFIGURATION_INVALID, null, null, null, null));
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
