package org.ikasan.studio.model.Ikasan;

import com.sun.istack.NotNull;
import org.ikasan.studio.StudioUtils;

import java.util.Objects;

/**
 * Represents all the possible properties an Ikasan component is allowed to have
 */
public class IkasanComponentPropertyMeta {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String BESPOKE_CLASS_NAME = "bespokeClassName";
    public static final String FROM_TYPE = "fromType";
    public static final String TO_TYPE = "toType";
    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true,
            IkasanComponentPropertyMeta.NAME, null, String.class, "",
            "The name of the component as displayed on diagrams, also used for the variable name in the generated code.");
    public static final IkasanComponentPropertyMeta STD_DESCIPTION_META_COMPONENT =
        new IkasanComponentPropertyMeta(true,
            IkasanComponentPropertyMeta.DESCRIPTION, null, String.class, "",
            "A more detailed description of the component that may assist in support.");

    Boolean mandatory;
    String propertyName;
    String propertyConfigFileLabel;
    Class dataType;
    Object defaultValue;
    String helpText;

    public IkasanComponentPropertyMeta(@NotNull boolean mandatory, @NotNull String propertyName, String propertyConfigFileLabel, @NotNull Class dataType, Object defaultValue, String helpText) {
        this.mandatory = mandatory;
        this.propertyName = propertyName;
        this.propertyConfigFileLabel = propertyConfigFileLabel;
        this.dataType = dataType;
        this.helpText = helpText;
        this.defaultValue = defaultValue;
    }

    public IkasanComponentPropertyMeta(@NotNull String propertyName, String propertyConfigFileLabel, @NotNull Class dataType, Object defaultValue, String helpText) {
        this(false, propertyName, propertyConfigFileLabel, dataType, defaultValue, helpText);
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyConfigFileLabel() {
        return propertyConfigFileLabel;
    }
    public String getPropertyConfigFileLabelAsVariable() {
        return StudioUtils.toJavaIdentifier(propertyConfigFileLabel);
    }
    public Class getDataType() {
        return dataType;
    }

    public String getHelpText() {
        return helpText;
    }

    public static IkasanComponentPropertyMeta getUnknownComponentMeta(final String name) {
        return new IkasanComponentPropertyMeta(false, name, null, String.class,"","");
    }

    /**
     * Class equals, note that the method deliberately selects mandatory, propertyName, dataType ONLY
     * @param o object to compare with.
     * @return the Java equals contract.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IkasanComponentPropertyMeta that = (IkasanComponentPropertyMeta) o;
        return mandatory.equals(that.mandatory) &&
                propertyName.equals(that.propertyName) &&
                dataType.equals(that.dataType);
    }

    /**
     * Note the hashcode method deliberately selects mandatory, propertyName, dataType ONLY
     */
    @Override
    public int hashCode() {
        return Objects.hash(mandatory, propertyName, dataType);
    }

    @Override
    public String toString() {
        return "IkasanComponentPropertyMeta{" +
                "mandatory=" + mandatory +
                ", propertyName='" + propertyName + '\'' +
                ", propertyConfigFileLabel='" + propertyConfigFileLabel + '\'' +
                ", dataType=" + dataType +
                ", defaultValue=" + defaultValue +
                ", helpText='" + helpText + '\'' +
                '}';
    }
}
