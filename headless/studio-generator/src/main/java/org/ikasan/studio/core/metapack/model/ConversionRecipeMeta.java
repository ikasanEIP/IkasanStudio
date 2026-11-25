package org.ikasan.studio.core.metapack.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.ikasan.studio.core.conversion.ConversionRecipeMatcher;

/** A stable meta-pack recipe; multiple construction strategies may share a source/target pair. */
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
    private String extractionTemplate;
    private String constructionTemplate;
    private List<String> configurationProperties;

    public boolean matches(String source, String target) {
        return sourceType != null && targetType != null
                && ConversionRecipeMatcher.javaType(sourceType).equals(ConversionRecipeMatcher.javaType(source))
                && ConversionRecipeMatcher.javaType(targetType).equals(ConversionRecipeMatcher.javaType(target));
    }
}
