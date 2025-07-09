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

@Data
@Builder
@Jacksonized
@AllArgsConstructor
//@JsonDeserialize(builder = ComponentTypeMeta.ComponentTypeMetaBuilder.class)
public class ComponentTypeMeta {
    @lombok.NonNull
    Integer displayOrder;
    String componentType;           // Component type might come from component if the type is not common for all component in this category e.g. routers.
    @lombok.NonNull
    String componentShortType;
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private Set<Dependency> jarDependencies;
    @Builder.Default
    private String helpText = "";
    @Builder.Default
    private Map<String, ComponentPropertyMeta> allowableProperties = new HashMap<>();

    public String getHelpText() {
        if (helpText == null) {
            helpText="";
        }
        return helpText;
    }
}
