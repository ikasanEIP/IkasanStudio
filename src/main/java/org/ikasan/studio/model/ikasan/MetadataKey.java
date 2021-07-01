package org.ikasan.studio.model.ikasan;

import org.jetbrains.annotations.NotNull;

public class MetadataKey implements Comparable<MetadataKey> {
    private String propertyName;
    private Integer parameterGroupNumber;
    private Integer parameterNumber;

    /**
     * Construct the Metadata key
     * @param propertyName which often maps to a setter method name
     */
    public MetadataKey(String propertyName) {
        this(propertyName, 1, 1);
        this.propertyName = propertyName;
    }

    /**
     * Construct the Metadata key
     * @param propertyName which often maps to a setter method name
     * @param parameterGroupNumber supports scenario where we have more than 1 group of parameters e.g. when we are overloading
     * @param parameterNumber used to construct the property, most properties have 1 parameter to set them.
     */
    public MetadataKey(String propertyName, Integer parameterGroupNumber, Integer parameterNumber) {
        this.propertyName = propertyName;
        this.parameterGroupNumber = parameterGroupNumber;
        this.parameterNumber = parameterNumber;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Integer getParameterGroupNumber() {
        return parameterGroupNumber;
    }

    public void setParameterGroupNumber(Integer parameterGroupNumber) {
        this.parameterGroupNumber = parameterGroupNumber;
    }

    public Integer getParameterNumber() {
        return parameterNumber;
    }

    public void setParameterNumber(Integer parameterNumber) {
        this.parameterNumber = parameterNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataKey that = (MetadataKey) o;

        if (!propertyName.equals(that.propertyName)) return false;
        if (!parameterGroupNumber.equals(that.parameterGroupNumber)) return false;
        return parameterNumber.equals(that.parameterNumber);
    }

    @Override
    public int hashCode() {
        int result = propertyName.hashCode();
        result = 31 * result + parameterGroupNumber.hashCode();
        result = 31 * result + parameterNumber.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetadataKey{" +
                "propertyName='" + propertyName + '\'' +
                ", parameterGroupNumber=" + parameterGroupNumber +
                ", parameterNumber=" + parameterNumber +
                '}';
    }

    public String getCompareKey() {
        return parameterGroupNumber + parameterNumber + propertyName;
    }

    @Override
    public int compareTo(@NotNull MetadataKey o) {
        if (o == null) {
            return this.getCompareKey().compareTo("null");
        } else {
            return this.getCompareKey().compareTo(o.getCompareKey());
        }
    }
}
