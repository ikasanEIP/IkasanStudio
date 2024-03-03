package org.ikasan.studio.model.ikasan.instance;

import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;

@EqualsAndHashCode(callSuper=true)
public class FlowBeskpokeElement extends FlowElement {
    private boolean overwiteEnabled;    // The auto-generated template will not overwrite any existing code unless this is true.

    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param parent flow that contains this element
     * @param overwiteEnabled to allow the bespoke class to be regenerated from the template
     */
    protected FlowBeskpokeElement(ComponentMeta type, Flow parent, boolean overwiteEnabled) {
        super(type, parent);
        this.overwiteEnabled = overwiteEnabled;
    }

    public boolean isOverwiteEnabled() {
        return overwiteEnabled;
    }

    public void setOverwiteEnabled(boolean overwiteEnabled) {
        this.overwiteEnabled = overwiteEnabled;
    }

    @Override
    public String toString() {
        return "IkasanFlowBeskpokeComponent{" +
                "overrideEnabled=" + overwiteEnabled +
                ", viewHandler=" + viewHandler +
                ", properties=" + configuredProperties +
                '}';
    }
}
