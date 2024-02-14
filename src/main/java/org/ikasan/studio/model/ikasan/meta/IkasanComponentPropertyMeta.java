package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.regex.Pattern;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
/*
 * Each Ikasan Component will have multiple properties e.g. name, description, configuredResourceID
 * This class holds the metadata about a single property e.g. 'description' - is i mandatory, what data type is it
 */
public class IkasanComponentPropertyMeta {
    public static final String BESPOKE_CLASS_NAME = "bespokeClassName";         // Special meta for a bespoke class used as a property
    public static final String CONFIGURATION = "configuration";                 // Bean containing user defined, Ikasan maintained properties
    public static final String CONFIGURED_RESOURCE_INTERFACE = "configuredResource";   // Is the configuration exposed in the dashboard.
    public static final String IS_CONFIGURED_RESOURCE = "isConfiguredResource";   // Is the configuration exposed in the dashboard.
    public static final String FROM_TYPE = "fromType";                          // Special meta for converter, the type of the inbound payload
    public static final String TO_TYPE = "toType";                              // Special meta for converter, the type of the outbound payload

    // Special META for component NAME, this standard for each component.
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String COMPONENT_NAME = "componentName";
    public static final String DESCRIPTION = "description";

    public static final String APPLICATION_PORT_NUMBER_NAME = "port";
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final String H2_DB_PORT_NUMBER_NAME = "h2DbPortNumber";
    public static final String H2_WEB_PORT_NUMBER_NAME = "h2WebPortNumber";
    public static final String H2_PORT_NUMBER_KEY = "h2.db.port";

    // Special META for package parent of the users bespoke packages, a little like a pom group
    public static final String APPLICATION_PACKAGE_NAME = "applicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";

    public static final IkasanComponentPropertyMeta STD_NAME_META_COMPONENT =
            IkasanComponentPropertyMeta.builder()
                    .propertyName(NAME)
                    .paramGroupNumber(1)
                    .mandatory(true)
                    .build();

    public static final IkasanComponentPropertyMeta STD_DESCRIPTION_META_COMPONENT =
        IkasanComponentPropertyMeta.builder()
            .propertyName(DESCRIPTION)
            .paramGroupNumber(1)
            .helpText("A more detailed description of the component that may assist in support.")
            .build();

    public static final IkasanComponentPropertyMeta STD_PACKAGE_NAME_META_COMPONENT =
        IkasanComponentPropertyMeta.builder()
            .propertyName(APPLICATION_PACKAGE_NAME)
            .paramGroupNumber(1)
            .mandatory(true)
            .helpText("The base java package for your application.")
            .build();

    // Special META to model the port number to be used to launch the app and part of its user driven config.
    public static final IkasanComponentPropertyMeta STD_PORT_NUMBER_META_COMPONENT =
            IkasanComponentPropertyMeta.builder()
                    .propertyName(APPLICATION_PORT_NUMBER_NAME)
                    .paramGroupNumber(1)
                    .mandatory(true)
                    .helpText("The port number that the running application will use locally.")
                    .build();

    @JsonKey
    private String propertyName;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private Integer paramGroupNumber = 1;
    private boolean causesUserCodeRegeneration;
    private boolean mandatory;
    private boolean userImplementedClass;
    private boolean setterProperty;
    private boolean userDefineResource;
    private String propertyConfigFileLabel;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private Class propertyDataType = java.lang.String.class;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String usageDataType = "";
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String validation = "";
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String validationMessage = "";
    private Pattern validationPattern;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private Object defaultValue = "";
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String helpText = "";


    private String componentType;
    private String standardBuilderMethod;

//   /**
//    * A IkasanComponentPropertyMeta could be a leaf i.e. hold a single set of metadata or could nest another set of metadata
//    *
//    * @param paramGroupNumber Supports sets of params e.g. overloaded methods
//    * @param causesUserCodeRegeneration Causes the user code to be regenerated
//    * @param mandatory for the component to be deemed to be complete
//    * @param userImplementedClass The user will define a beskpoke class that implements the interface, we will generate the spring property but leave implementation to client code.
//    * @param setterProperty The property should feature in the component factory setter.
//    * @param userDefineResource The user will define the details of the resource within the ResourceFactory.
//    * @param propertyName of the property, used on input screens and to generate variable names
//    * @param propertyConfigFileLabel Identifies the spring injected property name
//    * @param propertyDataType of the property
//    * @param validation validation string (used by the property editor to validate the format of the data)
//    * @param validationMessage to display if the field failr validation
//    * @param defaultValue for the property
//    * @param helpText for the property
//    */
//   /* @TODO convert to builder */
//    public IkasanComponentPropertyMeta(@NotNull Integer paramGroupNumber,
//                                        @NotNull boolean causesUserCodeRegeneration,
//                                        @NotNull boolean mandatory,
//                                        @NotNull boolean userImplementedClass,
//                                        boolean setterProperty,
//                                        boolean userDefineResource,
//                                        @NotNull String propertyName,
//                                        String propertyConfigFileLabel,
//                                        @NotNull Class propertyDataType,
//                                        String usageDataType,
//                                        String validation,
//                                        String validationMessage,
//                                        Object defaultValue,
//                                        String helpText) {
//        this.paramGroupNumber = paramGroupNumber;
//        this.causesUserCodeRegeneration = causesUserCodeRegeneration;
//        this.mandatory = mandatory;
//        this.userImplementedClass = userImplementedClass;
//        this.setterProperty = setterProperty;
//        this.userDefineResource = userDefineResource;
//        this.propertyName = propertyName;
//        this.propertyConfigFileLabel = propertyConfigFileLabel;
//        this.propertyDataType = propertyDataType;
//        if (usageDataType != null && usageDataType.length() > 0) {
//            this.usageDataType = usageDataType;
//        } else if (propertyDataType != null) {
//            this.usageDataType = propertyDataType.getCanonicalName();
//        }
//        this.validation = validation;
//        if (validation != null && !validation.isEmpty()) {
//            this.validationPattern = Pattern.compile(validation);
//            this.validationMessage = validationMessage;
//        }
//        this.defaultValue = defaultValue;
//        this.helpText = helpText;
//    }

    public boolean isVoid() {
        return propertyDataType == null;
    }

    public  void setPropertyDataType(String dataType) {
        if (dataType != null && !dataType.isEmpty()) {
            try {
                propertyDataType = Class.forName(dataType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void setValidation(String validation) {
        if (validation != null && !validation.isEmpty()) {
            this.validationPattern = Pattern.compile(validation);
        }
    }
}
