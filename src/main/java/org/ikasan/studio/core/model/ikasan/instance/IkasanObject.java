package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ikasan.studio.core.metapack.model.ComponentMeta;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
@Getter
@Setter
@ToString
public abstract class IkasanObject implements IkasanComponent {
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    protected ComponentMeta componentMeta;

    public IkasanObject() {}
    protected IkasanObject(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }
}
