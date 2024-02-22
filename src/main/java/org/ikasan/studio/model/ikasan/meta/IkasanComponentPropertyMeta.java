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

    // List of all known properties any components could have
    public static final String CONFIGURATION_ID = "configurationId";
    public static final String TO = "to";
    public static final String TO_CLASS = "toClass";
    public static final String FROM = "from";
    public static final String FROM_CLASS = "fromClass";
    public static final String BESKPOKE_CLASS_NAME = "beskpokeClassName";

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
