package org.ikasan.studio.model.ikasan.instance;

import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;

@EqualsAndHashCode(callSuper=true)
public class FlowUserImplementedElement extends FlowElement {
    private boolean overwriteEnabled;    // The auto-generated template will not overwrite any existing code unless this is true.

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param overwriteEnabled to allow the user implemented class to be regenerated from the template
     */
    protected FlowUserImplementedElement(ComponentMeta type, Flow parent, boolean overwriteEnabled) {
        super(type, parent);
        this.overwriteEnabled = overwriteEnabled;
    }

    public boolean isOverwriteEnabled() {
        return overwriteEnabled;
    }

    public void setOverwriteEnabled(boolean overwriteEnabled) {
        this.overwriteEnabled = overwriteEnabled;
    }

    @Override
    public String toString() {
        return "IkasanFlowBeskpokeComponent{" +
                "overrideEnabled=" + overwriteEnabled +
                ", viewHandler=" + viewHandler +
                ", properties=" + configuredProperties +
                '}';
    }
}
