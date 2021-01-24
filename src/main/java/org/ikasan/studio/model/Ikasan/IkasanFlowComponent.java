package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.ui.viewmodel.IkasanFlowElementViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.Map;

public class IkasanFlowComponent {
    private IkasanFlow parent;
    private ViewHandler viewHandler;
//    private String name;
//    private String description;
    private IkasanFlowComponentType type;
    private Map<String, IkasanComponentProperty> properties;

//    public IkasanFlowComponent() {
//        this(null, )
//        this.properties = new TreeMap<>();
//        viewHandler = new IkasanFlowElementViewHandler(this);
//    }

    /**
     *
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    public IkasanFlowComponent(IkasanFlowComponentType type, IkasanFlow parent, String name, String description) {
        this.type = type;
        this.parent = parent;
        this.properties = type.getMandatoryProperties();
        setValue(IkasanComponentPropertyMeta.NAME, name);
        setValue(IkasanComponentPropertyMeta.DESCRIPTION, description);
//        this.description = description;
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

    public IkasanComponentProperty getProperty(String key) {
        return properties.get(key);
    }

    public Object getValue(String key) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        return ikasanComponentProperty != null ? ikasanComponentProperty.getValue() : null;
    }

    public void setValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        }
    }

    /**
     *
     * @param type
     * @param parent
     */
    public IkasanFlowComponent(IkasanFlowComponentType type, IkasanFlow parent) {
        this(type, parent, "", "");
    }

    /**
     * The type influences the view handler
     * @param type
     */
    public void setTypeAndViewHandler(IkasanFlowComponentType type) {
        this.type = type;
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

//    public void setParent(IkasanFlow parent) {
//        this.parent = parent;
//    }

    public IkasanFlowComponentType getType() {
        return type;
    }

    public Map<String, IkasanComponentProperty> getProperties() {
        return properties;
    }

    public void addAllProperties(Map<String, IkasanComponentProperty> newProperties) {
        properties.putAll(newProperties);
    }

//    public void setProperties(Map<String, Object> properties) {
//        this.properties = properties;
//    }

    public String getName() {
        return (String)getValue(IkasanComponentPropertyMeta.NAME);
    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
    public String getDescription() {
        return (String)getValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }


    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent{" +
//                "viewHandler=" + viewHandler +
                ", name='" + getName() + '\'' +
//                ", description='" + description + '\'' +
                ", type=" + type +
                ", properties=" + properties +
                '}';
    }
}
