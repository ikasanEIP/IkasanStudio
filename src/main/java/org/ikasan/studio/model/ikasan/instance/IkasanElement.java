package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.intellij.openapi.diagnostic.Logger;
import lombok.Builder;
import org.apache.commons.collections.map.HashedMap;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.serialization.IkasanElementDeserializer;
import org.ikasan.studio.model.ikasan.instance.serialization.IkasanElementSerializer;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 * To make the instance model flexible and driven by Ikasan Meta Pack, most attributes are contained within the properties map.
 * This does
 */
@JsonSerialize(using = IkasanElementSerializer.class)
@JsonDeserialize(using = IkasanElementDeserializer.class)
public  class IkasanElement extends IkasanBaseElement {
    @JsonIgnore
    private static final Logger LOG = Logger.getInstance("#IkasanComponent");
//    @JsonPropertyOrder(alphabetic = true)
    @JsonPropertyOrder({"componentName", "description"})
    protected Map<String, IkasanComponentProperty> configuredProperties;
    public IkasanElement() {}

    protected IkasanElement(IkasanComponentMeta componentMeta) {
        super(componentMeta);
        this.configuredProperties = componentMeta.getMandatoryInstanceProperties();
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }
    @Builder
    protected IkasanElement(IkasanComponentMeta componentMeta, String description) {
        super(componentMeta);
        this.configuredProperties = componentMeta.getMandatoryInstanceProperties();
        setDescription(description);
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    /**
     * Convenience method to access the standard property called name. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    public String getComponentName() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.COMPONENT_NAME);
    }

    /**
     * Set the screen name (and indicate the java variable name) for this component
     * @param name for the instance of this component.
     */
    public void setComponentName(String name) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(IkasanComponentPropertyMeta.COMPONENT_NAME, name);
    }
    public void setName(String name) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, name);
    }
    public void setVersion(String version) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(IkasanComponentPropertyMeta.VERSION, version);
    }
    public String getVersion() {
//        this.setPropertyValue(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        return (String)this.getPropertyValue(IkasanComponentPropertyMeta.VERSION);
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
     * Convenience method to access the standard property called description. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    public String getDescription() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION);
    }

    /**
     * Set the description for this component
     * @param description for the component
     */
    public void setDescription(String description) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, IkasanComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
        this.setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, description);
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


    @JsonIgnore
    public IkasanComponentProperty getProperty(String key) {
        return configuredProperties.get(key);
    }
//    @JsonIgnore
//    public IkasanComponentProperty getProperty(String key) {
//        return configuredProperties.get(key);
//    }
//    @JsonIgnore
//    public Object getPropertyValue(String key) {
//        return getPropertyValue(key);
//    }
//
//    @JsonIgnore
//    public Object getPropertyValue(String key, int parameterGroup, int parameterNumber) {
//        return getPropertyValue(new IkasanComponentPropertyMetaKey(key, parameterGroup, parameterNumber));
//    }
    @JsonIgnore
    public Object getPropertyValue(String key) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        return ikasanComponentProperty != null ? ikasanComponentProperty.getValue() : null;
    }



//    /**
//     * Set the value of the (existing) property. Properties have associated meta data so we can't just add values.
//     * @param key of the data to be updated
//     * @param value for the updated property
//     */
//    public void updatePropertyValue(String key, Object value) {
//        updatePropertyValue(key, value);
//    }

    /**
     * Set the value of the (existing) property. Properties have associated meta data so we can't just add values.
     * @param key of the data to be updated
     * @param value for the updated property
     */
    public void updatePropertyValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            LOG.warn("Attempt made to update non-existant property will be ignored key =" + key + " value " + value);
        }
    }

//    /**
//     * This setter should be used if we think the property might not already be set but will require the correct meta data
//     * @param key of the property to be updated
//     * @param properyMeta for the property
//     * @param value for the property
//     */
//    public void setPropertyValue(String key, IkasanComponentPropertyMeta properyMeta, Object value) {
//        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
//        if (ikasanComponentProperty != null) {
//            ikasanComponentProperty.setValue(value);
//        } else {
//            LOG.warn("SERIOUS ERROR - Attempt to set property " + key + " with value [" + value + "] but no such meta data exists for " + getIkasanComponentMeta() + " this property will be ignored.");
////            configuredProperties.put(key, new IkasanComponentProperty(properyMeta, value));
//        }
//    }

