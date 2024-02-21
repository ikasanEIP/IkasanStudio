package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.instance.serialization.FlowDeserializer;
import org.ikasan.studio.model.ikasan.instance.serialization.FlowSerializer;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;

import java.util.ArrayList;
import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary.STD_IKASAN_PACK;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonSerialize(using = FlowSerializer.class)
@JsonDeserialize(using = FlowDeserializer.class)
public class Flow extends IkasanElement {
    // The fields of a Flow will need to be known for serialisation
    public static final String CONSUMER = "consumer";
    public static final String TRANSITIONS = "transitions";
    public static final String FLOW_ELEMENTS = "flowElements";

    private FlowElement consumer;
    private List<Transition> transitions;
    private List<FlowElement> flowElements = new ArrayList<>();
    private ExceptionResolver exceptionResolver;
//    String configurationId;
//    String flowStartupType;
//    String flowStartupComment;
    private FlowElement output;
    public Flow() {
        super (IkasanComponentLibrary.getFLow(STD_IKASAN_PACK));
    }
    @Builder(builderMethodName = "flowBuilder")
    public Flow(
                FlowElement consumer,
                List<Transition> transitions,
                List<FlowElement> flowElements,
                ExceptionResolver exceptionResolver,
                String name) {
        super(IkasanComponentLibrary.getFLow(STD_IKASAN_PACK));
        this.consumer = consumer;
        if (transitions != null) {
            this.transitions = transitions;
        } else {
            this.transitions = new ArrayList<>();
        }
        if (flowElements != null) {
            this.flowElements = flowElements;
        } else {
            if (this.flowElements == null) {
                this.flowElements = new ArrayList<>();
            }
        }

        this.exceptionResolver = exceptionResolver;
        super.setName(name);
    }

    public void addFlowComponent(FlowElement ikasanFlowComponent) {
        flowElements.add(ikasanFlowComponent);
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null && ! flowElements.isEmpty()) {
            getFlowElements().remove(ikasanFlowComponentToBeRemoved);
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent to br added
     * @return true if component valid to be added
     */
    public boolean isValidToAdd(IkasanComponentMeta newComponent) {
        return newComponent == null ||
                ((!hasConsumer() || !newComponent.isConsumer())) &&
                        (!hasProducer() || !newComponent.isProducer());
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
     * @param newComponent to be added
     * @return reason why the component can not be added or empty string if there is no problem.
     */
    public String issueCausedByAdding(IkasanComponentMeta newComponent) {
        String reason = "";
        if (hasConsumer() && newComponent.isConsumer()) {
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
        return flowElements.stream()
            .anyMatch(e->e.getIkasanComponentMeta().isConsumer());
    }

    public boolean hasProducer() {
        return flowElements.stream()
            .anyMatch(e->e.getIkasanComponentMeta().isProducer());
    }

    /**
     * Does the Flow have a valid exception resolver
     * @return if the flow has a valid exception resolver.
     */
    public boolean hasExceptionResolver() {
        return (exceptionResolver != null && exceptionResolver.isValid());
    }

    @Override
    public String toString() {
        return "IkasanFlow{" +
                "name='" + getComponentName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", flowElements=" + flowElements +
                '}';
    }
}
