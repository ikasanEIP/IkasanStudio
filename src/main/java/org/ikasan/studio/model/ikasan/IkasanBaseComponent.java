package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
public abstract class IkasanBaseComponent {
    // The view handler has a number of circular dependencies to be avoided.
    @JsonIgnore
    protected ViewHandler viewHandler;
    @JsonPropertyOrder(alphabetic = true)
    protected IkasanComponentType type;

    private IkasanBaseComponent() {}

    protected IkasanBaseComponent(IkasanComponentType type) {
        this.type = type;
    }
    @JsonIgnore
    public ViewHandler getViewHandler() {
        return viewHandler;
    }


    public IkasanComponentType getType() {
        return type;
    }

    public void setType(IkasanComponentType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "IkasanComponent{" +
                ", type=" + type +
                '}';
    }
}
