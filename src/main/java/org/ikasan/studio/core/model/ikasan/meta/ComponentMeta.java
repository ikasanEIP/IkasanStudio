package org.ikasan.studio.core.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * See Readme.md for a description of the meta-pack and how it is used.
 *
 * This class holds the metadata for an Ikasan component e.g. an email producer, ftp consumer.
 *
 * The purpose of this metadata is to describe the component e.g.
 * * what does it do (help text),
 * * is it a component that is used at the start / end of a flow or within the body of the flow,
 * * what jars are required when this component is used,
 * * what properties does it have.
 *
 * This metadata is populated from the meta-pack, so there will be an instance of this class
 * - for each component
 * -   for each version of Ikasan supported by the plugin.
 *
 * These classes are used to generate the Java code, help the user fill in the correct details for each component, help the UI
 * to display the component and provide validation for the use of the component in a flow or on the visualiser.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class ComponentMeta implements IkasanMeta {

    // Essential Ikasan Components
    public static final String COMSUMER_TYPE = "Consumer";
    public static final String ROUTER_TYPE = "Router";
    public static final String END_POINT_TYPE = "End Point";
    public static final String FLOW_TYPE = "Flow";
    public static final String MODULE_TYPE = "Module";
    public static final String PRODUCER_TYPE = "Producer";
    public static final String EXCEPTION_RESOLVER_TYPE = "Exception Resolver";
    public static final String GENERIC_KEY = "Generic";  // This component is a generic component i.e. the user will supply the class implementing the interface of this component type
    public static final String DEBUG_KEY = "DebugTransition";

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
    private Map<String, ComponentPropertyMeta> allowableProperties = new HashMap<>();
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
    private boolean isEndpoint;                     // Is this component an endpoint e.g. DB endpoint, sftp location
    private boolean isInternalEndpoint;             // This endpoint is internal to the flow
    @JsonSetter(nulls = Nulls.SKIP)                 // If the supplied value is null, ignore it.
    private Set<Dependency> jarDependencies;        // for this component, these will be added to the pom if this compponent is dragged into the flow
    private boolean usesBuilderInFactory;           // used by ftl to generate the correct builder code
    private boolean useImplementingClassInFactory;  // When true, 'implementingClass' is used in the factory method to create a new instance of the component.
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    private String webHelpURL = DEFAULT_README;

    @JsonIgnore
    private ImageIcon smallIcon;
    @JsonIgnore
    private ImageIcon canvasIcon;

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
    public boolean isDebug() {
        return DEBUG_KEY.equals(additionalKey);
    }
    public boolean isGeneric() {
        return GENERIC_KEY.equals(additionalKey);
    }
    public boolean isRouter() {
        return ROUTER_TYPE.equals(componentTypeMeta.getComponentShortType());
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

    public String getDisplayComponentType() {
        if (componentType.contains(".")) {
            return componentType.substring(componentType.lastIndexOf('.') + 1).trim();
        } else {
            return "";
        }
    }

    public String getComponentType() {
        if (componentType == null || componentType.isEmpty()) {
            return componentTypeMeta.getComponentType();
        } else {
            return componentType;
        }
    }

    public int getDisplayOrder() {
        return componentTypeMeta.getDisplayOrder();
    }

    public String getHelpText() {
        if (helpText == null || helpText.isEmpty()) {
            return componentTypeMeta.getHelpText();
        } else {
            return helpText;
        }
    }
}
