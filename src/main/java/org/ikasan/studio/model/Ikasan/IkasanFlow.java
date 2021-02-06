package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class IkasanFlow extends IkasanComponent {
    private IkasanFlowComponent input;
    private IkasanFlowComponent output;
    private List<IkasanFlowComponent> flowComponentList = new ArrayList<>();

    public IkasanFlow () {
        super();
        this.properties = new TreeMap<String, IkasanComponentProperty>();
        this.properties.put(IkasanComponentPropertyMeta.NAME, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT));
        this.properties.put(IkasanComponentPropertyMeta.DESCRIPTION, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_DESCIPTION_META_COMPONENT));

        this.viewHandler = new IkasanFlowViewHandler(this);
    }
    public List<IkasanFlowComponent> getFlowComponentList() {
        return flowComponentList;
    }

    public boolean addFlowElement(IkasanFlowComponent ikasanFlowComponent) {
        return flowComponentList.add(ikasanFlowComponent);
    }

    public void removeFlowElement(IkasanFlowComponent ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null && flowComponentList.size() > 0) {
            getFlowComponentList().remove(ikasanFlowComponentToBeRemoved);
        }
    }

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent
     * @return
     */
    public boolean isValidToAdd(IkasanFlowComponentType newComponent) {
        if (newComponent != null &&
            hasConsumer() && IkasanFlowComponentCategory.CONSUMER.equals(newComponent.getElementCategory()) ||
            hasProducer() && IkasanFlowComponentCategory.PRODUCER.equals(newComponent.getElementCategory())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine the current state of the flow for completeness
     * @return A status string
     */
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

    /**
     * Return true if it is valid to add the supplied component
     * @param newComponent
     * @return
     */
    public boolean issueCausedByAdding(IkasanFlowComponentType newComponent) {
        if (newComponent != null &&
                hasConsumer() && IkasanFlowComponentCategory.CONSUMER.equals(newComponent.getElementCategory()) ||
                hasProducer() && IkasanFlowComponentCategory.PRODUCER.equals(newComponent.getElementCategory())) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasConsumer() {
        return flowComponentList.stream()
                .filter(e->e.getType().getElementCategory().equals(IkasanFlowComponentCategory.CONSUMER))
                .findFirst()
                .isPresent();
    }

    public boolean hasProducer() {
        return flowComponentList.stream()
                .filter(e->e.getType().getElementCategory().equals(IkasanFlowComponentCategory.PRODUCER))
                .findFirst()
                .isPresent();
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

    @Override
    public String toString() {
        return "IkasanFlow{" +
                "name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", flowElementList=" + flowComponentList +
                '}';
    }
}
