package org.ikasan.studio.core.model.ikasan.instance;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

import java.util.List;

@EqualsAndHashCode(callSuper=true)
public class FlowUserImplementedElement extends FlowElement {
    private boolean overwriteEnabled;    // The auto-generated template will not overwrite any existing code unless this is true.

    /**
     * FlowUserImplementedElement is a flow element that has its implementation provided for by a user supplied class
     * @param componentMeta e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param containingFlow for this element
     * @param containingFlowRoute for this element
     * @param componentName for this element
     * @param decorators e.g. wiretaps
     * @param overwriteEnabled the stub in the user's project will only be regenerated if this is true
     */
    @Builder(builderMethodName = "flowUserImplementedElementBuilder")
    protected FlowUserImplementedElement(ComponentMeta componentMeta, Flow containingFlow, FlowRoute containingFlowRoute, String componentName, List<Decorator> decorators, boolean overwriteEnabled) {
        super(componentMeta, containingFlow, containingFlowRoute, componentName, decorators);
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
                ", properties=" + componentProperties +
                '}';
    }
}
