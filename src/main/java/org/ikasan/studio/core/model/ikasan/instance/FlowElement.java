package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
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
    static Logger LOG = LoggerFactory.getLogger(FlowElement.class);
    public static final String DECORATORS_JSON_TAG = "decorators";
    @JsonIgnore
    private Flow containingFlow;
    private FlowRoute containingFlowRoute;
    private List<Decorator> decorators;

    public FlowElement() {}
    protected void resetLogger(Logger logger) {
        LOG = logger;
    }

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

    public boolean hasDecorators() {
        return decorators != null && !decorators.isEmpty();
    }
    public boolean hasBeforeDecorators() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isBefore);
    }
    public boolean hasAfterDecorators() {
        return decorators != null && decorators.stream().anyMatch(Decorator::isAfter);
    }

    public List<Decorator> getBeforeDecorators() {
        return hasBeforeDecorators() ? decorators.stream().filter(Decorator::isBefore).toList() : new ArrayList<>();
    }
    public List<Decorator> getAfterDecorators() {
        return hasAfterDecorators() ? decorators.stream().filter(Decorator::isAfter).toList() : new ArrayList<>();
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
            LOG.warn("STUDIO: WARNING, attempt to add invalid decorator of [" + decorator + "] was ignored");
        }
    }
    /**
     * Attempt to delete the decorator from the flow element, if the decorator is absent, the action is ignored.
     * @param decoratorType to be removed
     * @param position to be removed
     */
    public void removeDecorator(DECORATOR_TYPE decoratorType, DECORATOR_POSITION position) {
        if (decorators != null) {
            Decorator toBeRemoved;
            toBeRemoved = getDecorators().stream()
                    .filter(t->decoratorType.equals(t.getType()))
                    .filter(p-> position.equals(p.getPosition()))
                    .findFirst()
                    .orElse(null);

            decorators.remove(toBeRemoved);
        }
    }

    /**
     * The intent is to clone the existing flow element but to a different meta-pack metapackVersion.
     * @param metapackVersion for cloned flow element
     * @return the cloned module with the new meta pack version
     * @throws StudioBuildException when cloning is not possible.
     */
    public FlowElement cloneToVersion(String metapackVersion, Flow containingFlow, FlowRoute containingFlowRoute) throws StudioBuildException {
        if (metapackVersion == null || metapackVersion.isBlank()) {
            LOG.error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
            return null;
        }
        ComponentMeta newComponentMeta = IkasanComponentLibrary.getIkasanComponentByKey(metapackVersion, this.getComponentMeta().getName());
        if (newComponentMeta == null) {
            throw new StudioBuildException("Component [" + this.getComponentMeta().getName() + "] not found in metapack version [" + metapackVersion + "]");
        }
        FlowElement clonedFlowElement = new FlowElement(newComponentMeta, containingFlow, containingFlowRoute, this.getComponentName(), this.getDecorators());
        super.cloneToVersion(clonedFlowElement);
        return clonedFlowElement;
    }

    /**
     * Best-effort design-time check: if this component's metadata declares an expectedInputType (e.g. Default
     * List Splitter expects java.util.List - see {@link ComponentMeta#getExpectedInputType()}) and the nearest
     * upstream element whose declared 'toType' represents the actual incoming payload (see
     * {@link Flow#findPayloadSourceElement}) has a type that clearly doesn't match, returns a warning message
     * suitable for display. Returns null whenever there isn't enough information to check - including the very
     * common case of an upstream Consumer, which never declares an output type in Studio's metadata - since
     * staying silent is safer than a false alarm; this is a textual heuristic, not real type resolution, so a
     * genuine mismatch can still slip through unflagged.
     * @return a human-readable warning, or null if nothing is wrong (or nothing could be checked)
     */
    public String getUpstreamTypeMismatchWarning() {
        if (getComponentMeta() == null || containingFlow == null) {
            return null;
        }
        String expectedInputType = getComponentMeta().getExpectedInputType();
        if (expectedInputType == null || expectedInputType.isBlank()) {
            return null;
        }
        FlowElement upstream = containingFlow.findPayloadSourceElement(this);
        if (upstream == null) {
            return null;
        }
        String upstreamOutputType = upstream.getPropertyValueAsString(ComponentPropertyMeta.TO_TYPE);
        if (upstreamOutputType == null || upstreamOutputType.isBlank()) {
            return null;
        }
        String expectedSimpleName = expectedInputType.substring(expectedInputType.lastIndexOf('.') + 1);
        if (upstreamOutputType.toLowerCase().contains(expectedSimpleName.toLowerCase())) {
            return null;
        }
        return "Possible type mismatch: the nearest upstream component, '" + upstream.getComponentName()
                + "', declares its output type as '" + upstreamOutputType + "', which does not look like a "
                + expectedSimpleName + ". " + getComponentMeta().getName() + " expects its incoming payload to be a "
                + expectedInputType + " - if it isn't, the flow will fail at runtime with a ClassCastException.";
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", name='" + getComponentName() + '\'' +
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getIdentity()) + '\'' +
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
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getIdentity()) + '\'' +
                ", containingFlowRoute ='" + (containingFlowRoute == null ? null : containingFlowRoute.getRouteName()) + '\'' +
                '}';
    }
}
