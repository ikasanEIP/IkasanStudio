package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The 'default' flow is contained in a single FlowRoute i.e. each node
 * And additional branches
 */
@Data
public class FlowRoute {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRoute.class);
    List<FlowRoute> childRoutes;
    private List<FlowElement> flowElements;

    Flow flow;  // A convenience link to get back to the containing flow
    String routeName;
    /**
     * Used primarily during deserialization.
     */
    public FlowRoute() {
        LOG.error("Parameterless version of flow called");
    }

    @Builder(builderMethodName = "flowBuilder")
    public FlowRoute(
            Flow flow,
            String routeName,
            List<FlowRoute> childRoutes,
            List<FlowElement> flowElements) throws StudioBuildException {
        this.flow = flow;
        if (routeName != null) {
            this.routeName = routeName;
        } else {
            this.routeName = Transition.DEFAULT_TRANSITION_NAME;
        }
        if (childRoutes != null) {
            this.childRoutes = childRoutes;
        } else {
            this.childRoutes = new ArrayList<>();
        }
        if (flowElements != null) {
            this.flowElements = flowElements;
        } else {
            this.flowElements = new ArrayList<>();
        }
        if (flow == null) {
            throw new StudioBuildException("Flow can not be null");
        }
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (flowElements!=null && flowElements.contains(ikasanFlowComponentToBeRemoved)) {
            flowElements.remove(ikasanFlowComponentToBeRemoved);
        } else if (childRoutes != null) {
            childRoutes.forEach(flowRoute -> flowRoute.removeFlowElement(ikasanFlowComponentToBeRemoved));
        }
    }

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
                if (!status.isEmpty()) {
                    status += " and a producer";
                } else {
                    status += "The flow needs a producer";
                }
            }
            return status;
        }
    }


    public boolean hasProducer() {
        return flowElements.stream()
                .anyMatch(e->e.getComponentMeta().isProducer());
    }

    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElements() {

        List<FlowElement> allElements = new ArrayList<>();
        if (flow.hasConsumer()) {
            allElements.add(flow.getConsumer());
        }
        allElements.addAll(ftlGetFlowElementsNoEndPoints());
        return allElements;
    }


    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetFlowElementsNoEndPoints() {
        List<FlowElement> allElements  = getFlowElements().stream()
                .filter(x-> ! x.componentMeta.isEndpoint())
                .toList();
        return allElements;
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
        if (newComponent.isFlow()) {
            reason += "You can add a flow to a module but not inside another flow";
        } else if (flow.hasConsumer() && newComponent.isConsumer()) {
            reason += "The flow cannot have more then one consumer";
        } else if (hasProducer() && newComponent.isProducer()) {
            reason += "The flow cannot have more then one producer";
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
}
