package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.ArrayList;
import java.util.List;

public class IkasanFlow extends IkasanComponent {

    private IkasanFlowComponent input;

    private IkasanFlowComponent output;

    private List<IkasanFlowComponent> flowComponentList = new ArrayList<>();

    private IkasanExceptionResolver ikasanExceptionResolver;

    public IkasanFlow () {
        super (IkasanComponentType.FLOW, IkasanComponentType.FLOW.getMandatoryProperties());
        this.configuredProperties.put(IkasanComponentPropertyMeta.NAME, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT));
        this.configuredProperties.put(IkasanComponentPropertyMeta.DESCRIPTION, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT));
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    public List<IkasanFlowComponent> getFlowComponentList() {
        return flowComponentList;
    }

    public boolean addFlowComponent(IkasanFlowComponent ikasanFlowComponent) {
        return flowComponentList.add(ikasanFlowComponent);
    }

    public void removeFlowElement(IkasanFlowComponent ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null && ! flowComponentList.isEmpty()) {
            getFlowComponentList().remove(ikasanFlowComponentToBeRemoved);
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent
     * @return
     */
    public boolean isValidToAdd(IkasanComponentType newComponent) {
        boolean isValid = true ;
        if (newComponent != null &&
            (hasConsumer() && IkasanComponentCategory.CONSUMER.equals(newComponent.getElementCategory()) ||
            hasProducer() && IkasanComponentCategory.PRODUCER.equals(newComponent.getElementCategory()))) {
            isValid = false;
        }
        return isValid;
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
     * @param newComponent to be added
     * @return reason why the component can not be added or empty string if there is no problem.
     */
    public String issueCausedByAdding(IkasanComponentType newComponent) {
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
            if (status.length() > 0) {
                status += " and a producer";
            } else {
                status += "The flow needs a producer";
            }
        }
        if (status.length() > 0) {
            status += " to be complete.";
        }
        return status;
    }

    public boolean hasConsumer() {
        return flowComponentList.stream()
            .anyMatch(e->e.getType().getElementCategory().equals(IkasanComponentCategory.CONSUMER));
    }

    public boolean hasProducer() {
        return flowComponentList.stream()
            .anyMatch(e->e.getType().getElementCategory().equals(IkasanComponentCategory.PRODUCER));
    }

    /**
     * Does the Flow have a valid exception resolver
     * @return if the flow has a valid exception resolver.
     */
    public boolean hasExceptionResolver() {
        return (ikasanExceptionResolver != null && ikasanExceptionResolver.isValid());
    }

    public IkasanFlowComponent getInput() {
        return input;
    }

    public void setInput(IkasanFlowComponent input) {
        this.input = input;
    }

    public IkasanFlowComponent getOutput() {
        return output;
    }

    public void setOutput(IkasanFlowComponent output) {
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
                "name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", flowComponentList=" + flowComponentList +
                '}';
    }
}
