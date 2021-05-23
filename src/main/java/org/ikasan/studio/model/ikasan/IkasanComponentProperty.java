package org.ikasan.studio.model.ikasan;

public class IkasanComponentProperty {
    private Object value;
    private Boolean regenerateAllowed = true;
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

    public String getValueString() {
        return value.toString();
    }

    public void setValue(Object value) {
        this.value = value;
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
    public boolean isEmpty() {
        boolean empty = false;
        if ((value == null) ||
            (value instanceof java.lang.String && ((String)value).isEmpty()) ||
            (value instanceof java.lang.Integer && ((Integer)value) ==0) ||
            (value instanceof java.lang.Long && ((Long)value) == 0l) ||
            (value instanceof java.lang.Double && ((Double)value) == 0.0) ||
            (value instanceof java.lang.Float && ((Float)value) == 0.0)) {
            empty = true;
        }
        return empty;
    }

}
