package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of flow elements and their corresponding transitions
 *
 */
@Data
public class FlowRoutelet {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRoutelet.class);
    Flow flow;  // A convenience link to get back to the containing flow
//    private List<Transition> transitions;
    private List<FlowElement> flowElements;

    public FlowRoutelet() throws StudioBuildException {
//        super (IkasanComponentLibrary.getFLowComponentMeta(IkasanComponentLibrary.DEFAULT_IKASAN_PACK), null);
        LOG.error("Parameterless version of flow called");
//        transitions = new ArrayList<>();
        flowElements = new ArrayList<>();
    }

    public FlowRoutelet(Flow flow, String metapackVersion) throws StudioBuildException {
//        super (IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        this.flow = flow;
//        transitions = new ArrayList<>();
        flowElements = new ArrayList<>();
    }

    public FlowRoutelet(String metapackVersion, Flow flow, List<Transition> transitions, List<FlowElement> flowElements) throws StudioBuildException {
//        super (IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        this.flow = flow;
//        if (transitions != null) {
//            // By default, Lombok uses immutable arrays
//            this.transitions = new ArrayList<>(transitions);
//        } else {
//            this.transitions = new ArrayList<>();
//        }
        if (flowElements != null) {
            this.flowElements = new ArrayList<>(flowElements);
        } else {
            if (this.flowElements == null) {
                this.flowElements = new ArrayList<>();
            }
        }
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null) {
            getFlowElements().remove(ikasanFlowComponentToBeRemoved);
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
    public List<FlowElement> ftlGetFlowElements() {
        List<FlowElement> allFlowElements = new ArrayList<>();
        if (flowElements != null && !flowElements.isEmpty()) {
            allFlowElements.addAll(flowElements);
        }
        return allFlowElements;
    }

    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElementsNoEndPoints() {
        return ftlGetFlowElements().stream()
                .filter(x-> ! x.componentMeta.isEndpoint())
                .toList();
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

    /**
     * Determine the current state of the flow for completeness
     * @return A status string
     */
    @JsonIgnore
    public String getFlowIntegrityStatus() {
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
