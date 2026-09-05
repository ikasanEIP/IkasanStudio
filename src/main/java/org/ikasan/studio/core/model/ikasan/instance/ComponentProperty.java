package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
//import liquibase.pro.packaged.J;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ikasan.studio.core.model.ModelUtils;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;

import java.util.List;

/**
 * Holds the value of a property
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ComponentProperty {

    private Object value;
    @JsonIgnore
    private ComponentPropertyMeta meta;
    // Transient, session-scoped only (never persisted to model.json), mirrors FlowUserImplementedElement.overwriteEnabled
    // but at per-property granularity: gates whether a "protectFromOverwrite" userSuppliedClass stub is (re)generated.
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private boolean overwriteEnabled = false;

    public ComponentProperty(ComponentPropertyMeta meta, Object value) {
        this.meta = meta;
        this.value = value;
    }

    public ComponentProperty(ComponentPropertyMeta meta) {
        this(meta, null);
    }

    @JsonIgnore
    public Object getDefaultValue() {
        return meta.getDefaultValue();
    }

    @JsonIgnore
    public String getValueString() {
        String returnValue = "null";
        if (value != null) {
            if (value instanceof List) {
                // Plain comma-joined, no brackets/spaces - this feeds straight into a Spring Boot properties
                // value (see propertiesTemplate_en.ftl), which splits a List<String> property on commas with no
                // awareness of Java's Arrays.toString()/List.toString() bracket-and-space convention. Using that
                // convention here previously wrote e.g. "[myFile\.txt, anotherFile\.txt]" into
                // application.properties, which Spring's comma-splitting then rebound as two broken entries
                // ("[myFile\.txt" and " anotherFile\.txt]") - the first of which isn't even a valid regex,
                // crashing FileMatcher's PatternSyntaxException at startup.
                returnValue = String.join(",", ((List<?>) value).stream().map(String::valueOf).toList());
            } else
                returnValue = value.toString();
        }
        return returnValue;
    }

    @JsonIgnore
    public ComponentPropertyMeta getMeta() {
        return meta;
    }


    public boolean affectsUserImplementedClass() {
        return getMeta().isAffectsUserImplementedClass();
    }

    /**
     * Called only from flowTemplate_en.ftl (both V3.3.9 and V4.1.6) via FreeMarker's bean-property syntax
     * (${param.templateRepresentationOfValue}), which static-usage analysis can't see - despite the "unused"
     * warning, this is load-bearing there.
     * Get the value and present it in such a way as to be appropriate for display in the template language
     * @return a string that contains the value display in such a way as to be appropriate for inclusion in a template
     */
    @JsonIgnore
    @SuppressWarnings("unused")
    public String getTemplateRepresentationOfValue() {
        String displayValue = "";
        if (value == null) {
            displayValue = null;
        } else if (meta != null) {
            if ("java.lang.String".equals(meta.getUsageDataType())) {
                displayValue = ModelUtils.stripStartAndEndQuotes((String)value);
                displayValue = "\"" + displayValue + "\"";
            } else {
                displayValue = value.toString();
            }
        }
        return displayValue;
    }

    /**
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    @JsonIgnore
    public boolean valueNotSet() {
        return (value == null) ||
                (value instanceof String && ((String) value).isEmpty()) ||
                (value instanceof Integer && ((Integer) value) == 0) ||
                (value instanceof Long && ((Long) value) == 0) ||
                (value instanceof Double && ((Double) value) == 0.0) ||
                (value instanceof Float && ((Float) value) == 0.0) ||
                (value instanceof List && ((List<?>) value).isEmpty()) ||
                (value instanceof String && isStaleEmptyListLiteral((String) value));
    }

    // model.json saved by a now-fixed older bug could hold the literal 2-character string "[]" for a List
    // property (java.util.List#toString() of an empty list, written out as a raw value rather than treated as
    // unset) - a genuinely unset field is expected here instead, so treat that stale literal the same way: as
    // not set. Left unguarded, it survives JSON round-tripping as a non-null, non-blank String and slips past
    // every other check here, then reaches Spring's @Value SpEL list-split in componentFactory_en.ftl as a
    // single-element list containing the text "[]" - which is not a valid email address.
    private boolean isStaleEmptyListLiteral(String stringValue) {
        return meta != null && meta.getUsageDataType() != null
                && meta.getUsageDataType().startsWith("java.util.List")
                && "[]".equals(stringValue.trim());
    }
}
