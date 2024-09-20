package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 * The view handler could be thought of as polluting the namespace since it serves no purpose
 * or value here. However, having a reference handle here, means we can plug in the view handlers
 * very easily when the object is used in the UI space, otherwise we would need to deal with very complex
 * parallel structures. Including the handle here is the lesser evil.
 */
@Getter
@Setter
@ToString
public class IkasanObject extends AbstractViewHandler implements IkasanComponent {
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    protected ComponentMeta componentMeta;

    public IkasanObject() {}
    protected IkasanObject(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }

    public String getName() {
        if (componentMeta != null) {
            return componentMeta.getName();
        } else {
            return "Object not yet set";
        }
    }
}
