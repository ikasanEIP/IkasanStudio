package org.ikasan.studio.model.ikasan.instance;

import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;

public class FlowElementFactory {
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     */
    public static FlowElement createFlowElement(IkasanComponentMeta type, Flow parent) {
        if (type.isBespokeClass()) {
            return new IkasanFlowBeskpokeElement(type, parent, false);
        } else if (type.isExceptionResolver()) {
            return new ExceptionResolver(parent);
        } else {
            return new FlowElement(type, parent);
        }
    }
}
