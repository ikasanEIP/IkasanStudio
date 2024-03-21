package org.ikasan.studio.core.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.regex.Pattern;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
/*
 * Each Ikasan Component will have multiple properties e.g. name, description, configuredResourceID
 * This class holds the metadata about a single property e.g. 'description' - is i mandatory, what data type is it
 */
public class ComponentPropertyMeta {
    public static final String USER_IMPLEMENTED_CLASS_NAME = "userImplementedClassName";         // Special meta for a user implemented class used as a property
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

    // Special META for package parent of the users own packages, a little like a pom group
    public static final String APPLICATION_PACKAGE_NAME = "applicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";

    // List of all known properties any components could have
    public static final String CONFIGURATION_ID = "configurationId";
    public static final String TO = "to";
    public static final String FROM = "from";

    public static final ComponentPropertyMeta STD_NAME_META_COMPONENT =
            ComponentPropertyMeta.builder()
                    .propertyName(NAME)
                    .mandatory(true)
                    .build();

    public static final ComponentPropertyMeta STD_DESCRIPTION_META_COMPONENT =
        ComponentPropertyMeta.builder()
            .propertyName(DESCRIPTION)
            .helpText("A more detailed description of the component that may assist in support.")
            .build();

    public static final ComponentPropertyMeta STD_PACKAGE_NAME_META_COMPONENT =
        ComponentPropertyMeta.builder()
            .propertyName(APPLICATION_PACKAGE_NAME)
            .mandatory(true)
            .helpText("The base java package for your application.")
            .build();

    // Special META to model the port number to be used to launch the app and part of its user driven config.
    public static final ComponentPropertyMeta STD_PORT_NUMBER_META_COMPONENT =
            ComponentPropertyMeta.builder()
                    .propertyName(APPLICATION_PORT_NUMBER_NAME)
                    .mandatory(true)
                    .helpText("The port number that the running application will use locally.")
                    .build();

    @JsonKey
    private String propertyName;

    private String userImplementClassFtlTemplate;
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean affectsUserImplementedClass=false;  // A change to this property should result in an update to the user implemnted class
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private List<String> choices;           // The value can be only one of the items in this list
    private String componentType;           // Features in the serialised model.json, the interface or short form type for the property
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private Object defaultValue = "";       // Default value e.g. displayed when property is created.
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String helpText = "";           // Describes the property, typically popping up on tooltips.

    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean mandatory=false;        // The value must be supplied for the component to be valid
    private String propertyConfigFileLabel; // Identifies the spring injected property name
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private Class propertyDataType = java.lang.String.class;    // Of the property
    @JsonSetter(nulls = Nulls.SKIP)                             // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean setterProperty=false;   // The component features in the component factory setter
    private String standardBuilderMethod;
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String usageDataType = "";      // The interface od properties that are classes i.e. a user implemented class that must implement this interface
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean userDefineResource=false;       // The user will define the details of the resource within the ResourceFactory.
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean userSuppliedClass=false;     // The user will define their own class that implements the interface, we will generate the spring property but leave implementation to client code.

    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String validation = "";         // The String representation of the regexp validation pattern
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String validationMessage = "";  // Message to be displayed when the object fails validation
    private Pattern validationPattern;      // Set internally when first got, if validation attribute exists, this is the compiled pattern

    public boolean isVoid() {
        return propertyDataType == null;
    }


    public void setPropertyDataType(String dataType) {
        if (dataType != null && !dataType.isEmpty()) {
            try {
                propertyDataType = Class.forName(dataType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Patterns are expensive, so only generate one when we need it but share the same one thereafter.
     * @return a compiled Pattern
     */
    public Pattern getValidationPattern() {
        if (validationPattern == null && validation != null && !validation.isBlank()) {
            this.validationPattern = Pattern.compile(validation);
        }
        return validationPattern;
    }
}
