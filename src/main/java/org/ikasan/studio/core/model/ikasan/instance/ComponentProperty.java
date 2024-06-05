package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import liquibase.pro.packaged.J;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ikasan.studio.core.model.ModelUtils;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;

import java.util.Arrays;
import java.util.List;

import static org.ikasan.studio.core.StudioBuildUtils.SUBSTITUTION_PREFIX;

/**
 * Holds the value of a property
 */
@Getter
@Setter
@ToString
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
        String returnValue = "null";
        if (value != null) {
            if (value instanceof List) {
                returnValue = Arrays.toString(((List) value).toArray());
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
