package org.ikasan.studio.model.ikasan;

public class IkasanComponentProperty {
    private Object value;
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

    @Override
    public String toString() {
        return "IkasanComponentProperty{" +
                "value=" + value  +'}';
    }
}