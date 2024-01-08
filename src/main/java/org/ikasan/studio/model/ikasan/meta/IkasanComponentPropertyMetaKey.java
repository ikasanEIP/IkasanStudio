package org.ikasan.studio.model.ikasan.meta;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class IkasanComponentPropertyMetaKey implements Comparable<IkasanComponentPropertyMetaKey> {
    private String propertyName;
    private Integer parameterGroupNumber;

    /**
     * Construct the Metadata key
     * @param propertyName which often maps to a setter method name
     */
    public IkasanComponentPropertyMetaKey(String propertyName) {
        this(propertyName, 1);
        this.propertyName = propertyName;
    }

    /**
     * Construct the Metadata key
     * @param propertyName which often maps to a setter method name
     * @param parameterGroupNumber supports scenario where we have more than 1 group of parameters e.g. when we are overloading
     */
    public IkasanComponentPropertyMetaKey(String propertyName, Integer parameterGroupNumber) {
        this.propertyName = propertyName;
        this.parameterGroupNumber = parameterGroupNumber;
    }

    public String getCompareKey() {
        return parameterGroupNumber + propertyName;
    }

    @Override
    public int compareTo(@NotNull IkasanComponentPropertyMetaKey o) {
        if (o == null) {
            return this.getCompareKey().compareTo("null");
        } else {
            return this.getCompareKey().compareTo(o.getCompareKey());
        }
    }
}
