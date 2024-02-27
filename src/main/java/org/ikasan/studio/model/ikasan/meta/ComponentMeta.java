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
import org.ikasan.studio.model.ikasan.instance.ComponentProperty;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import static org.ikasan.studio.model.ikasan.meta.ComponentType.*
        ;

@Data
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class ComponentMeta implements IkasanMeta {
    private static final String DEFAULT_README = "Readme.md";
//    public static final String EXCEPTION_RESOLVER_NAME = "Exception Resolver";
    public static final String FLOW_NAME = "Flow";

    public static final String NAME = "name";
    public static final String HELP_TEXT = "helpText";
    public static final String COMPONENT_TYPE = "componentType";
    public static final String IMPLEMENTING_CLASS = "implementingClass";

    // DO NOT RENAME - Will affect model.json
    String name;
    String helpText;
    String componentType;
    String implementingClass;
    String ikasanComponentFactoryMethod;
    boolean usesBuilder;
    String flowBuilderMethod;
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

    Map<String, ComponentPropertyMeta> properties;

    public ComponentMeta() {}

    /**
     * Get a list of the mandatory properties for this component.
     * @return An ordered map of the mandatory properties for this component
     */
    public Map<String, ComponentProperty> getMandatoryInstanceProperties() {
        Map<String, ComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, ComponentPropertyMeta> entry : properties.entrySet()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new ComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public Set<String> getPropetyKeys() {
        return properties.keySet();
    }

    public ComponentPropertyMeta getMetadata(String propertyName) {
        return properties.get(propertyName);
    }
    public boolean isConsumer() {
        return Consumer.classType.equals(componentType);
    }
    public boolean isProducer() {
        return Producer.classType.equals(componentType);
    }
    public boolean isFlow() {
        return Flow.classType.equals(componentType);
    }
    public boolean isModule() {
        return Module.classType.equals(componentType);
    }
    public boolean isExceptionResolver() {
        return ExceptionResolver.classType.equals(componentType);
    }

    public String getDisplayComponentType() {
        if (componentType != null && componentType.contains(".")) {
            return componentType.substring(componentType.lastIndexOf('.') + 1).trim();
        } else {
            return "";
        }
    }
}
