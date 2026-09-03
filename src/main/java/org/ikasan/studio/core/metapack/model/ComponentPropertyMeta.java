package org.ikasan.studio.core.metapack.model;

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
    public static final String REQUIRES_STUB = "requiresStub";                              // If false, userImplementedClassName is an existing, fully-qualified class - Studio must not generate a stub for it
    public static final String CONFIGURATION = "configuration";                             // Bean containing user defined, Ikasan maintained properties
    public static final String CONFIGURED_RESOURCE_INTERFACE = "configuredResource";        // Is the configuration exposed in the dashboard.
    public static final String IS_CONFIGURED_RESOURCE = "isConfiguredResource";             // Is the configuration exposed in the dashboard.
    public static final String FROM_TYPE = "fromType";                                      // Special meta for converter, the type of the inbound payload
    public static final String TO_TYPE = "toType";                                          // Special meta for converter, the type of the outbound payload
    public static final String ROUTE_NAMES = "routeNames";                                  // Special meta for converter, the type of the outbound payload
    public static final String TYPE = "type";                                               // Special meta for translator, the type of the outbound payload
    public static final String AUTO_CONTENT_CONVERSION = "autoContentConversion";
    public static final String CONVERSION_RECIPE_ID = "conversionRecipeId";            // Hidden instance selection of a meta-pack conversion recipe.

    public static final String APPLICATION_PORT_NUMBER_NAME = "port";
    public static final String APPLICATION_PORT_NUMBER_KEY = "server.port";
    public static final String H2_DB_PORT_NUMBER_NAME = "h2DbPortNumber";
    public static final String H2_WEB_PORT_NUMBER_NAME = "h2WebPortNumber";
    public static final String H2_PORT_NUMBER_KEY = "h2.db.port";
    public static final String USE_EMBEDDED_H2 = "useEmbeddedH2";
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
    public static final String SUBSTITUTION_NAME_VALUE_DELIM = ":";
    public static final String SUBSTITUTION_PAIR_DELIM = ",";
    public static final String SUBSTITUTION_FIELD_NAME = "__fieldName" + SUBSTITUTION_NAME_VALUE_DELIM;

    public static final String STRING_LIST = "java.util.List<String>";
    // Marker usageDataType for a property the user fills in as a fully-qualified class name. Two independent
    // effects, either or both may apply depending on the property:
    //  1. If the real setter takes a java.lang.Class literal (e.g.
    //     ObjectToXmlStringConverterBuilder#setObjectClass(Class)), StudioBuildUtils#toJavaLiteral appends
    //     ".class" when rendering the value into generated code.
    //  2. The Properties panel (see ComponentPropertyEditRow) offers a "Choose Class..." project-scope chooser
    //     for it, since the value is always an existing project class either way.
    // A property whose template uses the raw value directly (e.g. Translator's "type", used as a generic type
    // parameter/method parameter type, not a Class argument) only picks up effect 2 - toJavaLiteral is never
    // called on it, so no ".class" ever gets appended.
    public static final String CLASS_LITERAL = "classLiteral";

    public static final String PROPERTY_GROUP_ADVANCED = "advanced";           // Rendered last of all groups in the Optional Properties section
    public static final String PROPERTY_GROUP_MISCELLANEOUS = "Miscellaneous"; // Catch-all for non-mandatory properties with no explicit propertyGroup

    public static final ComponentPropertyMeta DUMB_VERSION =
            ComponentPropertyMeta.builder()
                    .propertyName(ComponentPropertyMeta.VERSION)
                    .build();

    @JsonKey
    private String propertyName;            // The name / identity of the property, e.g. 'name' (used only for flows and modules), 'componentName', 'description', 'configuredResourceId', 'port'
    private String displayLabel;            // Optional display label override; if set, shown in the UI instead of propertyName
    private String trueLabel;               // For boolean properties: optional label shown next to the "true" checkbox instead of "True", e.g. "Topic"
    private String falseLabel;              // For boolean properties: optional label shown next to the "false" checkbox instead of "False", e.g. "Queue"
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean affectsUserImplementedClass = false;  // A change to this property should result in an update to the user implemnted class
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private List<String> choices;           // The value can be only one of the items in this list
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean choicesEditable = false; // When true, the choices dropdown is also directly editable - a free-text
                                              // escape hatch for values outside the suggested list (e.g. emailFormat's
                                              // MIME type), rather than choices being the only permitted values.
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
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private List<String> mandatoryUnlessAnyOf;  // This property is treated as mandatory unless at least one of these
                                                 // sibling property names (by propertyName) currently has a genuinely
                                                 // set value - e.g. password's ["privateKeyFilename"] and
                                                 // privateKeyFilename's ["password"] express that exactly one of the
                                                 // two credential mechanisms must be supplied, without forcing both.
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private String mandatorySectionHeading;     // Optional: clusters this property, together with every other mandatory-
                                                 // section property sharing the exact same heading text, under a titled
                                                 // sub-panel inside the always-visible Mandatory Properties section (see
                                                 // ComponentPropertiesPanel#populatePropertiesEditorPanel) - e.g. Email
                                                 // Producer's six recipient fields all carry "At least one of..." here.
                                                 // Deliberately distinct from propertyGroup, which instead moves a
                                                 // property into the collapsed-by-default Optional Properties panel -
                                                 // see feedback_mandatory_properties_must_stay_ungrouped.
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private String mandatoryIfTrue;             // This property is treated as mandatory whenever the named sibling
                                                 // boolean property (by propertyName) currently holds a genuinely
                                                 // set value of true - e.g. FtpConsumer's ftpsKeyStoreFilePath has
                                                 // mandatoryIfTrue "ftps", since the underlying connector always
                                                 // requires a keystore path once ftps is enabled, with no fallback.
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    private int propertyDisplayOrder = 0;   // Optional explicit display order in the properties panel; 0 (default) preserves JSON insertion order
    private String propertyConfigFileLabel; // Identifies the spring injected property name
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    @Builder.Default
    @SuppressWarnings("rawtypes")
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
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean protectFromOverwrite = false;  // For userSuppliedClass properties that are genuinely bespoke (no pre-built implementation exists):
                                                     // generate the stub into the user's protected source root and never silently overwrite it again.
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    @Builder.Default
    private String propertyGroup = "";              // Logical group this property is shown under in the Optional Properties section, e.g. "advanced".
                                                     // Assigning a group pulls the property into that group's sub-section of Optional Properties even
                                                     // if userSuppliedClass/affectsUserImplementedClass would otherwise place it in the regenerating
                                                     // section. Non-mandatory properties with no group fall back to PROPERTY_GROUP_MISCELLANEOUS.
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    @Builder.Default
    private boolean noStubRequired = false;         // For userSuppliedClass properties that are always an externally-injected bean
                                                     // (e.g. a JTA transaction manager, a JMS ConnectionFactory) - keep the @Resource
                                                     // bean-wiring that userSuppliedClass drives in the generated factory, but never
                                                     // generate a stub class for it (a stub is never correct/useful here).

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

    @JsonIgnore
    public boolean isGroupedProperty() {
        return propertyGroup != null && !propertyGroup.isBlank();
    }

    @JsonIgnore
    public boolean hasMandatoryUnlessAnyOf() {
        return mandatoryUnlessAnyOf != null && !mandatoryUnlessAnyOf.isEmpty();
    }

    @JsonIgnore
    public boolean hasMandatoryIfTrue() {
        return mandatoryIfTrue != null && !mandatoryIfTrue.isBlank();
    }

    @JsonIgnore
    public boolean hasMandatorySectionHeading() {
        return mandatorySectionHeading != null && !mandatorySectionHeading.isBlank();
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
                choicesEditable == that.choicesEditable &&
                hiddenProperty == that.hiddenProperty &&
                ignoreProperty == that.ignoreProperty &&
                mandatory == that.mandatory &&
                readOnlyProperty == that.readOnlyProperty &&
                setterProperty == that.setterProperty &&
                userDefineResource == that.userDefineResource &&
                userSuppliedClass == that.userSuppliedClass &&
                protectFromOverwrite == that.protectFromOverwrite &&
                noStubRequired == that.noStubRequired &&
                Objects.equals(propertyName, that.propertyName) &&
                Objects.equals(propertyGroup, that.propertyGroup) &&
                Objects.equals(trueLabel, that.trueLabel) &&
                Objects.equals(falseLabel, that.falseLabel) &&
                Objects.equals(choices, that.choices) &&
                Objects.equals(mandatoryUnlessAnyOf, that.mandatoryUnlessAnyOf) &&
                Objects.equals(mandatoryIfTrue, that.mandatoryIfTrue) &&
                Objects.equals(mandatorySectionHeading, that.mandatorySectionHeading) &&
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
                this.getValidationPattern() != null && this.getValidationPattern().pattern().equals(that.getValidationPattern().pattern());
    }

    @Override
    public int hashCode() {

        return Objects.hash(propertyName, propertyGroup, trueLabel, falseLabel, affectsUserImplementedClass, choicesEditable, choices,
                mandatoryUnlessAnyOf, mandatoryIfTrue, mandatorySectionHeading, dataValidationType, defaultValue, helpText,
                hiddenProperty, ignoreProperty, mandatory, propertyConfigFileLabel, propertyDataType, readOnlyProperty, setterProperty,
                setterMethod, usageDataType, userDefineResource, userImplementClassFtlTemplate, userSuppliedClass,
                protectFromOverwrite, noStubRequired,
                validation, validationMessage,
                validationPattern!= null ? validationPattern.pattern() : "");
    }
}
