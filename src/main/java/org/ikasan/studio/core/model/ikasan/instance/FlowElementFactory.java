package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

public class FlowElementFactory {
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public static FlowElement createFlowElement(ComponentMeta type, Flow parent, String componentName) {
        if (type.isGeneratesUserImplementedClass()) {
            return new FlowUserImplementedElement(type, parent, false);
        } else if (type.isExceptionResolver()) {
            return new ExceptionResolver(parent);
        } else {
            return FlowElement.flowElementBuilder().componentMeta(type).containingFlow(parent).componentName(componentName).build();
        }
    }
}
