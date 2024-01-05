package org.ikasan.studio.model.ikasan.meta;

import org.ikasan.studio.StudioUtils;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Represents all the possible properties an Ikasan component is allowed to have
 */
public class IkasanComponentPropertyMeta {
    public static final String BESPOKE_CLASS_NAME = "BespokeClassName";         // Special meta for a bespoke class used as a property
    public static final String CONFIGURATION = "Configuration";                 // Bean containing user defined, Ikasan maintained properties
    public static final String CONFIGURED_RESOURCE_INTERFACE = "ConfiguredResource";   // Is the configuration exposed in the dashboard.
    public static final String IS_CONFIGURED_RESOURCE = "IsConfiguredResource";   // Is the configuration exposed in the dashboard.
    public static final String FROM_TYPE = "FromType";                          // Special meta for converter, the type of the inbound payload
    public static final String TO_TYPE = "ToType";                              // Special meta for converter, the type of the outbound payload

    // Special META for component NAME, this standard for each component.
    public static final IkasanComponentPropertyMetaKey NAME = new IkasanComponentPropertyMetaKey("Name");
    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(1, false, true, false, false, true,
            NAME.getPropertyName(), null, String.class, "", "", "", "",
            "The name of the component as displayed on diagrams, space are encouraged, succinct is best. The name should be unique for the flow.");

    // Special META for component DESCRIPTION, this standard for each component.
    public static final IkasanComponentPropertyMetaKey DESCRIPTION = new IkasanComponentPropertyMetaKey("Description");
    public static final IkasanComponentPropertyMeta STD_DESCRIPTION_META_COMPONENT =
        new IkasanComponentPropertyMeta(1, false, false, false, false, true,
            DESCRIPTION.getPropertyName(), null, String.class, "", "", "", "",
            "A more detailed description of the component that may assist in support.");

    // Special META for package parent of the users bespoke packages, a little like a pom group
    public static final IkasanComponentPropertyMetaKey APPLICATION_PACKAGE_NAME = new IkasanComponentPropertyMetaKey("ApplicationPackageName");
    public static final String APPLICATION_PACKAGE_KEY = "module.package";
    public static final IkasanComponentPropertyMeta STD_PACKAGE_NAME_META_COMPONENT =
        new IkasanComponentPropertyMeta(1, false, true, false, false, false,
            APPLICATION_PACKAGE_NAME.getPropertyName(), null, String.class, "", "", "", "",
            "The base java package for your application.");

    // Special META to model the port number to be used to launch the app and part of its user driven config.
    public static final IkasanComponentPropertyMetaKey APPLICATION_PORT_NUMBER_NAME = new IkasanComponentPropertyMetaKey("ApplicationPortNumber");
    public static final IkasanComponentPropertyMetaKey H2_DB_PORT_NUMBER_NAME = new IkasanComponentPropertyMetaKey("H2DbPortNumber");
    public static final IkasanComponentPropertyMetaKey H2_WEB_PORT_NUMBER_NAME = new IkasanComponentPropertyMetaKey("H2WebPortNumber");
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final String H2_PORT_NUMBER_KEY = "h2.db.port";
    public static final IkasanComponentPropertyMeta STD_PORT_NUMBER_META_COMPONENT =
            new IkasanComponentPropertyMeta(1, false, true, false, false, false,
                    APPLICATION_PORT_NUMBER_NAME.getPropertyName(), null, String.class, "", "", "", "",
                    "The port number that the running application will use locally.");

    Integer paramGroupNumber;
    boolean mandatory;
    boolean causesUserCodeRegeneration;
    boolean userImplementedClass;
    boolean setterProperty;
    boolean userDefineResource;
    String propertyName;
    String propertyConfigFileLabel;
    Class propertyDataType;
    String usageDataType;
    String validation;
    String validationMessage;
    Pattern validationPattern;
    Object defaultValue;
    String helpText;

    Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> metadataMap;
    boolean subProperties = false;

    /**
     * A IkasanComponentPropertyMeta could be a leaf i.e. hold a single set of metadata or could nest another set of metadata
     *
     * @param key the nested key for this property
     * @param value the nested value for this property
     */
    public IkasanComponentPropertyMeta(@NotNull IkasanComponentPropertyMetaKey key,
                                       @NotNull IkasanComponentPropertyMeta value) {
        this.metadataMap = new TreeMap<>();
        metadataMap.put(key, value);
        this.subProperties = true;
    }

