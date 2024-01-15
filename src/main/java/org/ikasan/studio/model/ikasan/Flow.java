package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentCategory;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolver;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.ArrayList;
import java.util.List;

public class Flow extends IkasanElement {

    String name;
    String configurationId;
    String flowStartupType;
    String flowStartupComment;
    org.ikasan.studio.model.ik.FlowElement consumer;
//    List<Transition> transitions;
//    List<org.ikasan.studio.model.ik.FlowElement> flowElements;

    private FlowElement input;

    private FlowElement output;

    private List<FlowElement> flowComponentList = new ArrayList<>();

    private IkasanExceptionResolver ikasanExceptionResolver;

    public Flow() {
        super (IkasanComponentMeta.FLOW, IkasanComponentMeta.FLOW.getMandatoryProperties());
//        this.configuredProperties.put(IkasanComponentPropertyMeta.NAME, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT));
//        this.configuredProperties.put(IkasanComponentPropertyMeta.DESCRIPTION, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT));
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    public List<FlowElement> getFlowComponentList() {
        return flowComponentList;
    }

    public void addFlowComponent(FlowElement ikasanFlowComponent) {
        flowComponentList.add(ikasanFlowComponent);
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null && ! flowComponentList.isEmpty()) {
            getFlowComponentList().remove(ikasanFlowComponentToBeRemoved);
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent to br added
     * @return true if component valid to be added
     */
    public boolean isValidToAdd(IkasanComponentMeta newComponent) {
        return newComponent == null ||
                ((!hasConsumer() || !IkasanComponentCategory.CONSUMER.equals(newComponent.getElementCategory())) &&
                        (!hasProducer() || !IkasanComponentCategory.PRODUCER.equals(newComponent.getElementCategory())));
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
     * @param newComponent to be added
     * @return reason why the component can not be added or empty string if there is no problem.
     */
    public String issueCausedByAdding(IkasanComponentMeta newComponent) {
        String reason = "";
        if (hasConsumer() && IkasanComponentCategory.CONSUMER.equals(newComponent.getElementCategory())) {
            reason += "The flow cannot have more then one consumer";
        } else if (hasProducer() && IkasanComponentCategory.PRODUCER.equals(newComponent.getElementCategory())) {
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
        return flowComponentList.stream()
            .anyMatch(e->e.getIkasanComponentTypeMeta().getElementCategory().equals(IkasanComponentCategory.CONSUMER));
    }

    public boolean hasProducer() {
        return flowComponentList.stream()
            .anyMatch(e->e.getIkasanComponentTypeMeta().getElementCategory().equals(IkasanComponentCategory.PRODUCER));
    }

    /**
     * Does the Flow have a valid exception resolver
     * @return if the flow has a valid exception resolver.
     */
    public boolean hasExceptionResolver() {
        return (ikasanExceptionResolver != null && ikasanExceptionResolver.isValid());
    }

    public FlowElement getInput() {
        return input;
    }

    public void setInput(FlowElement input) {
        this.input = input;
    }

    public FlowElement getOutput() {
        return output;
    }

    public void setOutput(FlowElement output) {
        this.output = output;
    }

    public IkasanExceptionResolver getIkasanExceptionResolver() {
        return ikasanExceptionResolver;
    }

    public void setIkasanExceptionResolver(IkasanExceptionResolver ikasanExceptionResolver) {
        this.ikasanExceptionResolver = ikasanExceptionResolver;
    }

    @Override
    public String toString() {
        return "IkasanFlow{" +
                "name='" + getComponentName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", flowComponentList=" + flowComponentList +
                '}';
    }
}
