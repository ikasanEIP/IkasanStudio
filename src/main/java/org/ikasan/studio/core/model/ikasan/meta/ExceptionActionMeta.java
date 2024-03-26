package org.ikasan.studio.core.model.ikasan.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class ExceptionActionMeta {
    String actionName;
    Map<String, ComponentPropertyMeta> actionProperties;

    public ComponentPropertyMeta getMetaProperty(String key) {
        ComponentPropertyMeta componentPropertyMeta = null;
        if (key != null) {
            componentPropertyMeta = actionProperties.get(key);
        }
        return componentPropertyMeta;
    }
}
