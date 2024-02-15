package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import org.ikasan.studio.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
public class FlowElement extends IkasanElement {
    @JsonIgnore
    private final Flow containingFlow;

//    /**
//     * Any component that belongs in the flow
//     * @param componentMeta e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
//     * @param parent flow that contains this element
//     * @param name of the element
//     * @param description of the element
//     */
//    protected FlowElement(IkasanComponentMeta componentMeta, Flow parent, String name, String description) {
//        super (componentMeta);
//        this.parent = parent;
//        if (name != null) {
//            setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
//        }
//        if (description != null) {
//            setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
//        }
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
//    }
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param containingFlow flow that contains this element
     */
    public FlowElement(IkasanComponentMeta type, Flow containingFlow) {
        super (type);

        setPropertyValue(IkasanComponentPropertyMeta.COMPONENT_NAME, "");
//        setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, "");
//        setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, "");
        setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, "");
        this.containingFlow = containingFlow;
//        this(type, parent, "", "");
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    @Builder (builderMethodName = "flowElementBuilder")
    protected FlowElement(Flow containingFlow, IkasanComponentMeta componentMeta, String componentName, String description) {
        super(componentMeta, description);
        setPropertyValue(IkasanComponentPropertyMeta.COMPONENT_NAME, componentName);
        this.containingFlow = containingFlow;
    }
//    public static FlowElement getDummyFlowElement() {
//        return new FlowElement(IkasanComponentMeta.builder().build(), null);
//    }

//
//    /**
//     * Any component that belongs in the flow
//     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
//     * @param parent flow that contains this element
//     * @param name of the element
//     * @param description of the element
//     * @todo maybe this should be in a factory, not in this class
//     */
//    public static FlowElement createFlowElement(IkasanComponentMeta type, Flow parent, String name, String description) {
//        if (type.isBespokeClass()) {
//            return new IkasanFlowBeskpokeElement(type, parent, name, description, false);
//        } else if (type.isExceptionResolver()) {
//            return new ExceptionResolver(parent);
//        } else {
//            return new FlowElement(type, parent, name, description);
//        }
//    }


    @JsonIgnore
    public Flow getContainingFlow() {
        return containingFlow;
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
