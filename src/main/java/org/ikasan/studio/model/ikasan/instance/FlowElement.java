package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
public class FlowElement extends BasicElement {
    @JsonIgnore
    private final Flow containingFlow;

//    /**
//     * Any component that belongs in the flow
//     * @param componentMeta e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
//     * @param parent flow that contains this element
//     * @param name of the element
//     * @param description of the element
//     */
//    protected FlowElement(ComponentMeta componentMeta, Flow parent, String name, String description) {
//        super (componentMeta);
//        this.parent = parent;
//        if (name != null) {
//            setPropertyValue(ComponentPropertyMeta.NAME, ComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
//        }
//        if (description != null) {
//            setPropertyValue(ComponentPropertyMeta.DESCRIPTION, ComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
//        }
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
//    }
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param containingFlow flow that contains this element
     */
    public FlowElement(ComponentMeta type, Flow containingFlow) {
        super (type);

        setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, "");
        setPropertyValue(ComponentPropertyMeta.DESCRIPTION, "");
        this.containingFlow = containingFlow;
    }

    @Builder (builderMethodName = "flowElementBuilder")
    protected FlowElement(Flow containingFlow, ComponentMeta componentMeta, String componentName, String description) {
        super(componentMeta, description);
        setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, componentName);
        this.containingFlow = containingFlow;
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }
//    public static FlowElement getDummyFlowElement() {
//        return new FlowElement(ComponentMeta.builder().build(), null);
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
//    public static FlowElement createFlowElement(ComponentMeta type, Flow parent, String name, String description) {
//        if (type.isBespokeClass()) {
//            return new FlowBeskpokeElement(type, parent, name, description, false);
//        } else if (type.isExceptionResolver()) {
//            return new ExceptionResolver(parent);
//        } else {
//            return new FlowElement(type, parent, name, description);
//        }
//    }


    /**
     * Return the name of this component in a format that would be appropriate to be used as a java class name
     * @return the class name format of the component name.
     */
    @JsonIgnore
    @Override
    public String getJavaClassName() {
        return StudioUtils.toJavaClassName(getComponentName());
    }

    @JsonIgnore
    public Flow getContainingFlow() {
        return containingFlow;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", flowComponent='" + getComponentMeta() + '\'' +
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
//            ComponentProperty destination = this.getProperty("DestinationJndiName");
//            if (destination != null && destination.getValue() != null) {
//                    destinationName = destination.getValueString();
//            }
//        }
//        return destinationName;
//    }
}
