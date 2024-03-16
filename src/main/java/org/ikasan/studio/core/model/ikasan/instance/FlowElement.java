package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowElementSerializer;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
@Data
@EqualsAndHashCode(callSuper=true)
@AllArgsConstructor
public class FlowElement extends BasicElement {
    @JsonIgnore
    private Flow containingFlow;

    public FlowElement() {}

    @Builder (builderMethodName = "flowElementBuilder")
    protected FlowElement(Flow containingFlow, ComponentMeta componentMeta, String componentName) {
        super(componentMeta, null);
        setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, componentName);
        this.containingFlow = containingFlow;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", flowComponent='" + getComponentMeta() + '\'' +
                ", name='" + getComponentName() + '\'' +
                ", properties=" + configuredProperties +
                '}';
    }
}
