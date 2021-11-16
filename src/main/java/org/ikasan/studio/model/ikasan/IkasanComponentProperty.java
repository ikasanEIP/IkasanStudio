package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.log4j.Logger;
import org.ikasan.studio.model.StudioPsiUtils;

import java.util.ArrayList;
import java.util.List;

public class IkasanComponentProperty {
    private static final Logger LOG = Logger.getLogger(IkasanComponentProperty.class);
    private Object value;
    private Boolean regenerateAllowed = true;
    @JsonIgnore
    private IkasanComponentPropertyMeta meta;

    public IkasanComponentProperty(IkasanComponentPropertyMeta meta, Object value) {
        this.meta = meta;
        if (value == null && meta.defaultValue != null) {
            this.value = meta.defaultValue;
        } else {
            this.value = value;
        }
    }

    public IkasanComponentProperty(IkasanComponentPropertyMeta meta) {
        this(meta, null);
    }

    public Object getValue() {
        return value;
    }

    /**
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
                displayValue = StudioPsiUtils.stripStartAndEndQuotes((String)value);
                displayValue = "\"" + displayValue + "\"";
            } else {
                displayValue = value.toString();
            }
        }
        return displayValue;
    }

    @JsonIgnore
    public String getValueString() {
        return value == null ? "null" : value.toString();
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Using the supplied string and meta data, attempt to set the value to an object of the appropriate class in accordance
     * with the metaData
     * @param newValue
     */
    public void setValueFromString(String newValue) {
        if (meta != null) {
            if (meta.getPropertyDataType() == String.class) {
                value = StudioPsiUtils.stripStartAndEndQuotes(newValue);
            } else {
                try {
                    if (meta.getPropertyDataType() == Long.class) {
                        value = Long.parseLong(newValue);
                    } else if (meta.getPropertyDataType() == Integer.class) {
                        value = Integer.parseInt(newValue);
                    } else if (meta.getPropertyDataType() == Double.class) {
                        value = Double.parseDouble(newValue);
                    } else if (meta.getPropertyDataType() == Float.class) {
                        value = Float.parseFloat(newValue);
                    }
                } catch (NumberFormatException nfe) {
                    LOG.warn("Failed to set value of type " + meta.getPropertyDataType() + " to value " + newValue);
                }
            }
        }
    }

    public IkasanComponentPropertyMeta getMeta() {
        return meta;
    }

    public void setMeta(IkasanComponentPropertyMeta meta) {
        this.meta = meta;
    }

    public Boolean isRegenerateAllowed() {
        return regenerateAllowed;
    }

    public void setRegenerateAllowed(Boolean regenerateAllowed) {
        this.regenerateAllowed = regenerateAllowed;
    }

    public boolean isUserImplementedClass() {
        return Boolean.TRUE.equals(getMeta().isUserImplementedClass());
    }

    public boolean causesUserCodeRegeneration() {
        return Boolean.TRUE.equals(getMeta().causesUserCodeRegeneration());
    }

    public static List<IkasanComponentProperty> generateIkasanComponentPropertyList(List<IkasanComponentPropertyMeta> metaList) {
        List<IkasanComponentProperty> propertyList = new ArrayList<>();
        if (metaList != null && !metaList.isEmpty()) {
            for(IkasanComponentPropertyMeta meta : metaList) {
                propertyList.add(new IkasanComponentProperty(meta));
            }
        }
        return propertyList;
    }

    @Override
    public String toString() {
        return "IkasanComponentProperty{" +
                "value=" + value +
                ", meta=" + meta +
                '}';
    }

    /**
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    @JsonIgnore
    public boolean valueNotSet() {
        boolean empty = false;
        if ((value == null) ||
            (value instanceof java.lang.String && ((String)value).isEmpty()) ||
            (value instanceof java.lang.Integer && ((Integer)value) ==0) ||
            (value instanceof Long && ((Long)value) == 0l) ||
            (value instanceof java.lang.Double && ((Double)value) == 0.0) ||
            (value instanceof java.lang.Float && ((Float)value) == 0.0)) {
            empty = true;
        }
        return empty;
    }

}
