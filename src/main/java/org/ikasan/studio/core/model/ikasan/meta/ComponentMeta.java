package org.ikasan.studio.core.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Data
@SuperBuilder
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

    private static final String DEFAULT_README = "Readme.md";

    public static final String ADDITIONAL_KEY = "additionalKey";
    public static final String COMPONENT_TYPE_KEY = "componentType";
    public static final String EXCEPTION_RESOLVER_KEY = "exceptionResolver";
    public static final String HELP_TEXT_KEY = "helpText";
    public static final String IMPLEMENTING_CLASS_KEY = "implementingClass";
    public static final String NAME_KEY = "name";
    public static final String EXCEPTIONS_CAUGHT_KEY = "exceptionsCaught";
    public static final String ACTION_KEY = "action";
    public static final String ACTION_PROPERTIES_KEY = "actionProperties";

    // DO NOT RENAME - Will affect model.json
    @lombok.NonNull
    private String name;
    private String additionalKey;  // only used by components where componentType + implementingClass are not unique e.g. Local File Consumer

    private String componentType;
    private String componentShortType;  // The componentType is a FQN to be persisted to module.json, this is the short form, using in logic.
    private String defaultValue;
    private int displayOrder;

    private boolean isEndpoint;             // Is this component an endpoint e.g. DB endpoint, sftp location
    private boolean isInternalEndpoint;     // This endpoint is internal to the flow
    private String endpointKey;             // Implies this component is not an endpoint, but has an endpoint, the name of which is endpointtKey
    private String endpointTextKey;         // The name of the property in the real component that the endpoint will display as text e.g. queuename

    private String flowBuilderMethod;
    private boolean generatesUserImplementedClass;
    private String helpText;
    private String ikasanComponentFactoryMethod;
    @lombok.NonNull
    private String implementingClass;
    @JsonSetter(nulls = Nulls.SKIP)         // If the supplied value is null, ignore it.
    private Set<Dependency> jarDependencies;
    @Builder.Default
    private Map<String, ComponentPropertyMeta> properties = new HashMap<>();
    private boolean usesBuilder;
    private boolean useImplementingClass;

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
        if (properties != null) {
            for (Map.Entry<String, ComponentPropertyMeta> entry : properties.entrySet()) {
                if (entry.getValue().isMandatory()) {
                    mandatoryProperties.put(entry.getKey(), new ComponentProperty(entry.getValue()));
                }
            }
        }
        return mandatoryProperties;
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public ComponentPropertyMeta getMetadata(String propertyName) {
        return properties.get(propertyName);
    }
    public boolean isConsumer() {
        return COMSUMER_TYPE.equals(componentShortType);
    }
    public boolean isRouter() {
        return ROUTER_TYPE.equals(componentShortType);
    }
    public boolean isEndpoint() {
        return END_POINT_TYPE.equals(componentShortType);
    }
    public boolean isProducer() {
        return PRODUCER_TYPE.equals(componentShortType);
    }
    public boolean isFlow() {
        return FLOW_TYPE.equals(componentShortType);
    }
    public boolean isModule() {
        return MODULE_TYPE.equals(componentShortType);
    }
    public boolean isExceptionResolver() {
        return EXCEPTION_RESOLVER_TYPE.equals(componentShortType);
    }

    public String getDisplayComponentType() {
        if (componentType.contains(".")) {
            return componentType.substring(componentType.lastIndexOf('.') + 1).trim();
        } else {
            return "";
        }
    }
}
