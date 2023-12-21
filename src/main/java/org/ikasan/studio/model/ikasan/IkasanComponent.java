package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections.map.HashedMap;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 */
public abstract class IkasanComponent extends IkasanBaseComponent {
    @JsonIgnore
    private static final Logger LOG = Logger.getInstance("#IkasanComponent");
    @JsonPropertyOrder(alphabetic = true)
    protected Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> configuredProperties;


    protected IkasanComponent(IkasanComponentType type, Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> configuredProperties) {
        super(type);
        this.configuredProperties = configuredProperties;
    }
    @JsonIgnore
    public IkasanComponentProperty getProperty(String key) {
        return configuredProperties.get(new IkasanComponentPropertyMetaKey(key));
    }
    @JsonIgnore
    public IkasanComponentProperty getProperty(IkasanComponentPropertyMetaKey key) {
        return configuredProperties.get(key);
    }
    @JsonIgnore
    public Object getPropertyValue(String key) {
        return getPropertyValue(new IkasanComponentPropertyMetaKey(key));
    }

//    @JsonIgnore
//    public Object getPropertyValue(String key, int parameterGroup, int parameterNumber) {
//        return getPropertyValue(new IkasanComponentPropertyMetaKey(key, parameterGroup, parameterNumber));
//    }
    @JsonIgnore
    public Object getPropertyValue(IkasanComponentPropertyMetaKey key) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        return ikasanComponentProperty != null ? ikasanComponentProperty.getValue() : null;
    }

    /**
     * Set the value of the (existing) property. Properties have associated meta data so we can't just add values.
     * @param key of the data to be updated
     * @param value for the updated property
     */
    public void updatePropertyValue(String key, Object value) {
        updatePropertyValue(new IkasanComponentPropertyMetaKey(key), value);
    }

    /**
     * Set the value of the (existing) property. Properties have associated meta data so we can't just add values.
     * @param key of the data to be updated
     * @param value for the updated property
     */
    public void updatePropertyValue(IkasanComponentPropertyMetaKey key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            LOG.warn("Attempt made to update non-existant property will be ignored key =" + key + " value " + value);
        }
    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param properyMeta for the property
     * @param value for the property
     */
    public void setPropertyValue(IkasanComponentPropertyMetaKey key, IkasanComponentPropertyMeta properyMeta, Object value) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            configuredProperties.put(key, new IkasanComponentProperty(properyMeta, value));
        }
    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(String key, Object value) {
        // If we are stating a string key on its own, assume its the simple version.
        setPropertyValue(new IkasanComponentPropertyMetaKey(key), value);
    }

    /**
     * remove a property for the given key
     * @param key of the property to be updated
     */
    public void removeProperty(String key) {
        configuredProperties.remove(new IkasanComponentPropertyMetaKey(key));
    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(IkasanComponentPropertyMetaKey key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            IkasanComponentPropertyMeta properyMeta = getType().getMetadata(key);
            if (properyMeta == null) {
                LOG.warn("SERIOUS ERROR - Attempt to set property " + key + " with value [" + value + "] but no such meta data exists for " + getType() + " this property will be ignored.");
            } else {
                configuredProperties.put(key, new IkasanComponentProperty(getType().getMetadata(key), value));
            }
        }
    }

    @JsonPropertyOrder(alphabetic = true)
    public Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> getConfiguredProperties() {
        return configuredProperties;
    }

    /**
     * Convenience getter to return User Implemented class properties
     * @return User Implemented class properties
     */
    @JsonIgnore
    public List<IkasanComponentProperty> getUserImplementedClassProperties() {
        return configuredProperties.values().stream()
            .filter(x -> x.getMeta().userImplementedClass && x.isRegenerateAllowed())
            .collect(Collectors.toList());
    }

    public boolean hasUserImplementedClass() {
        return configuredProperties.values()
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
    @JsonIgnore
    public Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> getStandardConfiguredProperties() {
        Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> standardProperties = new HashedMap();
        if (configuredProperties != null && !configuredProperties.isEmpty()) {
            for (Map.Entry<IkasanComponentPropertyMetaKey, IkasanComponentProperty> entry : configuredProperties.entrySet()) {
                if (! IkasanComponentPropertyMeta.NAME.equals(entry.getKey()) && !(IkasanComponentPropertyMeta.DESCRIPTION.equals(entry.getKey()))) {
                    standardProperties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        return standardProperties;
    }

    public void addAllProperties(Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> newProperties) {
        configuredProperties.putAll(newProperties);
    }
    public void addComponentProperty(String key, IkasanComponentProperty value) {
        configuredProperties.put(new IkasanComponentPropertyMetaKey(key), value);
    }

    /**
     * Convenience method to access the standard property called name. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    @JsonIgnore
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
     * Convenience method to access the standard property called description. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    @JsonIgnore
    public String getDescription() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }


    /**
     * Set the screen name (and indicate the java variable name) for this component
     * @param name for the instance of this component.
     */
    public void setName(String name) {
        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
    }

    /**
     * Set the description for this component
     * @param description for the component
     */
    public void setDescription(String description) {
        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
    }

    /**
     * Determine if there are some mandatory properties that have not yet been set.
     * @return true if there are mandatory components that do not yet have a value.
     */
    public boolean hasUnsetMandatoryProperties() {
        return configuredProperties.entrySet().stream()
            .anyMatch(x -> x.getValue().getMeta().isMandatory() && x.getValue().valueNotSet());
//
//        for (Map.Entry<IkasanComponentPropertyMetaKey, IkasanComponentProperty> entry : configuredProperties.entrySet()) {
//            IkasanComponentProperty ikasanComponentProperty = entry.getValue();
//            if (ikasanComponentProperty.getMeta().isMandatory() && ikasanComponentProperty.valueNotSet()) {
//                return true;
//            }
//        }
//        return false;
    }

    @Override
    public String toString() {
        return "IkasanComponent{" +
                "properties=" + configuredProperties +
                ", type=" + type +
                '}';
    }
}
