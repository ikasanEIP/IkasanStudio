package org.ikasan.studio.core.metapack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/** A meta-pack supplied implementation recipe for one exact source-to-target conversion. */
@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class ConversionRecipeMeta {
    private String id;
    private String displayName;
    private String sourceType;
    private String targetType;
    private String template;
    private String helpText;

    public boolean matches(String source, String target) {
        return sourceType != null && targetType != null
                && sourceType.equals(source) && targetType.equals(target);
    }
}
