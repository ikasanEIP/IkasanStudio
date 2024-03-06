package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.intellij.openapi.diagnostic.Logger;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.map.HashedMap;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.serialization.BasicElementDeserializer;
import org.ikasan.studio.model.ikasan.instance.serialization.BasicElementSerializer;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 * To make the instance model flexible and driven by Ikasan Meta Pack, most attributes are contained within the properties map.
 * This does
 */
@JsonSerialize(using = BasicElementSerializer.class)
@JsonDeserialize(using = BasicElementDeserializer.class)
@Data
@EqualsAndHashCode(callSuper=true)
public  class BasicElement extends IkasanObject {
    @JsonIgnore
    private static final Logger LOG = Logger.getInstance("#IkasanComponent");
//    @JsonPropertyOrder(alphabetic = true)
    @JsonPropertyOrder({"componentName", "description"})
    protected Map<String, ComponentProperty> configuredProperties;
    public BasicElement() {}

    protected BasicElement(ComponentMeta componentMeta) {
        super(componentMeta);
        this.configuredProperties = componentMeta.getMandatoryInstanceProperties();
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }
    @Builder
    protected BasicElement(ComponentMeta componentMeta, String description) {
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
        return (String) getPropertyValue(ComponentPropertyMeta.COMPONENT_NAME);
    }

    /**
     * Set the screen name (and indicate the java variable name) for this component
     * @param name for the instance of this component.
     */
    public void setComponentName(String name) {
//        this.setPropertyValue(ComponentPropertyMeta.NAME, ComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, name);
    }
    public void setName(String name) {
//        this.setPropertyValue(ComponentPropertyMeta.NAME, ComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(ComponentPropertyMeta.NAME, name);
    }
    public void setVersion(String version) {
//        this.setPropertyValue(ComponentPropertyMeta.NAME, ComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        this.setPropertyValue(ComponentPropertyMeta.VERSION, version);
    }
    public String getVersion() {
//        this.setPropertyValue(ComponentPropertyMeta.NAME, ComponentPropertyMeta.STD_NAME_META_COMPONENT, name);
        return (String)this.getPropertyValue(ComponentPropertyMeta.VERSION);
    }

    /**
     * Convenience method to access the standard property called name. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    @JsonIgnore
    public String getName() {
        if (this instanceof Flow || this instanceof Module) {
            return (String) getPropertyValue(ComponentPropertyMeta.NAME);
        } else {
            // to compily wil JSON, all flow elements use component name not name
            return (String) getPropertyValue(ComponentPropertyMeta.COMPONENT_NAME);
        }
    }


    /**
     * Convenience method to access the standard property called description. Since this is in properties, set JsonIgnore
     * @return the component description
     */
    public String getDescription() {
        return (String) getPropertyValue(ComponentPropertyMeta.DESCRIPTION);
    }

    /**
     * Set the description for this component
     * @param description for the component
     */
    public void setDescription(String description) {
//        this.setPropertyValue(ComponentPropertyMeta.DESCRIPTION, ComponentPropertyMeta.STD_DESCRIPTION_META_COMPONENT, description);
        this.setPropertyValue(ComponentPropertyMeta.DESCRIPTION, description);
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
    public ComponentProperty getProperty(String key) {
        return configuredProperties.get(key);
    }
//    @JsonIgnore
//    public ComponentProperty getProperty(String key) {
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
        ComponentProperty componentProperty = configuredProperties.get(key);
        return componentProperty != null ? componentProperty.getValue() : null;
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
        ComponentProperty componentProperty = configuredProperties.get(key);
        if (componentProperty != null) {
            componentProperty.setValue(value);
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
//    public void setPropertyValue(String key, ComponentPropertyMeta properyMeta, Object value) {
//        ComponentProperty ikasanComponentProperty = configuredProperties.get(key);
//        if (ikasanComponentProperty != null) {
//            ikasanComponentProperty.setValue(value);
//        } else {
//            LOG.warn("SERIOUS ERROR - Attempt to set property " + key + " with value [" + value + "] but no such meta data exists for " + getComponentMeta() + " this property will be ignored.");
////            configuredProperties.put(key, new ComponentProperty(properyMeta, value));
//        }
//    }

//    /**
//     * This setter should be used if we think the property might not already be set but will require the correct meta data
//     * @param key of the property to be updated
//     * @param value for the property
//     */
//    public void setPropertyValue(String key, Object value) {
//        // If we are stating a string key on its own, assume its the simple version.
//        configuredProperties.put(key, new ComponentProperty( value));
//    }

    /**
     * This setter should be used if we think the property might not already be set but will require the correct meta data
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(String key, Object value) {
        ComponentProperty componentProperty = configuredProperties.get(key);
        if (componentProperty != null) {
            componentProperty.setValue(value);
        } else {
            ComponentPropertyMeta properyMeta = getComponentMeta().getMetadata(key);
            if (properyMeta == null) {
                Thread thread = Thread.currentThread();

                LOG.warn("SERIOUS ERROR - Attempt to set property " + key + " on Element " + this.getName() + " with value [" + value + "], the known properties are " + getComponentMeta().getPropetyKeys() + " this property will be ignored." + Arrays.toString(thread.getStackTrace()));
            } else {
                configuredProperties.put(key, new ComponentProperty(getComponentMeta().getMetadata(key), value));
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
    public Map<String, ComponentProperty> getConfiguredProperties() {
        return configuredProperties;
    }

    /**
     * Convenience getter to return User Implemented class properties
     * @return User Implemented class properties
     */
    @JsonIgnore
    public List<ComponentProperty> getUserSuppliedClassProperties() {
        return configuredProperties.values().stream()
            .filter(x -> x.getMeta().isUserSuppliedClass())
            .collect(Collectors.toList());
    }

    public boolean hasUserSuppliedClass() {
        return configuredProperties.values()
            .stream()
            .anyMatch(x -> x.getMeta().isUserSuppliedClass());
    }
//    public boolean hasBespokeClass() {
//        return configuredProperties.values()
//            .stream()
//            .anyMatch(x -> x.getMeta().isGeneratesBespokeClass());
//    }

    /**
     * ** Used in FTL **
     * Get all the standard properties i.e. exclude the special 'name and description' properties.
     * @return the Map of standard properties for this component.
     */
    @JsonIgnore
    public Map<String, ComponentProperty> getStandardConfiguredProperties() {
        Map<String, ComponentProperty> standardProperties = new HashedMap();
        if (configuredProperties != null && !configuredProperties.isEmpty()) {
            for (Map.Entry<String, ComponentProperty> entry : configuredProperties.entrySet()) {
                if (! ComponentPropertyMeta.NAME.equals(entry.getKey()) && !(ComponentPropertyMeta.DESCRIPTION.equals(entry.getKey()))) {
                    standardProperties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        return standardProperties;
    }

    public void addAllProperties(Map<String, ComponentProperty> newProperties) {
        configuredProperties.putAll(newProperties);
    }
    public void addComponentProperty(String key, ComponentProperty value) {
        configuredProperties.put(key, value);
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
                ", type=" + componentMeta +
                '}';
    }
}
