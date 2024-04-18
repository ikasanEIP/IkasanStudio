package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.serialization.BasicElementSerializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parent of all Ikasan Components e.g. flows, module, flowComponent
 * To make the instance model flexible and driven by Ikasan Meta Pack, most attributes are contained within the properties map.
 * This does
 */
@JsonSerialize(using = BasicElementSerializer.class)
@Getter
@Setter
public  class BasicElement extends IkasanObject {
    private static final Logger LOG = LoggerFactory.getLogger(BasicElement.class);
    private static final com.intellij.openapi.diagnostic.Logger LOGI = com.intellij.openapi.diagnostic.Logger.getInstance("#BasicElement");
//    @JsonPropertyOrder({"componentName", "description"})
    protected Map<String, ComponentProperty> componentProperties;
    public BasicElement() {}

    protected BasicElement(
            ComponentMeta componentMeta,
            String description) {
        super(componentMeta);
        this.componentProperties = componentMeta.getMandatoryInstanceProperties();
        setDescription(description);
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
        this.setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, name);
    }
    public void setName(String name) {
        this.setPropertyValue(ComponentPropertyMeta.NAME, name);
    }
    public void setVersion(String version) {
        this.setPropertyValue(ComponentPropertyMeta.VERSION, version);
    }
    public String getVersion() {
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
        if (description != null) {
            this.setPropertyValue(ComponentPropertyMeta.DESCRIPTION, description);
        }
    }

    /**
     * Return the name of this component in a format that would be appropriate to be used as a component in a package name
     * @return the package name format of the component name.
     */
    @JsonIgnore
    public String getJavaPackageName() {
        return StudioBuildUtils.toJavaPackageName(getName());
    }

    @JsonIgnore
    public String getJavaVariableName() {
        return StudioBuildUtils.toJavaIdentifier(getName());
    }


    @JsonIgnore
    public ComponentProperty getProperty(String key) {
        return componentProperties.get(key);
    }

    @JsonIgnore
    public Object getPropertyValue(String key) {
        ComponentProperty componentProperty = componentProperties.get(key);
        return componentProperty != null ? componentProperty.getValue() : null;
    }

    /**
     * Also used by ftl
     * @param key of the property
     * @return string value of the paroperty or empty string
     */
    @JsonIgnore
    public String getPropertyValueAsString(String key) {
        String returnValue = "";
        ComponentProperty componentProperty = componentProperties.get(key);
        if (componentProperty != null) {
            Object value = componentProperty.getValue();

            if (value instanceof List) {
                returnValue = Arrays.toString(((List)value).toArray());
            } else {
                returnValue = value.toString();
            }
        }
        return returnValue;
    }

    public List<ComponentProperty> getComponentPropertyList() {
        if (componentProperties != null && !componentProperties.isEmpty()) {
            return new ArrayList<>(componentProperties.values());
        } else {
            return Collections.EMPTY_LIST;
        }
    }


    /**
     * This setter should be used if we think the property might not already be set but will require the correct metadata
     * @param key of the property to be updated
     * @param value for the property
     */
    public void setPropertyValue(String key, Object value) {
        ComponentProperty componentProperty = componentProperties.get(key);
        if (componentProperty != null) {
            componentProperty.setValue(value);
        } else {
            ComponentPropertyMeta properyMeta = getComponentMeta().getMetadata(key);
            if (properyMeta == null) {
                Thread thread = Thread.currentThread();
                LOG.error("STUDIO: SERIOUS ERROR - Attempt to set property " + key + " on Element " + this.getName() + ", class " + this.getClass() + " with value [" + value + "], the known properties are " + getComponentMeta().getPropertyKeys() + " this property will be ignored." + Arrays.toString(thread.getStackTrace()));
            } else {
                componentProperties.put(key, new ComponentProperty(getComponentMeta().getMetadata(key), value));
            }
        }
    }

    /**
     * remove a property for the given key
     * @param key of the property to be updated
     */
    public void removeProperty(String key) {
        componentProperties.remove(key);
    }

    @JsonPropertyOrder(alphabetic = true)
    public Map<String, ComponentProperty> getComponentProperties() {
        return componentProperties;
    }

    /**
     * Convenience getter to return User Implemented class properties
     * @return User Implemented class properties
     */
    @JsonIgnore
    public List<ComponentProperty> getUserSuppliedClassProperties() {
        return componentProperties.values().stream()
            .filter(x -> x.getMeta().isUserSuppliedClass())
            .collect(Collectors.toList());
    }

    public boolean hasUserSuppliedClass() {
        return componentProperties.values()
            .stream()
            .anyMatch(x -> x.getMeta().isUserSuppliedClass());
    }

    /**
     * ** Also Used in FTL **
     * Get all the standard properties i.e. exclude the special 'name and description' properties.
     * @return the Map of standard properties for this component.
     */
    @JsonIgnore
    public Map<String, ComponentProperty> getStandardComponentProperties() {
        Map<String, ComponentProperty> standardProperties = new TreeMap<>();
        if (componentProperties != null && !componentProperties.isEmpty()) {
            for (Map.Entry<String, ComponentProperty> entry : componentProperties.entrySet()) {
                if (! ComponentPropertyMeta.NAME.equals(entry.getKey()) && !(ComponentPropertyMeta.DESCRIPTION.equals(entry.getKey()))) {
                    standardProperties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }

        return standardProperties;
    }

//    public void addAllProperties(Map<String, ComponentProperty> newProperties) {
//        componentProperties.putAll(newProperties);
//    }
    public void addComponentProperty(String key, ComponentProperty value) {
        componentProperties.put(key, value);
    }

    /**
     * Return the name of this component in a format that would be appropriate to be used as a java class name
     * @return the class name format of the component name.
     */
    @JsonIgnore
    public String getJavaClassName() {
        return StudioBuildUtils.toJavaClassName(getName());
    }

    /**
     * Determine if there are some mandatory properties that have not yet been set.
     * @return true if there are mandatory components that do not yet have a value.
     */
    public boolean hasUnsetMandatoryProperties() {
        return componentProperties.entrySet().stream()
            .anyMatch(x -> x.getValue().getMeta().isMandatory() && x.getValue().valueNotSet());
    }

    @Override
    public String toString() {
        return "IkasanComponent{" +
                "properties=" + componentProperties +
                ", type=" + componentMeta +
                '}';
    }

    public String toSimpleString() {
        StringBuilder builder = new StringBuilder();
        if (componentProperties != null && !componentProperties.isEmpty()) {
            for(ComponentProperty componentProperty : componentProperties.values()) {
                builder.append(componentProperty.getMeta().getPropertyName()).append("=").append(componentProperty.getValue()).append(", ");
            }
        }

        return "type=" + (componentMeta!=null ? componentMeta.getName() : null) +
                ",properties[" + builder + "]" ;
    }
}
