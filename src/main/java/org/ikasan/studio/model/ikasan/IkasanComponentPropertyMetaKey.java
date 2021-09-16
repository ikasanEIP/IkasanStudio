package org.ikasan.studio.model.ikasan;

import org.jetbrains.annotations.NotNull;

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IkasanComponentPropertyMetaKey that = (IkasanComponentPropertyMetaKey) o;

        if (!propertyName.equals(that.propertyName)) return false;
        return parameterGroupNumber.equals(that.parameterGroupNumber);
    }

    @Override
    public int hashCode() {
        int result = propertyName.hashCode();
        result = 31 * result + parameterGroupNumber.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetadataKey{" +
                "propertyName='" + propertyName + '\'' +
                ", parameterGroupNumber=" + parameterGroupNumber +
                '}';
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
