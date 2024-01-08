package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentTypeMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
@Data
public abstract class IkasanBaseElement {
    // The view handler has a number of circular dependencies to be avoided.
    @JsonIgnore
    protected ViewHandler viewHandler;
    @JsonPropertyOrder(alphabetic = true)
    protected IkasanComponentTypeMeta ikasanComponentTypeMeta;

    public IkasanBaseElement() {}

    protected IkasanBaseElement(IkasanComponentTypeMeta ikasanComponentTypeMeta) {
        this.ikasanComponentTypeMeta = ikasanComponentTypeMeta;
    }

    public String getConponentType() {
        return this.ikasanComponentTypeMeta.getComponentType();
    }
//    @JsonIgnore
//    public ViewHandler getViewHandler() {
//        return viewHandler;
//    }
//
//
//    public IkasanComponentType getType() {
//        return type;
//    }
//
//    public void setType(IkasanComponentType type) {
//        this.type = type;
//    }
//
//    @Override
//    public String toString() {
//        return "IkasanComponent{" +
//                ", type=" + type +
//                '}';
//    }
}
