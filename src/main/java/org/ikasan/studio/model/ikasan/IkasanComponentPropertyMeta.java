package org.ikasan.studio.model.ikasan;

import com.sun.istack.NotNull;
import org.ikasan.studio.StudioUtils;

import java.util.Objects;

/**
 * Represents all the possible properties an ikasan component is allowed to have
 */
public class IkasanComponentPropertyMeta {
    public static final String NAME = "Name";
    public static final String DESCRIPTION = "Description";
    public static final String BESPOKE_CLASS_NAME = "BespokeClassName";
    public static final String APPLICATION_PACKAGE_NAME = "ApplicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";
    public static final String FROM_TYPE = "FromType";
    public static final String TO_TYPE = "ToType";
    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true, false,
            IkasanComponentPropertyMeta.NAME, null, String.class, "", "",
            "The name of the component as displayed on diagrams, space are encouraged, succinct is best. The name should be unique for the flow.");
    public static final IkasanComponentPropertyMeta STD_DESCIPTION_META_COMPONENT =
        new IkasanComponentPropertyMeta(false, false,
            IkasanComponentPropertyMeta.DESCRIPTION, null, String.class, "", "",
            "A more detailed description of the component that may assist in support.");
    public static final IkasanComponentPropertyMeta STD_PACKAGE_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true, false,
            IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME, null, String.class, "", "",
            "The back java package for your application.");

    Boolean mandatory;
    Boolean userImplementedClass;
    String propertyName;
    String propertyConfigFileLabel;
    Class dataType;
    String validation;
    Object defaultValue;
    String helpText;

    /**
     *
     * @param mandatory for the component to be deemed to be complete
     * @param userImplementedClass The user will define a beskpoke class that implements the interface, we will generate the spring property but leave implementation to client code.
     * @param propertyName of the property, used on input screens and to generate variable names
     * @param propertyConfigFileLabel Identifies the spring injected property name
     * @param dataType of the property
     * @param validation validation string (used by the property editor to validate the format of the data)
     * @param defaultValue for the property
     * @param helpText for the property
     */
    public IkasanComponentPropertyMeta(@NotNull boolean mandatory, @NotNull boolean userImplementedClass, @NotNull String propertyName, String propertyConfigFileLabel, @NotNull Class dataType, String validation, Object defaultValue, String helpText) {
        this.mandatory = mandatory;
        this.userImplementedClass = userImplementedClass;
        this.propertyName = propertyName;
        this.propertyConfigFileLabel = propertyConfigFileLabel;
        this.dataType = dataType;
        this.helpText = helpText;
        this.validation = validation;
        this.defaultValue = defaultValue;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public Boolean getUserImplementedClass() {
        return userImplementedClass;
    }

    public Boolean isUserImplementedClass() {
        return userImplementedClass;
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
        return new IkasanComponentPropertyMeta(false, false, name, null, String.class,"", "","");
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
                userImplementedClass.equals(that.userImplementedClass) &&
                propertyName.equals(that.propertyName) &&
                dataType.equals(that.dataType);
    }

    /**
     * Note the hashcode method deliberately selects mandatory, propertyName, dataType ONLY
     */
    @Override
    public int hashCode() {
        return Objects.hash(mandatory, userImplementedClass, propertyName, dataType);
    }

    @Override
    public String toString() {
        return "IkasanComponentPropertyMeta{" +
                "mandatory=" + mandatory +
                ", userImplementedClass=" + userImplementedClass +
                ", propertyName='" + propertyName + '\'' +
                ", propertyConfigFileLabel='" + propertyConfigFileLabel + '\'' +
                ", dataType=" + dataType +
                ", validation=" + validation +
                ", defaultValue=" + defaultValue +
                ", helpText='" + helpText + '\'' +
                '}';
    }
}
