package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class IkasanComponent {
    @JsonIgnore
    private static final Logger log = Logger.getLogger(IkasanComponent.class);
    @JsonIgnore
    protected ViewHandler viewHandler;
    protected Map<String, IkasanComponentProperty>properties;
    protected IkasanComponentType type;

    private IkasanComponent() {}

    protected IkasanComponent(IkasanComponentType type, Map<String, IkasanComponentProperty> properties) {
        this.type = type;
        this.properties = properties;
    }

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

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = properties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            IkasanComponentPropertyMeta properyMeta = getType().getMetadata(key);
            if (properyMeta == null) {
                log.error("SERIOUS ERROR - Attempt to set property " + key + " with value [" + value + "] but not such meta data exists for " + getType() + " this property will be ignored.");
            } else {
                properties.put(key, new IkasanComponentProperty(getType().getMetadata(key), value));
            }
        }
    }

    public Map<String, IkasanComponentProperty> getProperties() {
        return properties;
    }

    /**
     * Convenience getter to return User Implemented class properties
     * @return User Implemented class properties
     */
    @JsonIgnore
    public List<IkasanComponentProperty> getUserImplementedClassProperties() {
        return properties.values().stream()
            .filter(x -> x.getMeta().userImplementedClass && x.isRegenerateAllowed())
            .collect(Collectors.toList());
    }

    public boolean hasUserImplementedClass() {
        return properties.values()
            .stream()
            .anyMatch(x -> x.getMeta().userImplementedClass && x.isRegenerateAllowed());
    }

    public void resetUserImplementedClassPropertiesRegenratePermission() {
        for (IkasanComponentProperty property : getUserImplementedClassProperties()) {
            property.setRegenerateAllowed(false);
        }
    }

    /**
     * Get all the standard properties i.e. exclude the special 'name and description' properties.
     * @return the Map of standard properties for this component.
     */
    public Map<String, IkasanComponentProperty> getStandardProperties() {
        Map<String, IkasanComponentProperty> standardProperties = new HashedMap();
        if (properties != null && properties.size() > 0) {
            for (Map.Entry<String, IkasanComponentProperty> entry : properties.entrySet()) {
                if (! IkasanComponentPropertyMeta.NAME.equals(entry.getKey()) && !(IkasanComponentPropertyMeta.DESCRIPTION.equals(entry.getKey()))) {
                    standardProperties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
//            Map<String, IkasanComponentProperty> standardProperties = properties.entrySet().stream()
//                    .filter(x -> ! IkasanComponentPropertyMeta.NAME.equals(x.getKey()) && ! IkasanComponentPropertyMeta.DESCRIPTION.equals(x.getKey()))
//                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        }
        return standardProperties;
//        return properties;
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
     * Return the name of this component in a format that would be appropriate to be used as a java class name
     * @return the class name format of the component name.
     */
    @JsonIgnore
    public String getJavaClassName() {
        return StudioUtils.toJavaClassName(getName());
    }

    /**
     * Return the name of this component in a format that would be appropriate to be used as a component in a package name
     * @return the package name format of the component name.
     */
    @JsonIgnore
    public String getJavaPackageName() {
        return StudioUtils.toJavaPackageName(getName());
    }

    @JsonIgnore
    public String getJavaVariableName() {
        return StudioUtils.toJavaIdentifier(getName());
    }

    /**
     * Set the screen name (and indicate the java variable name) for this component
     * @param name for the instance of this component.
     */
    public void setName(String name) {
        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
    }

    public String getDescription() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }
    public void setDescription(String description) {
        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCIPTION_META_COMPONENT, description);
    }


    /**
     * Determine if there are some mandatory properties that have not yet been set.
     * @return true if there are mandatory components that do not yet have a value.
     */
    public boolean hasUnsetMandatoryProperties() {
        for (Map.Entry<String, IkasanComponentProperty> entry : properties.entrySet()) {
            IkasanComponentProperty ikasanComponentProperty = entry.getValue();
            if (ikasanComponentProperty.getMeta().isMandatory() && ikasanComponentProperty.isEmpty()) {
                return true;
            }
        }
        return false;
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
                "properties=" + properties +
                ", type=" + type +
                '}';
    }
}
