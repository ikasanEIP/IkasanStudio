package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonSerialize(using = FlowSerializer.class)
public class Flow extends BasicElement {
    private static final Logger LOG = LoggerFactory.getLogger(BasicElement.class);
    // The fields of a Flow will need to be known for serialisation
    public static final String CONSUMER_JSON_TAG = "consumer";
    public static final String TRANSITIONS_JSON_TAG = "transitions";
    public static final String FLOW_ELEMENTS_JSON_TAG = "flowElements";

    private FlowElement consumer;
    private List<Transition> transitions;
    private List<FlowElement> flowElements;
    private ExceptionResolver exceptionResolver;

    /**
     * Used primarily during deserialization.
     */
    public Flow() throws StudioBuildException {
        super (IkasanComponentLibrary.getFLowComponentMeta(IkasanComponentLibrary.DEFAULT_IKASAN_PACK), null);
        LOG.error("Parameterless version of flow called");
        transitions = new ArrayList<>();
        flowElements = new ArrayList<>();
    }

    public Flow(String metapackVersion) throws StudioBuildException {
        super (IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        transitions = new ArrayList<>();
        flowElements = new ArrayList<>();
    }

    @Builder(builderMethodName = "flowBuilder")
    public Flow(
                @NonNull
                String metapackVersion,
                FlowElement consumer,
                List<Transition> transitions,
                List<FlowElement> flowElements,
                ExceptionResolver exceptionResolver,
                String name,
                String description) throws StudioBuildException {
        super(IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        if (consumer != null) {
            if (!consumer.getComponentMeta().isConsumer()) {
                LOG.error("ERROR : Tried to set consumer on " + this + " with a flowElement that is not a consumer " + consumer + ", this will be ignored");
            } else {
                this.consumer = consumer;
            }
        }
        if (transitions != null) {
            // By default, Lombok uses immutable arrays
            this.transitions = new ArrayList<>(transitions);
        } else {
            this.transitions = new ArrayList<>();
        }
        if (flowElements != null) {
            this.flowElements = new ArrayList<>(flowElements);
        } else {
            if (this.flowElements == null) {
                this.flowElements = new ArrayList<>();
            }
        }

        this.exceptionResolver = exceptionResolver;
        super.setName(name);
        super.setDescription(description);
    }

    public void addFlowComponent(FlowElement ikasanFlowComponent) {
        flowElements.add(ikasanFlowComponent);
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null) {
            if (ikasanFlowComponentToBeRemoved.getComponentMeta().isConsumer()) {
                setConsumer(null);
            } else if (! flowElements.isEmpty()) {
                getFlowElements().remove(ikasanFlowComponentToBeRemoved);
            } else {
                LOG.warn("Attempt to remove element " + ikasanFlowComponentToBeRemoved + " because it could not be found in the memory model");
            }
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
                ((!hasConsumer() || !newComponent.isConsumer())) &&
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
        } else if (hasConsumer() && newComponent.isConsumer()) {
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
        if (! hasConsumer()) {
            status += "The flow needs a consumer";
        }
        if (! hasProducer()) {
            if (!status.isEmpty()) {
                status += " and a producer";
            } else {
                status += "The flow needs a producer";
            }
        }
        if (!status.isEmpty()) {
            status += " to be complete.";
        }
        return status;
    }

    public boolean hasConsumer() {
        return getConsumer() != null;
    }

    public boolean hasProducer() {
        return flowElements.stream()
            .anyMatch(e->e.getComponentMeta().isProducer());
    }

    /**
     * Does the Flow have a valid exception resolver
     * @return if the flow has a valid exception resolver.
     */
    public boolean hasExceptionResolver() {
        return (exceptionResolver != null && exceptionResolver.isValid());
    }


    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElements() {
        List<FlowElement> allFlowElements = new ArrayList<>();
        if (consumer != null) {
            allFlowElements.add(consumer);
        }
        if (flowElements != null && !flowElements.isEmpty()) {
            allFlowElements.addAll(flowElements);
        }
        return allFlowElements;
    }
}
