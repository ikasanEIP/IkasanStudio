package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.core.model.ikasan.instance.serialization.ModuleDeserializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
@Getter
@Setter
public class FlowElement extends BasicElement {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDeserializer.class);
    public static final String DECORATORS_JSON_TAG = "decorators";
    @JsonIgnore
    private Flow containingFlow;
    private FlowRoute containingFlowRoute;
    private List<Decorator> decorators;

    public FlowElement() {}

    /**
     * FlowElement e.g. filter, producer, consumer
     * @param componentMeta e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param containingFlow for this element
     * @param containingFlowRoute for this element
     * @param componentName for this element
     * @param decorators e.g. wiretaps
     */
    @Builder (builderMethodName = "flowElementBuilder")
    protected FlowElement(ComponentMeta componentMeta, Flow containingFlow, FlowRoute containingFlowRoute, String componentName, List<Decorator> decorators) {
        super(componentMeta, null);
        if (!componentMeta.isExceptionResolver()) {
            setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, componentName);
        }
        this.containingFlow = containingFlow;
        this.containingFlowRoute = containingFlowRoute;
        this.decorators = decorators;
    }

    public void setContainingFlowRoute(FlowRoute containingFlowRoute) {
        if (containingFlowRoute != null) {
            this.containingFlowRoute = containingFlowRoute;
            this.containingFlow = containingFlowRoute.getFlow();
        }
    }

    public boolean hasWiretap() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isWiretap);
    }
    public boolean hasLogWiretap() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isLogWiretap);
    }

    public List<Decorator> getWiretaps() {
        return hasWiretap() ? decorators.stream().filter(Decorator::isWiretap).toList() : new ArrayList<>();
    }
    public List<Decorator> getLogWiretaps() {
        return hasLogWiretap() ? decorators.stream().filter(Decorator::isLogWiretap).toList() : new ArrayList<>();
    }

    public boolean hasBeforeDecorators() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isBefore);
    }
    public boolean hasAfterDecorators() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isAfter);
    }

    public List<Decorator> getBeforeDecorators() {
        return hasWiretap() ? decorators.stream().filter(Decorator::isBefore).toList() : new ArrayList<>();
    }
    public List<Decorator> getAfterDecorators() {
        return hasLogWiretap() ? decorators.stream().filter(Decorator::isAfter).toList() : new ArrayList<>();
    }

    /**
     * Attempt to add the decorator to the flow element, if the decorator is already added, the action is ignored.
     * @param decorator to be added
     */
    public void addDecorator(Decorator decorator) {
        if (decorator != null && decorator.isValid()) {
            if (decorators == null) {
                decorators = new ArrayList<>();
                decorators.add(decorator);
            } else if (!decorators.contains(decorator)) {
                decorators.add(decorator);
            }
        } else {
            LOG.warn("STUDIO: WARN, attempt to add invalid decorator of [" + decorator + "] was ignored");
        }
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", name='" + getComponentName() + '\'' +
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getName()) + '\'' +
                ", containingFlowRoute ='" + (containingFlowRoute == null ? null : containingFlowRoute.getRouteName()) + '\'' +
                ", flowComponent='" + getComponentMeta().getName() + '\'' +
                ", properties=" + componentProperties +
                '}';
    }

    @Override
    public String toSimpleString() {
        return "{" +
                super.toSimpleString() +
                ", flowComponent='" + getComponentMeta().getName() + '\'' +
                ", flowName='" + getComponentName() + '\'' +
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getName()) + '\'' +
                ", containingFlowRoute ='" + (containingFlowRoute == null ? null : containingFlowRoute.getRouteName()) + '\'' +
                '}';
    }
}
