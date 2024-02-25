package org.ikasan.studio.model.ikasan.instance;

import org.ikasan.studio.model.ikasan.meta.ComponentMeta;

public class FlowBeskpokeElement extends FlowElement {
    private boolean overrideEnabled;

//    /**
//     * Any component that belongs in the flow
//     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
//     * @param parent flow that contains this element
//     * @param name of the element
//     * @param description of the element
//     * @param overrideEnabled to allow the bespoke class to be regenerated from the template
//     */
//    public FlowBeskpokeElement(ComponentMeta type, Flow parent, String name, String description, boolean overrideEnabled) {
//        super (type, parent, name, description);
//        this.overrideEnabled = overrideEnabled;
//    }

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param overrideEnabled to allow the bespoke class to be regenerated from the template
     */
    protected FlowBeskpokeElement(ComponentMeta type, Flow parent, boolean overrideEnabled) {
        super(type, parent);
        this.overrideEnabled = overrideEnabled;
    }

    public boolean isOverrideEnabled() {
        return overrideEnabled;
    }

    public void setOverrideEnabled(boolean overrideEnabled) {
        this.overrideEnabled = overrideEnabled;
    }

    @Override
    public String toString() {
        return "IkasanFlowBeskpokeComponent{" +
                "overrideEnabled=" + overrideEnabled +
                ", viewHandler=" + viewHandler +
                ", properties=" + configuredProperties +
                '}';
    }
}
