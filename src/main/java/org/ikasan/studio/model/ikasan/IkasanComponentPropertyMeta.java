package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.StudioUtils;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Represents all the possible properties an ikasan component is allowed to have
 */
public class IkasanComponentPropertyMeta {
    public static final String NAME = "Name";
    public static final String DESCRIPTION = "Description";
    public static final String BESPOKE_CLASS_NAME = "BespokeClassName";
    public static final String FROM_TYPE = "FromType";
    public static final String TO_TYPE = "ToType";
    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true, false, false,
            IkasanComponentPropertyMeta.NAME, null, String.class, "", "", "",
            "The name of the component as displayed on diagrams, space are encouraged, succinct is best. The name should be unique for the flow.");
    public static final IkasanComponentPropertyMeta STD_DESCIPTION_META_COMPONENT =
        new IkasanComponentPropertyMeta(false, false,false,
            IkasanComponentPropertyMeta.DESCRIPTION, null, String.class, "", "", "",
            "A more detailed description of the component that may assist in support.");

    public static final String APPLICATION_PACKAGE_NAME = "ApplicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";
    public static final IkasanComponentPropertyMeta STD_PACKAGE_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(true, false,false,
            IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME, null, String.class, "", "", "",
            "The base java package for your application.");

    public static final String APPLICATION_PORT_NUMBER_NAME = "ApplicationPortNumber";
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final IkasanComponentPropertyMeta STD_PORT_NUMBER_META_COMPONENT =
            new IkasanComponentPropertyMeta(true, false,false,
                    IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME, null, String.class, "", "", "",
                    "The port number that the running application will use locally.");

    boolean mandatory;
    boolean userImplementedClass;
    boolean userDefineResource;
    String propertyName;
    String propertyConfigFileLabel;
    Class propertyDataType;
    String usageDataType;
    String validation;
    Object defaultValue;
    String helpText;

    /**
     *
     * @param mandatory for the component to be deemed to be complete
     * @param userImplementedClass The user will define a beskpoke class that implements the interface, we will generate the spring property but leave implementation to client code.
     * @param userDefineResource The user will define a the details of the resource within the ResourceFactory.
     * @param propertyName of the property, used on input screens and to generate variable names
     * @param propertyConfigFileLabel Identifies the spring injected property name
     * @param propertyDataType of the property
     * @param validation validation string (used by the property editor to validate the format of the data)
     * @param defaultValue for the property
     * @param helpText for the property
     */
    public IkasanComponentPropertyMeta(@NotNull boolean mandatory,
                                       @NotNull boolean userImplementedClass,
                                       boolean userDefineResource,
                                       @NotNull String propertyName,
                                       String propertyConfigFileLabel,
                                       @NotNull Class propertyDataType,
                                       String usageDataType,
                                       String validation,
                                       Object defaultValue,
                                       String helpText) {
        this.mandatory = mandatory;
        this.userImplementedClass = userImplementedClass;
        this.userDefineResource = userDefineResource;
        this.propertyName = propertyName;
        this.propertyConfigFileLabel = propertyConfigFileLabel;
        this.propertyDataType = propertyDataType;
        if (usageDataType != null && usageDataType.length() > 0) {
            this.usageDataType = usageDataType;
        } else if (propertyDataType != null) {
            this.usageDataType = propertyDataType.getCanonicalName();
        }
        this.validation = validation;
        this.defaultValue = defaultValue;
        this.helpText = helpText;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean getMandatory() {
        return mandatory;
    }

    public boolean getUserImplementedClass() {
        return userImplementedClass;
    }

    public boolean isUserImplementedClass() {
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
    public Class getPropertyDataType() {
        return propertyDataType;
    }

    public String getUsageDataType() {
        return usageDataType;
    }

    public String getValidation() {
        return validation;
    }

    public String getHelpText() {
        return helpText;
    }

    public static IkasanComponentPropertyMeta getUnknownComponentMeta(final String name) {
        return new IkasanComponentPropertyMeta(false, false, false, name, null, String.class, "", "", "", "");
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
        return mandatory == that.mandatory &&
                userImplementedClass == that.userImplementedClass &&
                propertyName.equals(that.propertyName) &&
                propertyDataType.equals(that.propertyDataType);
    }

    /**
     * Note the hashcode method deliberately selects mandatory, propertyName, dataType ONLY
     */
    @Override
    public int hashCode() {
        return Objects.hash(mandatory, userImplementedClass, propertyName, propertyDataType);
    }

    @Override
    public String toString() {
        return "IkasanComponentPropertyMeta{" +
                "mandatory=" + mandatory +
                ", userImplementedClass=" + userImplementedClass +
                ", userDefineResource=" + userDefineResource +
                ", propertyName='" + propertyName + '\'' +
                ", propertyConfigFileLabel='" + propertyConfigFileLabel + '\'' +
                ", propertyDataType=" + propertyDataType +
                ", usageDataType=" + usageDataType +
                ", validation=" + validation +
                ", defaultValue=" + defaultValue +
                ", helpText='" + helpText + '\'' +
                '}';
    }
}
