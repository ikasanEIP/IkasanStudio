package org.ikasan.studio.model.ikasan.instance;

import org.ikasan.studio.model.ikasan.meta.ComponentMeta;

public class FlowElementFactory {
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public static FlowElement createFlowElement(ComponentMeta type, Flow parent) {
        if (type.isBespokeClass()) {
            return new FlowBeskpokeElement(type, parent, false);
        } else if (type.isExceptionResolver()) {
            return new ExceptionResolver(parent);
        } else {
            return new FlowElement(type, parent);
        }
    }
}
