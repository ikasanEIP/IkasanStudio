package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
public class IkasanFlowComponent extends IkasanComponent {
    @JsonIgnore
    private IkasanFlow parent;

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    protected IkasanFlowComponent(IkasanComponentType type, IkasanFlow parent, String name, String description) {
        super (type, type.getMandatoryProperties());
        this.parent = parent;
        updatePropertyValue(IkasanComponentPropertyMeta.NAME, name);
        updatePropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, description);
        viewHandler = new IkasanFlowComponentViewHandler(this);
    }

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    protected IkasanFlowComponent(IkasanComponentType type, IkasanFlow parent) {
        this(type, parent, "", "");
    }


    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    public static IkasanFlowComponent getInstance(IkasanComponentType type, IkasanFlow parent, String name, String description) {
        if (type.isBespokeClass()) {
            return new IkasanFlowBeskpokeComponent(type, parent, name, description, false);
        } else {
            return new IkasanFlowComponent(type, parent, name, description);
        }
    }

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public static IkasanFlowComponent getInstance(IkasanComponentType type, IkasanFlow parent) {
        if (type.isBespokeClass()) {
            return new IkasanFlowBeskpokeComponent(type, parent, false);
        } else {
            return new IkasanFlowComponent(type, parent);
        }
    }

    public IkasanFlow getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", flowComponent='" + getType() + '\'' +
                ", name='" + getName() + '\'' +
                ", properties=" + configuredProperties +
                '}';
    }

    /**
     * A helper to extract the destination name for a component that might have one e.g. JMS, FTP
     * @return
     */
    public String getDestinationName() {
        String destinationName = "";
        if (type.isJms()) {
            IkasanComponentProperty destination = this.getConfiguredProperties().get("DestinationJndiName");
            if (destination != null && destination.getValue() != null) {
                    destinationName = destination.getValueString();
            }
        }
        return destinationName;
    }
}