//    /**
//     * This setter should be used if we think the property might not already be set but will require the correct meta data
//     * @param key of the property to be updated
//     * @param value for the property
//     */
//    public void setPropertyValue(String key, Object value) {
//        // If we are stating a string key on its own, assume its the simple version.
//        configuredProperties.put(key, new IkasanComponentProperty( value));
//    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(String key, Object value) {
        IkasanComponentProperty ikasanComponentProperty = configuredProperties.get(key);
        if (ikasanComponentProperty != null) {
            ikasanComponentProperty.setValue(value);
        } else {
            IkasanComponentPropertyMeta properyMeta = getIkasanComponentMeta().getMetadata(key);
            if (properyMeta == null) {
                Thread thread = Thread.currentThread();

                LOG.warn("SERIOUS ERROR - Attempt to set property " + key + " on Element " + this.getName() + " with value [" + value + "], the known properties are " + getIkasanComponentMeta().getPropetyKeys() + " this property will be ignored." + thread.getStackTrace());
            } else {
                configuredProperties.put(key, new IkasanComponentProperty(getIkasanComponentMeta().getMetadata(key), value));
            }
        }
    }

    /**
     * remove a property for the given key
     * @param key of the property to be updated
     */
    public void removeProperty(String key) {
        configuredProperties.remove(key);
    }

    @JsonPropertyOrder(alphabetic = true)
    public Map<String, IkasanComponentProperty> getConfiguredProperties() {
        return configuredProperties;
    }

    /**
     * Convenience getter to return User Implemented class properties
     * @return User Implemented class properties
     */
    @JsonIgnore
    public List<IkasanComponentProperty> getUserImplementedClassProperties() {
        return configuredProperties.values().stream()
            .filter(x -> x.getMeta().isUserImplementedClass())
            .collect(Collectors.toList());
    }

    public boolean hasUserImplementedClass() {
        return configuredProperties.values()
            .stream()
            .anyMatch(x -> x.getMeta().isUserImplementedClass());
    }

    /**
     * ** Used in FTL **
     * Get all the standard properties i.e. exclude the special 'name and description' properties.
     * @return the Map of standard properties for this component.
     */
    @JsonIgnore
    public Map<String, IkasanComponentProperty> getStandardConfiguredProperties() {
        Map<String, IkasanComponentProperty> standardProperties = new HashedMap();
        if (configuredProperties != null && !configuredProperties.isEmpty()) {
            for (Map.Entry<String, IkasanComponentProperty> entry : configuredProperties.entrySet()) {
                if (! IkasanComponentPropertyMeta.NAME.equals(entry.getKey()) && !(IkasanComponentPropertyMeta.DESCRIPTION.equals(entry.getKey()))) {
                    standardProperties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        return standardProperties;
    }

    public void addAllProperties(Map<String, IkasanComponentProperty> newProperties) {
        configuredProperties.putAll(newProperties);
    }
    public void addComponentProperty(String key, IkasanComponentProperty value) {
        configuredProperties.put(key, value);
    }

    /**
     * Return the name of this component in a format that would be appropriate to be used as a java class name
     * @return the class name format of the component name.
     */
    @JsonIgnore
    public String getJavaClassName() {
        return StudioUtils.toJavaClassName(getComponentName());
    }

    /**
     * Determine if there are some mandatory properties that have not yet been set.
     * @return true if there are mandatory components that do not yet have a value.
     */
    public boolean hasUnsetMandatoryProperties() {
        return configuredProperties.entrySet().stream()
            .anyMatch(x -> x.getValue().getMeta().isMandatory() && x.getValue().valueNotSet());
    }

    @Override
    public String toString() {
        return "IkasanComponent{" +
                "properties=" + configuredProperties +
                ", type=" + ikasanComponentMeta +
                '}';
    }
}
