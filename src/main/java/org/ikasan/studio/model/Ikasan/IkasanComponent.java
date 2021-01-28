package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.Map;

public class IkasanComponent {
    protected ViewHandler viewHandler;
    protected Map<String, IkasanComponentProperty> properties;
    private String javaVariableName;

    public IkasanComponentProperty getProperty(String key) {
        return properties.get(key);
    }

    public Object getPropertyValue(String key) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        return ikasanComponentProperty != null ? ikasanComponentProperty.getValue() : null;
    }

    public void setPropertyValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        }
    }

    public Map<String, IkasanComponentProperty> getProperties() {
        return properties;
    }

    public void addAllProperties(Map<String, IkasanComponentProperty> newProperties) {
        properties.putAll(newProperties);
    }
    public void addComponentProperty(String key, IkasanComponentProperty value) {
        properties.put(key, value);
    }

    public String getName() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.NAME);
    }
    public String getDescription() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }
    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    public void setName(String name) {
        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, name);
        if (name != null && name.length() > 0) {
            javaVariableName = StudioUtils.toJavaIdentifier(name);
        }
    }

    public void setDescription(String description) {
        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, description);
    }

    public String getJavaVariableName() {
        return javaVariableName;
    }

}
