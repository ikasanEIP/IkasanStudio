package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentPropertyInstance;

import javax.swing.*;
import java.util.Map;
import java.util.TreeMap;

@Data
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class IkasanComponentMeta implements IkasanMeta {
    private static final String DEFAULT_README = "Readme.md";
    // Its assumed these types are so fundamental they will not change, if they do they need to be instantiated from the Ikasan Version Pack
    private static final String CONSUMER_TYPE = "org.ikasan.spec.component.endpoint.Consumer";
    private static final String PRODUCER_TYPE = "org.ikasan.spec.component.endpoint.Producer";
    private static final String EXCEPTION_RESOLVER_TYPE = "org.ikasan.exceptionResolver.ExceptionResolver";
    private static final String FLOW_TYPE = "org.ikasan.spec.flow.Flow";
    private static final String MODULE_TYPE = "org.ikasan.spec.module.Module";

    String name;
    String helpText;
    String componentType;
    String implementingClass;
    String ikasanComponentFactoryMethod;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    String webHelpURL = DEFAULT_README;
    String smallPaletteIcon;
    String mediumPaletteIcon;
    String largePaletteIcon;
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
     * @return A map of the mandatory properties for this component
     */
    public Map<String, IkasanComponentPropertyInstance> getMandatoryInstanceProperties() {
        Map<String, IkasanComponentPropertyInstance> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : properties.entrySet()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentPropertyInstance(entry.getValue()));
            }
        }
        return mandatoryProperties;
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
