package org.ikasan.studio.model.Ikasan;

import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class IkasanComponent {
    private static final Logger log = Logger.getLogger(IkasanComponent.class);
    protected ViewHandler viewHandler;
    protected Map<String, IkasanComponentProperty> properties = new TreeMap<>();
    private String javaVariableName;

    public IkasanComponentProperty getProperty(String key) {
        return properties.get(key);
    }

    public Object getPropertyValue(String key) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        return ikasanComponentProperty != null ? ikasanComponentProperty.getValue() : null;
    }

    /**
     * Set the value of the (existing) property. Properties have associated meta data so we can't just add values.
     * @param key of the data to be updated
     * @param value for the updated property
     */
    public void updatePropertyValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            log.warn("Attempt made to update non-existant property will be ignored key =" + key + " value " + value);
        }
    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param properyMeta for the property
     * @param value for the property
     */
    public void setPropertyValue(String key, IkasanComponentPropertyMeta properyMeta, Object value) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            properties.put(key, new IkasanComponentProperty(properyMeta, value));
        }
    }
    public Map<String, IkasanComponentProperty> getProperties() {
        return properties;
    }


    /**
     * Get all the standard properties i.e. exclude the special 'name and description' properties
     * @return
     */
    public Map<String, IkasanComponentProperty> getStandardProperties() {
        if (properties != null && properties.size() > 0) {
            Map<String, IkasanComponentProperty> standardProperties = properties.entrySet().stream()
                    .filter(x -> ! x.getKey().equals(IkasanComponentPropertyMeta.NAME) && !x.getKey().equals(IkasanComponentPropertyMeta.DESCRIPTION))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return standardProperties;
        }
        return properties;
    }

    public void addAllProperties(Map<String, IkasanComponentProperty> newProperties) {
        properties.putAll(newProperties);
    }
    public void addComponentProperty(String key, IkasanComponentProperty value) {
        properties.put(key, value);
    }

    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    public String getName() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.NAME);
    }

    /**
     * Set the screen name (and indicate the java variable name) for this component
     * @param name for the instance of this component.
     */
    public void setName(String name) {
        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        if (name != null && name.length() > 0) {
            javaVariableName = StudioUtils.toJavaIdentifier(name);
        }
    }

    public String getDescription() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }
    public void setDescription(String description) {
        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCIPTION_META_COMPONENT, description);
    }

    /**
     * The setter is deliberably private since the data should be set via setName
     * @param javaVariableName for the instance of this component
     */
    private void setJavaVariableName(String javaVariableName) {
        this.javaVariableName = javaVariableName;
    }
    public String getJavaVariableName() {
        return javaVariableName;
    }
}
