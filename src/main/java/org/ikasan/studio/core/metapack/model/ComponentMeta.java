package org.ikasan.studio.core.metapack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * See Readme.md for a description of the meta-pack and how it is used.
 * -
 * This class holds the metadata for an Ikasan component e.g. an email producer, ftp consumer.
 * -
 * The purpose of this metadata is to describe the component e.g.
 * * what does it do (help text),
 * * is it a component that is used at the start / end of a flow or within the body of the flow,
 * * what jars are required when this component is used,
 * * what properties does it have.
 * -
 * This metadata is populated from the meta-pack, so there will be an instance of this class
 * - for each component
 * -   for each version of Ikasan supported by the plugin.
 * -
 * These classes are used to generate the Java code, help the user fill in the correct details for each component, help the UI
 * to display the component and provide validation for the use of the component in a flow or on the visualiser.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
@EqualsAndHashCode
public class ComponentMeta implements IkasanMeta {

    // Essential Ikasan Components
    public static final String COMSUMER_TYPE = "Consumer";
    public static final String ROUTER_TYPE = "Router";
    public static final String FILTER_TYPE = "Filter";
    public static final String TRANSLATOR_TYPE = "Translator";
    public static final String END_POINT_TYPE = "End Point";
    public static final String FLOW_TYPE = "Flow";
    public static final String MODULE_TYPE = "Module";
    public static final String PRODUCER_TYPE = "Producer";
    public static final String EXCEPTION_RESOLVER_TYPE = "Exception Resolver";
    public static final String GENERIC_KEY = "Generic";  // This component is a generic component i.e. the user will supply the class implementing the interface of this component type
    public static final String DEBUG_KEY = "DebugTransition";
    // Shared by every "time event" consumer (Scheduled/FTP/SFTP/Local File Consumer) - see isTimeEventConsumer().
    public static final String SCHEDULED_CONSUMER_IMPLEMENTING_CLASS = "org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer";
    // See producesFileListPayload().
    public static final String FILE_LIST_TYPE = "java.util.List<java.io.File>";
    // See isSelfGeneratingConsumer().
    public static final String EVENT_GENERATING_CONSUMER_IMPLEMENTING_CLASS = "org.ikasan.component.endpoint.consumer.EventGeneratingConsumer";
    // See supportsTestMailServer().
    public static final String EMAIL_PRODUCER_IMPLEMENTING_CLASS = "org.ikasan.component.endpoint.email.producer.EmailProducer";

    private static final String DEFAULT_README = "Readme.md";

    public static final String ADDITIONAL_KEY = "additionalKey";
    public static final String COMPONENT_TYPE_KEY = "componentType";
    public static final String CONFIGURATION_ID_KEY = "configurationId";
    public static final String CONFIGURABLE_KEY = "configurable";
    public static final String DECORATORS_KEY = "decorators";
    public static final String EXCEPTION_RESOLVER_KEY = "exceptionResolver";
    public static final String HELP_TEXT_KEY = "helpText";
    public static final String IMPLEMENTING_CLASS_KEY = "implementingClass";
    public static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";
    public static final String EXCEPTIONS_CAUGHT_KEY = "exceptionsCaught";
    public static final String ACTION_KEY = "action";
    public static final String ACTION_PROPERTIES_KEY = "actionProperties";

