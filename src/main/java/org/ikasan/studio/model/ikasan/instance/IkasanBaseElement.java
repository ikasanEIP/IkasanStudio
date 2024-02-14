package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
@Data
public class IkasanBaseElement {
//public abstract class IkasanBaseElement {
    // The view handler has a number of circular dependencies to be avoided.
    @JsonIgnore
    protected ViewHandler viewHandler;
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    protected IkasanComponentMeta ikasanComponentMeta;

    public IkasanBaseElement() {}

    protected IkasanBaseElement(IkasanComponentMeta ikasanComponentMeta) {
        this.ikasanComponentMeta = ikasanComponentMeta;
    }

}
