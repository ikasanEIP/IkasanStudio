package org.ikasan.studio.core.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Data
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor

/*
 * Each Ikasan Component will have multiple properties e.g. name, description, configuredResourceID
 * This class holds the metadata about a single property e.g. 'description' - is it mandatory, what data type is it
 */
public class ComponentPropertyMeta {
    public static final String NAME = "name";                       // The identity of Flows and Modules (according to Ikasan metadata.json)
    public static final String COMPONENT_NAME = "componentName";    // The identity of components (according to Ikasan metadata.json)
    public static final String VERSION = "version";
    public static final String DESCRIPTION = "description";

    public static final String USER_IMPLEMENTED_CLASS_NAME = "userImplementedClassName";    // Special meta for a user implemented class used as a property
    public static final String CONFIGURATION = "configuration";                             // Bean containing user defined, Ikasan maintained properties
    public static final String CONFIGURED_RESOURCE_INTERFACE = "configuredResource";        // Is the configuration exposed in the dashboard.
    public static final String IS_CONFIGURED_RESOURCE = "isConfiguredResource";             // Is the configuration exposed in the dashboard.
    public static final String FROM_TYPE = "fromType";                                      // Special meta for converter, the type of the inbound payload
    public static final String TO_TYPE = "toType";                                          // Special meta for converter, the type of the outbound payload
    public static final String ROUTE_NAMES = "routeNames";                                  // Special meta for converter, the type of the outbound payload
    public static final String TYPE = "type";                                               // Special meta for translator, the type of the outbound payload

    public static final String APPLICATION_PORT_NUMBER_NAME = "port";
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final String H2_DB_PORT_NUMBER_NAME = "h2DbPortNumber";
    public static final String H2_WEB_PORT_NUMBER_NAME = "h2WebPortNumber";
    public static final String H2_PORT_NUMBER_KEY = "h2.db.port";
    public static final String CONFIGURED_RESOURCE_ID = "configuredResourceId";

    // Special META for package parent of the users own packages, a little like a pom group
    public static final String APPLICATION_PACKAGE_NAME = "applicationPackageName";
    public static final String APPLICATION_PACKAGE_KEY = "module.package";

    // List of all known properties any components could have
    public static final String CONFIGURATION_ID = "configurationId";
    public static final String TO = "to";
    public static final String FROM = "from";

    // The substitution constants are used to indicate that the valud is not literal and should be replaced with a substitution field.
    public static final String SUBSTITUTION_PREFIX = "__";
    public static final String SUBSTITUTION_PREFIX_FLOW = "__flow";
    public static final String SUBSTITUTION_PREFIX_COMPONENT = "__component";
    public static final String SUBSTITUTION_PREFIX_MODULE = "__module";
    public static final String SUBSTITUTION_FIELD_NAME = "__fieldName:";

    public static final String STRING_LIST = "java.util.List<String>";

    public static final ComponentPropertyMeta DUMB_VERSION =
            ComponentPropertyMeta.builder()
                    .propertyName(ComponentPropertyMeta.VERSION)
                    .build();

    @JsonKey
    private String propertyName;            // The name / identity of the property, e.g. 'name' (used only for flows and modules), 'componentName', 'description', 'configuredResourceId', 'port'
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean affectsUserImplementedClass = false;  // A change to this property should result in an update to the user implemnted class
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private List<String> choices;           // The value can be only one of the items in this list
    private String componentType;           // Features in the serialised model.json, the interface or short form type for the property
    private String dataValidationType;      // Support for popup data entry helpers / data types
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private Object defaultValue = null;     // Default value e.g. displayed when a property is created.

    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String helpText = "";           // Describes the property, typically popping up on tooltips.
    private boolean hiddenProperty;         // The property is used by the templates but is not edited / shown to the user
    private boolean ignoreProperty;         // The property is consumed but not featured in any screens or used to update ftl

    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean mandatory = false;        // The value must be supplied for the component to be valid
    private String propertyConfigFileLabel; // Identifies the spring injected property name
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private Class propertyDataType = java.lang.String.class;    // Of the property
    private boolean readOnlyProperty;       // The property can be viewed but not changed
    @JsonSetter(nulls = Nulls.SKIP)                             // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean setterProperty = false;   // The component features in the component factory setter
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String setterMethod = "";         // Some properties in Ikasan do not follow convention e.g. configurationId is a property but its setter is setConfiguredResourceId
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private String usageDataType = "";      // The interface of properties that are classes i.e. a user implemented class that must implement this interface
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean userDefineResource = false;       // The user will define the details of the resource within the ResourceFactory.

