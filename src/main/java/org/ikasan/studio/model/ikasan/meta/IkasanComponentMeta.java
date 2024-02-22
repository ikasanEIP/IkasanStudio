package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Data
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class IkasanComponentMeta implements IkasanMeta {
    private static final String DEFAULT_README = "Readme.md";
    // Its assumed these types are so fundamental they will not change, if they do they need to be instantiated from the Ikasan Version Pack
    public static final String CONSUMER_TYPE = "org.ikasan.spec.component.endpoint.Consumer";
    public static final String PRODUCER_TYPE = "org.ikasan.spec.component.endpoint.Producer";
    public static final String EXCEPTION_RESOLVER_TYPE = "org.ikasan.exceptionResolver.ExceptionResolver";
    public static final String FLOW_TYPE = "org.ikasan.spec.flow.Flow";
    public static final String MODULE_TYPE = "org.ikasan.spec.module.Module";

    public static final String NAME = "name";
    public static final String HELP_TEXT = "helpText";
    public static final String COMPONENT_TYPE = "componentType";
    public static final String IMPLEMENTING_CLASS = "implementingClass";

    // Many of these attributes are used by FreeMarker templates so be careful if refactoring.
    String name;
    String helpText;
    String componentType;
    String implementingClass;
    String ikasanComponentFactoryMethod;
    List<Dependency> jarDepedencies;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    String webHelpURL = DEFAULT_README;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    boolean bespokeClass;
    int displayOrder;

    @JsonIgnore
    ImageIcon smallIcon;
    @JsonIgnore
    ImageIcon canvasIcon;

    Map<String, IkasanComponentPropertyMeta> properties;

    public IkasanComponentMeta() {}

    /**
     * Get a list of the mandatory properties for this component.
     * @return An ordered map of the mandatory properties for this component
     */
    public Map<String, IkasanComponentProperty> getMandatoryInstanceProperties() {
        Map<String, IkasanComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : properties.entrySet()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public Set<String> getPropetyKeys() {
        return properties.keySet();
    }

    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
        return properties.get(propertyName);
    }
    public boolean isConsumer() {
        return CONSUMER_TYPE.equals(componentType);
    }
    public boolean isProducer() {
        return PRODUCER_TYPE.equals(componentType);
    }
    public boolean isFlow() {
        return FLOW_TYPE.equals(componentType);
    }
    public boolean isModule() {
        return MODULE_TYPE.equals(componentType);
    }
    public boolean isExceptionResolver() {
        return EXCEPTION_RESOLVER_TYPE.equals(componentType);
    }

    public String getDisplayComponentType() {
        if (componentType != null && componentType.contains(".")) {
            return componentType.substring(componentType.lastIndexOf('.') + 1).trim();
        } else {
            return "";
        }
    }
}
