package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentPropertyInstance;

import javax.swing.*;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class IkasanComponentMeta {
    private static final String DEFAULT_README = "Readme.md";
    private static final String CONSUMER_TYPE = "org.ikasan.spec.component.endpoint.Consumer";
    private static final String PRODUCER_TYPE = "org.ikasan.spec.component.endpoint.Producer";

    String name;
    String helpText;
    String componentType;
    String implementingClass;
    String ikasanComponentFactoryMethod;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    String webHelpURL = DEFAULT_README;
    String smallPalletteIcon;
    String mediumPalletteIcon;
    String largePalletteIcon;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    boolean bespokeClass;
    int displayOrder;

    ImageIcon smallIcon;
    ImageIcon canvasIcon;

    Map<String, IkasanComponentPropertyMeta> properties;
    //Map<String, IkasanComponentPropertyMeta> componentProperties = new LinkedHashMap<>();

    /**
     * Get a list of the mandatory properties for this component.
     * @return A map of the mandatory properties for this component
     */
    public Map<String, IkasanComponentPropertyInstance> getMandatoryInstanceProperties() {
        Map<String, IkasanComponentPropertyInstance> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : properties.entrySet()) {
//            if (!entry.getValue().subProperties && entry.getValue().isMandatory()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentPropertyInstance(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
        return properties.get(propertyName);
    }

//    "componentType": "org.ikasan.spec.component.endpoint.Producer",

    public boolean isConsumer() {
        return CONSUMER_TYPE.equals(componentType);
    }
    public boolean isProducer() {
        return PRODUCER_TYPE.equals(componentType);
    }
}
