package org.ikasan.studio.model.Ikasan;

public class IkasanComponentProperty {
    Object value;
    IkasanComponentPropertyMeta meta;

    public IkasanComponentProperty(IkasanComponentPropertyMeta meta, Object value) {
        this.meta = meta;
        this.value = value;
    }

    public IkasanComponentProperty(IkasanComponentPropertyMeta meta) {
        this(meta, null);
    }

    public Object getValue() {
        return value;
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
