package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
@Data
public class IkasanOject {
    // The view handler has a number of circular dependencies to be avoided.
    @JsonIgnore
    protected ViewHandler viewHandler;
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    protected ComponentMeta componentMeta;

    public IkasanOject() {}

    protected IkasanOject(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }
}