    // These fields map directly onto the values in component-meta_en_GB.json
    // DO NOT RENAME !!
    @lombok.NonNull
    private String name;                            // Name of the component e.g. Email Producer, FTP Consumer
    private String additionalKey;                   // only used by components where componentType + implementingClass are not unique e.g. Local File Consumer, or to indicate the component is Generic
    @Builder.Default                                // The list of properties that this component is allowed to have e.g. 'name', 'port', 'url', these will be added to ComponentTypeMeta allowableProperties
    private Map<String, ComponentPropertyMeta> allowableProperties = new LinkedHashMap<>();
    private String componentType;                   // The type can be that of the group type (see componentTypeMeta) or a type specific to this component.
    @JsonIgnore
    private ComponentTypeMeta componentTypeMeta;    // The meta associated with the component type from the metapack, e.g. broker, producer, consumer, router, etc.
    private String endpointKey;                     // Implies this component is not an endpoint, but has an endpoint, the name of which is endpointtKey
    private String endpointTextKey;                 // The name of the property in the real component that the endpoint will display as text e.g. queuename
    private String flowBuilderMethod;               // used by ftl to invoke the correct flow builder method for this component
    private boolean generatesUserImplementedClass;  // If true, the component will generate a user-implemented class, e.g. a custom filter or transformer.
    private String helpText;                        // The help text for this component, used in the component properties' dialog.
    private String ikasanComponentFactoryMethod;    // used by ftl to invoke the correct factory method for this component
    @lombok.NonNull
    private String implementingClass;               // e.g. org.ikasan.spec.component.filter.Filter.Custom
    private String expectedInputType;               // Optional: a fully-qualified type (or simple name e.g. "List") this component's incoming
                                                      // payload is expected to be assignable to, e.g. Default List Splitter expects "java.util.List".
                                                      // Used for components with no user-configurable input type of their own - see
                                                      // expectedInputTypeProperty below for components that do. Drives a best-effort canvas warning
                                                      // (see FlowElement#getUpstreamTypeMismatchWarning) when the nearest upstream component's
                                                      // declared 'toType' clearly doesn't match - absent (both this and expectedInputTypeProperty
                                                      // apply their own fallbacks) means no check is attempted for this component.
    private String expectedInputTypeProperty;        // Optional: names which of THIS component's own properties declares its expected input
                                                      // type, for components (Converter, Broker, Splitter, Filter, Router, Producer, ...) whose
                                                      // expected input type is user-configurable rather than fixed. Defaults to 'fromType' when
                                                      // absent, since that is the overwhelming convention - only needed where a component uses a
                                                      // differently-named property for the same purpose (e.g. Object To XML String Converter's
                                                      // 'objectClass'). See FlowElement#getUpstreamTypeMismatchWarning.
    private String producedOutputType;               // A fixed, fully-qualified type this component hands downstream as the payload, for
                                                      // components with no 'toType' property of their own to hold a user-configured answer -
                                                      // e.g. Local File Consumer produces "java.util.List<java.io.File>" (see its default
                                                      // messageProvider, FileMessageProvider), Object To XML String Converter produces
                                                      // "java.lang.String". Consumers are the main users of this (they have no 'toType' at all,
                                                      // since nothing flows into them to transform), but any component whose real output type is
                                                      // fixed rather than user-configurable can use it. Absent means either the component is
                                                      // generic (isGeneric() - depends entirely on the user's own implementation), genuinely
                                                      // configuration-dependent (e.g. the base Scheduled Consumer's messageProvider), or its real
                                                      // output genuinely isn't captured anywhere in Studio's metadata (e.g. Default List
                                                      // Splitter's output is whichever type was inside the incoming list). See
                                                      // FlowElement#getEffectiveOutputTypeDescription.
    private boolean routesToMultipleTargets;         // Router only: true if route() may return more than one target in a single
                                                      // invocation and so needs a List<String> return type (e.g. Multi Recipient Router) -
                                                      // false (the default) for routers whose route() returns a single String (e.g. Single
                                                      // Recipient Router). Drives routerTemplate_en.ftl's generated method shape - see there.
    private boolean isFileBasedConsumer;             // Consumer only: true if the payload it deals in is file content/a file path rather
                                                      // than a message (e.g. FTP/SFTP/Local File/Generic Consumer) - drives the canvas's
                                                      // file-flavoured Send Test Message badge, see IkasanFlowRouteViewHandler.
    private boolean isEndpoint;                     // Is this component an endpoint e.g. DB endpoint, sftp location
    private boolean isInternalEndpoint;             // This endpoint is internal to the flow
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    private Set<Dependency> jarDependencies;        // for this component, these will be added to the pom if this compponent is dragged into the flow
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    private Set<String> importResources;            // classpath Spring XML resources (e.g. "classpath:filetransfer-service-conf.xml") this component's
                                                      // builder method needs on the Spring context (beans not discoverable via component-scan), added to
                                                      // the generated ModuleConfig's @ImportResource if this component is dragged into the flow
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    private Set<String> importConfigurationClasses; // fully-qualified @Configuration classes (e.g.
                                                      // "org.ikasan.connector.basefiletransfer.BaseFileTransferAutoConfiguration") this component's
                                                      // builder method needs on the Spring context, added to the generated ModuleConfig's @Import if
                                                      // this component is dragged into the flow. Newer Ikasan versions favour these Java auto-configuration
                                                      // classes over the older Spring XML resources (see importResources above).
    private boolean usesBuilderInFactory;           // used by ftl to generate the correct builder code
    private boolean useImplementingClassInFactory;  // When true, 'implementingClass' is used in the factory method to create a new instance of the component.
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String webHelpURL = DEFAULT_README;


    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String iconResourceDirectory;

