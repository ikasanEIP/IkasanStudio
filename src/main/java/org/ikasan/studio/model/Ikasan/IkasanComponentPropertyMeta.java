package org.ikasan.studio.model.Ikasan;

import com.sun.istack.NotNull;

import java.util.Objects;

/**
 * Represents all the possible properties an Ikasan component is allowed to have
 */
public class IkasanComponentPropertyMeta {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true,
            IkasanComponentPropertyMeta.NAME, null, String.class,
            "The name of the component as displayed on diagrams, also used for the variable name in the generated code.");
    public static final IkasanComponentPropertyMeta STD_DESCIPTION_META_COMPONENT =
        new IkasanComponentPropertyMeta(true,
            IkasanComponentPropertyMeta.DESCRIPTION, null, String.class,
            "A more detailed description of the component that may assist in support.");

    Boolean mandatory;
    String propertyName;
    String propertyConfigFileLabel;
    Class dataType;
    String helpText;

    public IkasanComponentPropertyMeta(@NotNull boolean mandatory, @NotNull String propertyName, String propertyConfigFileLabel, @NotNull Class dataType, String helpText) {
        this.mandatory = mandatory;
        this.propertyName = propertyName;
        this.propertyConfigFileLabel = propertyConfigFileLabel;
        this.dataType = dataType;
        this.helpText = helpText;
    }

    public IkasanComponentPropertyMeta(@NotNull String propertyName, String propertyConfigFileLabel, @NotNull Class dataType, String helpText) {
        this(false, propertyName, propertyConfigFileLabel, dataType, helpText);
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyConfigFileLabel() {
        return propertyConfigFileLabel;
    }
    public Class getDataType() {
        return dataType;
    }

    public String getHelpText() {
        return helpText;
    }

    public static IkasanComponentPropertyMeta getUnknownComponentMeta(final String name) {
        return new IkasanComponentPropertyMeta(false, name, null, String.class,"");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IkasanComponentPropertyMeta that = (IkasanComponentPropertyMeta) o;
        return mandatory.equals(that.mandatory) &&
                propertyName.equals(that.propertyName) &&
                dataType.equals(that.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mandatory, propertyName, dataType);
    }
}