    // userImplementedClass vs userSuppliedClass - here is the difference:
    // userImplementedClass - A component that the user implements, we generate the component stub from the interface
    // userSuppliedClass - A class used by a component, the implementation of which the use supplies
    private String userImplementClassFtlTemplate;
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean userSuppliedClass = false;     // The user will define their own class that implements the interface, we will generate the spring property but leave implementation to client code.

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

    public static boolean isIdentityKey(String propertyName) {
        return propertyName != null && (propertyName.equals(NAME) || propertyName.equals(COMPONENT_NAME));
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

    @JsonIgnore
    public boolean isOptional() {
        return (!isMandatory()) && (!isAffectsUserImplementedClass());
    }

    /**
     * Patterns are expensive, so only generate one when we need it but share the same one thereafter.
     *
     * @return a compiled Pattern
     */
    public Pattern getValidationPattern() {
        if (validationPattern == null && validation != null && !validation.isBlank()) {
            this.validationPattern = Pattern.compile(validation);
        }
        return validationPattern;
    }

    public static boolean isSubstitutionValue(Object value) {
        return value instanceof String && ((String) value).startsWith(SUBSTITUTION_PREFIX);
    }

    /**
     * Standard equals method to compare two ComponentPropertyMeta objects.
     * Note we can't use annotation bases because Pattern is a library class that does not expose field based equals.
     *
     * @param o to check
     * @return true if the value is a substitution value
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComponentPropertyMeta that)) return false;
        return affectsUserImplementedClass == that.affectsUserImplementedClass &&
                hiddenProperty == that.hiddenProperty &&
                ignoreProperty == that.ignoreProperty &&
                mandatory == that.mandatory &&
                readOnlyProperty == that.readOnlyProperty &&
                setterProperty == that.setterProperty &&
                userDefineResource == that.userDefineResource &&
                userSuppliedClass == that.userSuppliedClass &&
                Objects.equals(propertyName, that.propertyName) &&
                Objects.equals(choices, that.choices) &&
                Objects.equals(componentType, that.componentType) &&
                Objects.equals(dataValidationType, that.dataValidationType) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(helpText, that.helpText) &&
                Objects.equals(propertyConfigFileLabel, that.propertyConfigFileLabel) &&
                Objects.equals(propertyDataType, that.propertyDataType) &&
                Objects.equals(setterMethod, that.setterMethod) &&
                Objects.equals(usageDataType, that.usageDataType) &&
                Objects.equals(userImplementClassFtlTemplate, that.userImplementClassFtlTemplate) &&
                Objects.equals(validation, that.validation) &&
                Objects.equals(validationMessage, that.validationMessage) &&
                this.getValidationPattern() == null && that.getValidationPattern() == null ||
                this.getValidationPattern().pattern().equals(that.getValidationPattern().pattern());
    }

    @Override
    public int hashCode() {

        return Objects.hash(propertyName, affectsUserImplementedClass, choices, componentType, dataValidationType, defaultValue, helpText,
                hiddenProperty, ignoreProperty, mandatory, propertyConfigFileLabel, propertyDataType, readOnlyProperty, setterProperty,
                setterMethod, usageDataType, userDefineResource, userImplementClassFtlTemplate, userSuppliedClass,
                validation, validationMessage, validationPattern.pattern());
    }
}