    public ComponentMeta() {}

    /**
     * Get a list of the mandatory properties for this component.
     * @return An ordered map of the mandatory properties for this component
     */
    public Map<String, ComponentProperty> getMandatoryInstanceProperties() {
        Map<String, ComponentProperty> mandatoryProperties = new TreeMap<>();
        if (allowableProperties != null) {
            for (Map.Entry<String, ComponentPropertyMeta> entry : allowableProperties.entrySet()) {
                if (entry.getValue().isMandatory()) {
                    mandatoryProperties.put(entry.getKey(), new ComponentProperty(entry.getValue()));
                }
            }
        }
        return mandatoryProperties;
    }

    public Set<String> getPropertyKeys() {
        return allowableProperties.keySet();
    }

    public ComponentPropertyMeta getMetadata(String propertyName) {
        return allowableProperties.get(propertyName);
    }
    public boolean isConsumer() {
        return COMSUMER_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    /**
     * True for every Consumer backed by Ikasan's Quartz ScheduledConsumer (Scheduled/FTP/SFTP/Local File
     * Consumer) - these are driven by a cron schedule rather than a broker/listener, so "at will" testing
     * means triggering an invocation now rather than injecting a payload.
     */
    public boolean isTimeEventConsumer() {
        return SCHEDULED_CONSUMER_IMPLEMENTING_CLASS.equals(implementingClass);
    }
    /**
     * True for Ikasan's EventGeneratingConsumer, which drives itself continuously (many events per second) once
     * the flow starts rather than waiting on a broker, listener or schedule - there's no meaningful "send one
     * test message"/"trigger now" action for it, since it's already firing on its own. See
     * {@link #supportsSendTestMessage()}, the positive-sense predicate callers should use instead.
     */
    private boolean isSelfGeneratingConsumer() {
        return EVENT_GENERATING_CONSUMER_IMPLEMENTING_CLASS.equals(implementingClass);
    }
    /**
     * True if this component should offer the canvas "Send Test Message"/"Trigger Now" action: any Consumer
     * except a self-generating one (Event Generating Consumer - see {@link #isSelfGeneratingConsumer()}), which
     * already fires continuously on its own and has nothing meaningful to trigger.
     */
    public boolean supportsSendTestMessage() {
        return isConsumer() && !isSelfGeneratingConsumer();
    }
    /**
     * True for the Email Producer - offers the canvas "Start Test Mail Server" action, which downloads (once,
     * cached) and launches MailHog, a local SMTP server with a web-based inbox UI, bound to this component's
     * configured mailSmtpHost/mailSmtpPort - so the user can see what the flow actually sends without a real
     * mail account. Gated on implementingClass rather than name, matching isTimeEventConsumer() above. A click
     * on the Email Endpoint node resolves back to this same Email Producer FlowElement before the context menu
     * is built (see DesignerCanvas#getComponentAtXY / IkasanFlowRouteViewHandler#getOwnerForEndpointAtXY), so
     * this one predicate covers both right-click targets the user expects it from.
     */
    public boolean supportsTestMailServer() {
        return EMAIL_PRODUCER_IMPLEMENTING_CLASS.equals(implementingClass);
    }
    public boolean isDebug() {
        return DEBUG_KEY.equals(additionalKey);
    }
    public boolean isGeneric() {
        return GENERIC_KEY.equals(additionalKey);
    }
    public boolean isRouter() {
        return ROUTER_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isFilter() {
        return FILTER_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isTranslator() {
        return TRANSLATOR_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isEndpoint() {
        return END_POINT_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isProducer() {
        return PRODUCER_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isFlow() {
        return FLOW_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isModule() {
        return MODULE_TYPE.equals(componentTypeMeta.getComponentShortType());
    }
    public boolean isExceptionResolver() {
        return EXCEPTION_RESOLVER_TYPE.equals(componentTypeMeta.getComponentShortType());
    }

//    public String getDisplayComponentType() {
//        if (componentType.contains(".")) {
//            return componentType.substring(componentType.lastIndexOf('.') + 1).trim();
//        } else {
//            return "";
//        }
//    }

    public String getComponentType() {
        if (componentType == null || componentType.isEmpty()) {
            return componentTypeMeta.getComponentType();
        } else {
            return componentType;
        }
    }

    public int getPaletteDisplayOrder() {
        return componentTypeMeta.getPaletteDisplayOrder();
    }

    public String getHelpText() {
        if (helpText == null || helpText.isEmpty()) {
            return componentTypeMeta.getHelpText();
        } else {
            return helpText;
        }
    }

    /**
     * Which of this component's own properties declares its expected input type - expectedInputTypeProperty
     * when set (e.g. Object To XML String Converter's 'objectClass'), otherwise the 'fromType' convention used
     * by Converter, Broker, Splitter, Filter, Router and Producer components. Used both to check a component's
     * declared input type against its upstream neighbour's declared output type (see
     * FlowElement#getUpstreamTypeMismatchWarning) and to suggest a starting value for it when the component is
     * first dropped onto the canvas (see DesignerCanvas#applySuggestedInputTypeFromUpstream) - callers must
     * still confirm the named property actually exists (via getMetadata) since most components have neither.
     */
    public String getEffectiveInputTypePropertyName() {
        return (expectedInputTypeProperty != null && !expectedInputTypeProperty.isBlank())
                ? expectedInputTypeProperty : ComponentPropertyMeta.FROM_TYPE;
    }

    /**
     * The type this component itself expects its incoming payload to be, or null if it doesn't declare one -
     * see {@link org.ikasan.studio.core.model.ikasan.instance.FlowElement#getEffectiveInputTypeDescription()}
     * for the full rules; this is the shared engine behind it, taking a propertyValueResolver so the same
     * logic works both against a real FlowElement's current property values (see there) and, with no
     * FlowElement instance at all, against this metadata's own property defaults (see
     * {@link org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler} - a palette item has no instance
     * yet, so it can only ever preview what a freshly-dropped one would default to).
     * @param propertyValueResolver given a property name, returns that property's current (or default) value
     * as a display string, or null/blank if unset
     */
    public String getEffectiveInputTypeDescription(Function<String, String> propertyValueResolver) {
        if (isConsumer()) {
            return null;
        }
        if (expectedInputType != null && !expectedInputType.isBlank()) {
            return expectedInputType;
        }
        // A component with no "fromType"/custom expectedInputTypeProperty at all (e.g. any Producer wrapping
        // an implementingClass directly, like Basic AMQ Spring JMS Producer) genuinely has nothing to state
        // here - blank-check the resolved value the same way getEffectiveOutputTypeDescription already does
        // for toType/producedOutputType below, rather than let a resolver's "not set" convention (some return
        // "" for a property that doesn't exist at all, not just an unset one - see BasicElement#getPropertyValueAsString)
        // leak through as a stray "Input:" with nothing after it.
        String resolvedInputType = propertyValueResolver.apply(getEffectiveInputTypePropertyName());
        return (resolvedInputType != null && !resolvedInputType.isBlank()) ? resolvedInputType : null;
    }

    /**
     * The type this component hands to whatever comes after it, or null if that can't be stated - see
     * {@link org.ikasan.studio.core.model.ikasan.instance.FlowElement#getEffectiveOutputTypeDescription()} for
     * the full rules; this is the shared engine behind it, see {@link #getEffectiveInputTypeDescription} for
     * why it takes a propertyValueResolver rather than reading property values itself.
     */
    public String getEffectiveOutputTypeDescription(Function<String, String> propertyValueResolver) {
        if (isProducer()) {
            return null;
        }
        if (isRouter() || isFilter() || isTranslator() || isDebug()) {
            return getEffectiveInputTypeDescription(propertyValueResolver);
        }
        String toType = propertyValueResolver.apply(ComponentPropertyMeta.TO_TYPE);
        if (toType != null && !toType.isBlank()) {
            return toType;
        }
        // A JMS consumer's declared producedOutputType (javax/jakarta.jms.Message) is only what's actually
        // delivered downstream while Auto Content Conversion is off - once it's on, JmsMessageConverter
        // unwraps the raw message before the flow ever sees it (see autoContentConversion's own help text for
        // the exact per-message-type mapping: TextMessage->String, MapMessage->Map, ObjectMessage->the
        // unwrapped Object, BytesMessage->byte[] - Studio has no way to know which of those a given consumer
        // will actually receive at runtime, so java.lang.Object is the one static type that's honestly always
        // correct here, rather than continuing to claim the never-actually-delivered raw Message type).
        if ("true".equalsIgnoreCase(propertyValueResolver.apply(ComponentPropertyMeta.AUTO_CONTENT_CONVERSION))) {
            return "java.lang.Object (auto-converted)";
        }
        return (producedOutputType != null && !producedOutputType.isBlank()) ? producedOutputType : null;
    }

    /**
     * True only for a Consumer whose declared payload really is a real {@code java.util.List<java.io.File>}
     * (currently just Local File Consumer) - narrower than {@link #isFileBasedConsumer()}, which also covers
     * FTP/SFTP Consumer (payload is {@code org.ikasan.filetransfer.Payload}, a richer transfer-metadata object
     * a local file picker can't honestly stand in for) and Generic Consumer (output type is whatever the user's
     * own implementation decides - not captured in metadata at all). Drives whether "Send Test Message" offers
     * a real (multi-select) file picker instead of the generic text/JSON payload dialog - see
     * SendTestMessageAction.
     */
    public boolean producesFileListPayload() {
        return FILE_LIST_TYPE.equals(producedOutputType);
    }

    /**
     * Preview of {@link #getEffectiveInputTypeDescription(Function)} with no FlowElement instance to read live
     * property values from (e.g. a palette item, which hasn't been dropped onto a flow yet) - falls back to
     * each property's own declared default value instead, i.e. what a freshly-dropped instance would start
     * with before the user changes anything.
     */
    public String getEffectiveInputTypeDescriptionPreview() {
        return getEffectiveInputTypeDescription(this::getDefaultValueAsString);
    }

    /** Preview counterpart to {@link #getEffectiveInputTypeDescriptionPreview()} - see there for why. */
    public String getEffectiveOutputTypeDescriptionPreview() {
        return getEffectiveOutputTypeDescription(this::getDefaultValueAsString);
    }

    private String getDefaultValueAsString(String propertyName) {
        ComponentPropertyMeta propertyMeta = getMetadata(propertyName);
        if (propertyMeta == null || propertyMeta.getDefaultValue() == null) {
            return null;
        }
        return String.valueOf(propertyMeta.getDefaultValue());
    }
}
