package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.ui.viewmodel.IkasanFlowElementViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.Map;
import java.util.TreeMap;

public class IkasanFlowElement {
    private IkasanFlow parent;
    private ViewHandler viewHandler;
    private String name;
    private String description;
    private IkasanFlowElementType type;
    private Map<String, Object> properties;

    public IkasanFlowElement() {
        this.properties = new TreeMap<>();
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

    /**
     *
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    public IkasanFlowElement(IkasanFlow parent, String name, String description) {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.properties = new TreeMap<>();
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

    /**
     * The type influences the view handler
     * @param type
     */
    public void setTypeAndViewHandler(IkasanFlowElementType type) {
        this.type = type;
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

    public void setParent(IkasanFlow parent) {
        this.parent = parent;
    }

    public IkasanFlowElementType getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    @Override
    public String toString() {
        return "IkasanFlowElement{" +
                "viewHandler=" + viewHandler +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", properties=" + properties +
                '}';
    }
}
