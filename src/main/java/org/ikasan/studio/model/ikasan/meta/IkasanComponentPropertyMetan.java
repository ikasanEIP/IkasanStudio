package org.ikasan.studio.model.ikasan.meta;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;
import java.util.regex.Pattern;

@Data
@Builder
@Jacksonized
/**
 * Represents all the possible properties an Ikasan component is allowed to have
 */
public class IkasanComponentPropertyMetan {
    public static final String BESPOKE_CLASS_NAME = "BespokeClassName";         // Special meta for a bespoke class used as a property
    public static final String CONFIGURATION = "Configuration";                 // Bean containing user defined, Ikasan maintained properties
    public static final String CONFIGURED_RESOURCE_INTERFACE = "ConfiguredResource";   // Is the configuration exposed in the dashboard.
    public static final String IS_CONFIGURED_RESOURCE = "IsConfiguredResource";   // Is the configuration exposed in the dashboard.
    public static final String FROM_TYPE = "FromType";                          // Special meta for converter, the type of the inbound payload
    public static final String TO_TYPE = "ToType";                              // Special meta for converter, the type of the outbound payload

    // Special META for component NAME, this standard for each component.
    public static final String NAME = "Name";
    public static final IkasanComponentPropertyMetan STD_NAME_META_COMPONENT =
        new IkasanComponentPropertyMetan(1, false, true, false, false, true,
            NAME, null, String.class, "", "", "", "",
            "The name of the component as displayed on diagrams, space are encouraged, succinct is best. The name should be unique for the flow.");

    // Special META for component DESCRIPTION, this standard for each component.
    public static final String DESCRIPTION = "Description";
    public static final IkasanComponentPropertyMetan STD_DESCRIPTION_META_COMPONENT =
        new IkasanComponentPropertyMetan(1, false, false, false, false, true,
            DESCRIPTION, null, String.class, "", "", "", "",
            "A more detailed description of the component that may assist in support.");

    // Special META for package parent of the users bespoke packages, a little like a pom group
    public static final String APPLICATION_PACKAGE_NAME = "ApplicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";
    public static final IkasanComponentPropertyMetan STD_PACKAGE_NAME_META_COMPONENT =
        new IkasanComponentPropertyMetan(1, false, true, false, false, false,
            APPLICATION_PACKAGE_NAME, null, String.class, "", "", "", "",
            "The base java package for your application.");

    // Special META to model the port number to be used to launch the app and part of its user driven config.
    public static final String APPLICATION_PORT_NUMBER_NAME = "ApplicationPortNumber";
    public static final String H2_DB_PORT_NUMBER_NAME = "H2DbPortNumber";
    public static final String H2_WEB_PORT_NUMBER_NAME = "H2WebPortNumber";
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final String H2_PORT_NUMBER_KEY = "h2.db.port";
    public static final IkasanComponentPropertyMetan STD_PORT_NUMBER_META_COMPONENT =
            new IkasanComponentPropertyMetan(1, false, true, false, false, false,
                    APPLICATION_PORT_NUMBER_NAME, null, String.class, "", "", "", "",
                    "The port number that the running application will use locally.");

    private Integer paramGroupNumber;
    private boolean mandatory;
    private boolean causesUserCodeRegeneration;
    private boolean userImplementedClass;
    private boolean setterProperty;
    private boolean userDefineResource;
    private String propertyName;
    private String propertyConfigFileLabel;
    private Class propertyDataType;
    private String usageDataType;
    private String validation;
    private String validationMessage;
    private Pattern validationPattern;
    private Object defaultValue;
    private String helpText;

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
    public IkasanComponentPropertyMetan(@NotNull Integer paramGroupNumber,
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

    public boolean isVoid() {
        return propertyDataType == null;
    }

}
