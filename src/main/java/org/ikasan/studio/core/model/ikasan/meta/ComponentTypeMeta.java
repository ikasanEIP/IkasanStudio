package org.ikasan.studio.core.model.ikasan.meta;


import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.apache.maven.model.Dependency;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Each meta-pack holds a complete set of information for all the components for a given version of Ikasan e.g.
 * - Properties,
 * - screen icon,
 * - template generation file,
 * - validation etc)
 */
@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class ComponentTypeMeta {
    @lombok.NonNull
    Integer displayOrder;                       // In the Palette, the order in which the components are displayed.
    String componentType;                       // e.g. org.ikasan.spec.component.endpoint.Producer
    @lombok.NonNull
    String componentShortType;                  // e.g. Producer
    @JsonSetter(nulls = Nulls.SKIP)             // If the supplied value is null, ignore it.
    private Set<Dependency> jarDependencies;    // The jars required to use this component type, e.g. org.ikasan:ikasan-email-producer:1.0.0
    @Builder.Default
    private String helpText = "";               // The help text for the component, used in the palette and in the component properties dialog
    @Builder.Default                            // The list of properties that this component is allowed to have, it will be merged with the list of properties from ComponentMeta which is more specialised
    private Map<String, ComponentPropertyMeta> allowableProperties = new HashMap<>();

    public String getHelpText() {
        if (helpText == null) {
            helpText="";
        }
        return helpText;
    }
}
