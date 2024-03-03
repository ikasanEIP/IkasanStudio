package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
@Data
@EqualsAndHashCode(callSuper=true)
public class FlowElement extends BasicElement {
    @JsonIgnore
    private final Flow containingFlow;

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
    }

//    /**
//     * Return the name of this component in a format that would be appropriate to be used as a java class name
//     * @return the class name format of the component name.
//     */
//    @JsonIgnore
//    @Override
//    public String getJavaClassName() {
//        return StudioUtils.toJavaClassName(getComponentName());
//    }
//
//    @JsonIgnore
//    public Flow getContainingFlow() {
//        return containingFlow;
//    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", flowComponent='" + getComponentMeta() + '\'' +
                ", name='" + getComponentName() + '\'' +
                ", properties=" + configuredProperties +
                '}';
    }
}
