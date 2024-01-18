package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMetax;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolver;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
public class FlowElement extends IkasanElement {
    @JsonIgnore
    private Flow parent;

    /**
     * Any component that belongs in the flow
     * @param componentMeta e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    public FlowElement(IkasanComponentMeta componentMeta, Flow parent, String name, String description) {
        super (componentMeta);
        this.parent = parent;
        if (name != null) {
            setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        }
        if (description != null) {
            setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
        }
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public FlowElement(IkasanComponentMeta type, Flow parent) {
        this(type, parent, "", "");
    }

    public static FlowElement getDummyFlowElement() {
        return new FlowElement(IkasanComponentMeta.builder().build(), null);
    }


    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     * @todo maybe this should be in a factory, not in this class
     */
    public static FlowElement getElement(IkasanComponentMeta type, Flow parent, String name, String description) {
        if (type.isBespokeClass()) {
            return new IkasanFlowBeskpokeElement(type, parent, name, description, false);
        } else if (IkasanComponentMetax.EXCEPTION_RESOLVER.equals(type)) {
            return new IkasanExceptionResolver(parent);
        } else {
            return new FlowElement(type, parent, name, description);
        }
    }

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public static FlowElement getElement(IkasanComponentMeta type, Flow parent) {
        if (type.isBespokeClass()) {
            return new IkasanFlowBeskpokeElement(type, parent, false);
        } else if (IkasanComponentMetax.EXCEPTION_RESOLVER.equals(type)) {
            return new IkasanExceptionResolver(parent);
        } else {
            return new FlowElement(type, parent);
        }
    }

    @JsonIgnore
    public Flow getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", flowComponent='" + getIkasanComponentMeta() + '\'' +
                ", name='" + getComponentName() + '\'' +
                ", properties=" + configuredProperties +
                '}';
    }

//    /**
//     * A helper to extract the destination name for a component that might have one e.g. JMS, FTP
//     * @return
//     */
//    public String getDestinationName() {
//        String destinationName = "";
//        if (ikasanComponentTypeMeta.isJms()) {
//            IkasanComponentProperty destination = this.getProperty("DestinationJndiName");
//            if (destination != null && destination.getValue() != null) {
//                    destinationName = destination.getValueString();
//            }
//        }
//        return destinationName;
//    }
}
