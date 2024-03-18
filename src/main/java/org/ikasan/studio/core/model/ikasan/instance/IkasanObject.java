package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
@Data
public class IkasanObject {
    // The view handler could be thought of as polluting the build namespace since it serves no purpose
    // or value here. However, having a reference handle here, means we can plug in the view handlers
    // very easily when the object is used in the UI space, otherwise we would need to deal with very complex
    // parallel structures. Including the handle here is the lesser evil.
    @JsonIgnore
    protected ViewHandler viewHandler;
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    protected ComponentMeta componentMeta;

    public IkasanObject() {}
    protected IkasanObject(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }
}