   /**
    * A IkasanComponentPropertyMeta could be a leaf i.e. hold a single set of metadata or could nest another set of metadata
    *
    * @param paramGroupNumber Supports sets of params e.g. overloaded methods
    * @param causesUserCodeRegeneration Causes the user code to be regenerated
    * @param mandatory for the component to be deemed to be complete
    * @param userImplementedClass The user will define a beskpoke class that implements the interface, we will generate the spring property but leave implementation to client code.
    * @param setterProperty The property should feature in the component factory setter.
    * @param userDefineResource The user will define the details of the resource within the ResourceFactory.
    * @param propertyName of the property, used on input screens and to generate variable names
    * @param propertyConfigFileLabel Identifies the spring injected property name
    * @param propertyDataType of the property
    * @param validation validation string (used by the property editor to validate the format of the data)
    * @param validationMessage to display if the field failr validation
    * @param defaultValue for the property
    * @param helpText for the property
    */
   /* @TODO convert to builder */
    public IkasanComponentPropertyMeta(@NotNull Integer paramGroupNumber,
                                       @NotNull boolean causesUserCodeRegeneration,
                                       @NotNull boolean mandatory,
                                       @NotNull boolean userImplementedClass,
                                       boolean setterProperty,
                                       boolean userDefineResource,
                                       @NotNull String propertyName,
                                       String propertyConfigFileLabel,
                                       @NotNull Class propertyDataType,
                                       String usageDataType,
                                       String validation,
                                       String validationMessage,
                                       Object defaultValue,
                                       String helpText) {
        this.paramGroupNumber = paramGroupNumber;
        this.causesUserCodeRegeneration = causesUserCodeRegeneration;
        this.mandatory = mandatory;
        this.userImplementedClass = userImplementedClass;
        this.setterProperty = setterProperty;
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
        if (validation != null && !validation.isEmpty()) {
            this.validationPattern = Pattern.compile(validation);
            this.validationMessage = validationMessage;
        }
        this.defaultValue = defaultValue;
        this.helpText = helpText;
    }

    public boolean hasSubProperties() { return subProperties; }

    /**
     * For the meta that represents certain types of properties e.g. Exception Resolutions actions, some of the properties
     * represent marker properties i.e. on/off e.g. stop()
     * @return
     */
    public boolean isVoid() {
        return propertyDataType == null;
    }

    public void addSubProperty(@NotNull IkasanComponentPropertyMetaKey key,
                               @NotNull IkasanComponentPropertyMeta value) {
        if (metadataMap == null) {
            this.metadataMap = new TreeMap<>();
        }
        metadataMap.put(key, value);
        this.subProperties = true;
    }

    public Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> getSubProperties() {
        return this.metadataMap;
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

    public boolean causesUserCodeRegeneration() {
        return causesUserCodeRegeneration;
    }

    public void setCausesUserCodeRegeneration(boolean causesUserCodeRegeneration) {
        this.causesUserCodeRegeneration = causesUserCodeRegeneration;
    }

    public boolean isSetterProperty() {
        return setterProperty;
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

    public Pattern getValidationPattern() {
        return validationPattern;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public String getHelpText() {
        return helpText;
    }

    public static IkasanComponentPropertyMeta getUnknownComponentMeta(final String name) {
        return new IkasanComponentPropertyMeta(1, false, false, false, false, false, name, null, String.class, "", "", "", "", "");
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
        return paramGroupNumber.equals(that.paramGroupNumber) &&
                mandatory == that.mandatory &&
                userImplementedClass == that.userImplementedClass &&
                propertyName.equals(that.propertyName) &&
                propertyDataType.equals(that.propertyDataType);
    }

    /**
     * Note the hashcode method deliberately selects mandatory, propertyName, dataType ONLY
     */
    @Override
    public int hashCode() {
        return Objects.hash(paramGroupNumber, mandatory, userImplementedClass, propertyName, propertyDataType);
    }

    @Override
    public String toString() {
        return "IkasanComponentPropertyMeta{" +
                "paramGroupNumber=" + paramGroupNumber +
                ", causesUserCodeRegeneration=" + causesUserCodeRegeneration +
                ", mandatory=" + mandatory +
                ", userImplementedClass=" + userImplementedClass +
                ", userDefineResource=" + userDefineResource +
                ", propertyName='" + propertyName + '\'' +
                ", propertyConfigFileLabel='" + propertyConfigFileLabel + '\'' +
                ", propertyDataType=" + propertyDataType +
                ", usageDataType=" + usageDataType +
                ", validation=" + validation +
                ", validationMessage=" + validationMessage +
                ", validationPattern=" + validationPattern +
                ", defaultValue=" + defaultValue +
                ", helpText='" + helpText + '\'' +
                '}';
    }
}
