package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.ikasan.studio.core.model.ModelUtils;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;

/**
 * Holds the value of a property
 */
@Data
public class ComponentProperty {
    private Object value;
    @JsonIgnore
    private ComponentPropertyMeta meta;

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
        return value == null ? "null" : value.toString();
    }

    @JsonIgnore
    public ComponentPropertyMeta getMeta() {
        return meta;
    }


    public boolean affectsUserImplementedClass() {
        return Boolean.TRUE.equals(getMeta().isAffectsUserImplementedClass());
    }

    /**
     * Used by template library, IDE may incorrectly identify as redundant
     * Get the value and present it in such a way as to be appropriate for display in the template language
     * @return a string that contains the value display in such a way as to be appropriate for inclusion in a template
     */
    @JsonIgnore
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
                (value instanceof Float && ((Float) value) == 0.0);
    }
}
