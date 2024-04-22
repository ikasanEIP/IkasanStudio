package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.instance.Transition.DEFAULT_TRANSITION_NAME;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.ROUTE_NAMES;

/**
 * The 'default' flow is contained in a single FlowRoute i.e. each node
 * And additional branches
 */
@Getter
@Setter
public class FlowRoute  implements IkasanComponent {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRoute.class);
    List<FlowRoute> childRoutes;
    private List<FlowElement> flowElements;

    Flow flow;  // A convenience link to get back to the containing flow
    String routeName;
    /**
     * Used primarily during deserialization.
     */
    public FlowRoute() {
        LOG.warn("STUDIO: Serious: Parameterless version of flow called");
    }

    @Builder(builderMethodName = "flowRouteBuilder")
    public FlowRoute(
            Flow flow,
            String routeName,
            List<FlowRoute> childRoutes,
            List<FlowElement> flowElements) throws StudioBuildException {
        this.flow = flow;
        if (routeName != null) {
            this.routeName = routeName;
        } else {
            this.routeName = DEFAULT_TRANSITION_NAME;
        }
        if (childRoutes != null) {
            this.childRoutes = childRoutes;
        } else {
            this.childRoutes = new ArrayList<>();
        }

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
        if (childRoutes != null || !childRoutes.isEmpty()) {
            for(FlowRoute childRoute : childRoutes) {
                if (!childRoute.isEmpty()) {
                    return false;
                }
            }
        }
        return flowElements == null || flowElements.isEmpty();
    }

    protected FlowRoute getRouteOfName(String routeName) {
        if (routeName != null && childRoutes != null && childRoutes.size() > 0) {
            for(FlowRoute childRoute : childRoutes) {
                if (childRoute.getRouteName().equals(routeName)) {
                    return childRoute;
                }
            }
        }
        return null;
    }


    /**
     * Attempt to remove the element from the flow. Note that the UI threads can sometimes call this multiple times so
     * Extra checks are required.
     * @param ikasanFlowComponentToBeRemoved from this route
     */
    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null) {
            if (ikasanFlowComponentToBeRemoved.componentMeta.isRouter()) {
                for (String routeName : (List<String>) ikasanFlowComponentToBeRemoved.getPropertyValue(ROUTE_NAMES)) {
                    FlowRoute deleteTarget = getRouteOfName(routeName);
                    if (deleteTarget != null && this != deleteTarget) {
//                        deleteTarget.deleteRoute();
                        childRoutes.remove(deleteTarget);
                    }
                }
            }
            flowElements.remove(ikasanFlowComponentToBeRemoved);
        }
    }

//    public void removeFlowElement2(FlowElement ikasanFlowComponentToBeRemoved) {
//        if (flowElements!=null && flowElements.contains(ikasanFlowComponentToBeRemoved)) {
//            flowElements.remove(ikasanFlowComponentToBeRemoved);
//        } else if (childRoutes != null) {
//            childRoutes.forEach(flowRoute -> flowRoute.removeFlowElement2(ikasanFlowComponentToBeRemoved));
//        }
//    }

    /**
     * Determine the current state of the flow for completeness
     * @return A status string
     */
    @JsonIgnore
    public String getFlowIntegrityStatus() {
        if (childRoutes != null) {
            return childRoutes.stream().map(f -> f.getFlowIntegrityStatus()).collect(Collectors.joining (","));
        } else {
            String status = "";
            if (! hasProducer()) {
                status += "The flow needs a producer";
            }
            return status;
        }
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
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> getConsumerAndFlowRouteElements() {

        List<FlowElement> allElements = new ArrayList<>();
        // Only the default (primary) flowRoute includes the consumer
        if (flow.hasConsumer() && DEFAULT_TRANSITION_NAME.equals(getRouteName())) {
            allElements.add(flow.getConsumer());
        }
        allElements.addAll(getFlowElementsNoExternalEndPoints());
        return allElements;
    }


    /**
     * @return A list of all non-null flow elements, including the consumer and router endpoints
     */
    public List<FlowElement> getFlowElementsNoExternalEndPoints() {
        if (getFlowElements() == null) {
            return new ArrayList<>();
        } else {
            List<FlowElement> allElements = getFlowElements().stream()
                    .filter(x -> !x.componentMeta.isEndpoint() || x.componentMeta.isInternalEndpoint())
                    .toList();
            return allElements;
        }
    }
    /**
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElementsNoEndPoints() {
        if (getConsumerAndFlowRouteElements() == null) {
            return new ArrayList<>();
        } else {
            List<FlowElement> allElements = getConsumerAndFlowRouteElements().stream()
                    .filter(x -> !x.componentMeta.isEndpoint())
                    .toList();
            return allElements;
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent to br added
     * @return true if component valid to be added
     */
    public boolean isValidToAdd(ComponentMeta newComponent) {
        return  newComponent == null ||
                !newComponent.isFlow() ||
                ((!flow.hasConsumer() || !newComponent.isConsumer())) &&
                        (!hasProducer() || !newComponent.isProducer());
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
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
                flowElementsBuilder.append(flowElement.getName()).append(",");
            }
        }

        StringBuilder childRoutesBuilder = new StringBuilder();
        if (childRoutes != null && !childRoutes.isEmpty()) {
            for(FlowRoute childFlowRoute : childRoutes) {
                childRoutesBuilder.append(childFlowRoute.toSimpleString()).append(",");
            }
        }

        return "FlowRouteName='" + routeName + '\'' +
                ",parentFlow='" + (flow!=null ? flow.getName() : null) + '\'' +
                "[flowElements [" + flowElementsBuilder + "]\n" +
                "childRoutes [" + childRoutesBuilder + "]" +
                ']';
    }
}
