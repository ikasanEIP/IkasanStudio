package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
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
        ComponentMeta newComponentMeta = ComponentLibrary.getIkasanComponentByKey(metapackVersion, this.getComponentMeta().getName());
        if (newComponentMeta == null) {
            throw new StudioBuildException("Component [" + this.getComponentMeta().getName() + "] not found in metapack version [" + metapackVersion + "]");
        }
        FlowElement clonedFlowElement = new FlowElement(newComponentMeta, containingFlow, containingFlowRoute, this.getComponentName(), this.getDecorators());
        super.cloneToVersion(clonedFlowElement);
        return clonedFlowElement;
    }

    // The full-event escape hatch documented in converterTemplate_en.ftl / brokerTemplate_en.ftl / splitterTemplate_en.ftl -
    // a component deliberately set to receive org.ikasan.spec.flow.FlowEvent gets the whole event, not the upstream's
    // declared payload type, so that is never a real mismatch. Object means "accepts/declares anything" (e.g. a
    // Debug breakpoint's fixed, hidden fromType), so it can never conflict with anything either. Applied
    // symmetrically to both sides of the comparison - this component's expected input and the upstream's declared
    // output - and compared against as a simple (unqualified) type name.
    private static final String FLOW_EVENT_SIMPLE_NAME = "FlowEvent";
    private static final String OBJECT_SIMPLE_NAME = "Object";
    // A marker interface, not a concrete type - substring name-matching (see the per-candidate loop below) is
    // structurally the wrong tool for it, since an implementing class has no reason to have "Serializable"
    // anywhere in its own name (e.g. Ikasan's real org.ikasan.filetransfer.component.DefaultPayload implements
    // Payload, not Serializable, and neither name suggests the other). Real resolution needs PSI, which this
    // class must never depend on - see the Function<String, Boolean> overload below.
    private static final String SERIALIZABLE_TYPE = "java.io.Serializable";

    /**
     * Best-effort design-time check: if this component has an effective expected input type (see
     * {@link #getEffectiveInputTypeDescription()} - one type, or several comma-separated candidates, e.g. Basic
     * AMQ JMS Producer's "java.lang.String, byte[], java.util.Map, java.io.Serializable", Spring JmsTemplate's
     * actual accepted set) and the nearest upstream element whose declared 'toType' represents the actual
     * incoming payload (see {@link Flow#findPayloadSourceElement}) has a type that clearly satisfies none of
     * them, returns a warning message suitable for display. Returns null whenever there isn't enough
     * information to check - including the very common case of an upstream Consumer, which never declares an
     * output type in Studio's metadata - since staying silent is safer than a false alarm; every candidate but
     * one is a textual heuristic, not real type resolution, so a genuine mismatch can still slip through
     * unflagged.
     * -
     * The one exception is the literal candidate "java.io.Serializable" - a marker interface, where name
     * matching is structurally the wrong tool (see {@link #SERIALIZABLE_TYPE}'s own comment). This no-arg
     * overload can never confirm or deny it (this class must never depend on PSI - see
     * {@link #getUpstreamTypeMismatchWarning(Function)} for the real check, wired up by a UI-layer caller that
     * does have PSI access), so a Serializable candidate here only ever suppresses a warning it can't rule out,
     * never causes one on its own.
     * @return a human-readable warning, or null if nothing is wrong (or nothing could be checked)
     */
    public String getUpstreamTypeMismatchWarning() {
        return getUpstreamTypeMismatchWarning(upstreamType -> null);
    }

    /**
     * The real, PSI-backed variant of {@link #getUpstreamTypeMismatchWarning()} - see there for the full rules.
     * This class lives in the framework-independent core layer (see LayerBoundaryTest/ArchitectureBoundaryTest,
     * which forbid it depending on com.intellij.* or UI packages directly), so it cannot resolve "does this class
     * implement Serializable" itself; instead the caller (a UI-layer class with real PSI access) supplies that
     * one answer as a function.
     * @param serializableChecker given the upstream's declared output type, returns TRUE if it's confirmed to
     * implement java.io.Serializable, FALSE if confirmed not to, or null if that can't be determined (unresolvable
     * class, or no real PSI available - the no-arg overload always supplies null here)
     * @return a human-readable warning, or null if nothing is wrong (or nothing could be checked)
     */
    public String getUpstreamTypeMismatchWarning(Function<String, Boolean> serializableChecker) {
        if (getComponentMeta() == null || containingFlow == null) {
            return null;
        }
        String expectedInputTypes = getEffectiveInputTypeDescription();
        if (expectedInputTypes == null || expectedInputTypes.isBlank()) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (String rawCandidate : expectedInputTypes.split(",")) {
            String candidate = rawCandidate.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            String candidateSimpleName = candidate.substring(candidate.lastIndexOf('.') + 1);
            if (OBJECT_SIMPLE_NAME.equals(candidateSimpleName) || FLOW_EVENT_SIMPLE_NAME.equals(candidateSimpleName)) {
                // This component declares it accepts anything - nothing to check, regardless of how many other
                // candidates are also listed alongside it.
                return null;
            }
            candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        FlowElement upstream = containingFlow.findPayloadSourceElement(this);
        if (upstream == null) {
            return null;
        }
        String upstreamOutputType = upstream.getEffectiveOutputTypeDescription();
        if (upstreamOutputType == null || upstreamOutputType.isBlank()) {
            return null;
        }
        String upstreamSimpleName = upstreamOutputType.substring(upstreamOutputType.lastIndexOf('.') + 1);
        if (OBJECT_SIMPLE_NAME.equals(upstreamSimpleName) || FLOW_EVENT_SIMPLE_NAME.equals(upstreamSimpleName)) {
            return null;
        }

        boolean serializableUnresolved = false;
        for (String candidate : candidates) {
            if (SERIALIZABLE_TYPE.equals(candidate)) {
                Boolean isSerializable = serializableChecker.apply(upstreamOutputType);
                if (Boolean.TRUE.equals(isSerializable)) {
                    return null;
                }
                if (isSerializable == null) {
                    serializableUnresolved = true;
                }
                continue;
            }
            String candidateSimpleName = candidate.substring(candidate.lastIndexOf('.') + 1);
            if (upstreamOutputType.toLowerCase().contains(candidateSimpleName.toLowerCase())) {
                return null;
            }
        }
        if (serializableUnresolved) {
            // Every other candidate definitively failed, but Serializable itself couldn't be confirmed or
            // denied - staying silent here (rather than warning) is the same "silence over false alarm"
            // philosophy this method already applies everywhere else it lacks enough information.
            return null;
        }
        String expectedDescription = candidates.size() == 1 ? candidates.get(0) : "one of: " + String.join(", ", candidates);
        return "Possible type mismatch: the nearest upstream component, '" + upstream.getComponentName()
                + "', declares its output type as '" + upstreamOutputType + "', which does not look like it satisfies "
                + expectedDescription + ". " + getComponentMeta().getName() + " expects its incoming payload to be "
                + expectedDescription + " - if it isn't, the flow may fail at runtime.\n"
                + "Suggested Fix: add a Converter before '" + getComponentMeta().getName() + "' to convert '"
                + upstreamOutputType + "' to " + expectedDescription + ".";
    }

    /**
     * The type this component itself expects its incoming payload to be, or null if it doesn't declare one:
     * a fixed metadata constant when the component has no user-configurable input type of its own (e.g.
     * Default List Splitter, JMS Object Message To Object Converter - see {@link ComponentMeta#getExpectedInputTypes()}),
     * otherwise the current value of whichever of this component's own properties represents that (see
     * {@link ComponentMeta#getEffectiveInputTypePropertyName()}) if that property is currently set. Consumers
     * always return null here - they start the flow, nothing flows into them.
     * -
     * This just supplies live property values to {@link ComponentMeta#getEffectiveInputTypeDescription} - see
     * there for the actual rules, and for the metadata-only variant used where there's no FlowElement instance
     * yet (e.g. a palette item, see IkasanPaletteElementViewHandler).
     * @return a display-ready type description, or null if this component declares no input type (Consumers,
     * or any component whose declared input-type property is currently unset)
     */
    public String getEffectiveInputTypeDescription() {
        if (getComponentMeta() == null) {
            return null;
        }
        return getComponentMeta().getEffectiveInputTypeDescription(this::getPropertyValueAsString);
    }

    /**
     * The type this component hands to whatever comes after it, or null if that can't be stated from metadata
     * alone - see {@link ComponentMeta#getEffectiveOutputTypeDescription} for the actual rules (Producers are
     * terminal, Routers/Filters/Translators/Debug pass the payload through unchanged, everything else is its
     * own 'toType' property or a fixed {@link ComponentMeta#getProducedOutputType()}). This just supplies live
     * property values to that shared engine.
     * @return a display-ready type description, or null if nothing can be said
     */
    public String getEffectiveOutputTypeDescription() {
        if (getComponentMeta() == null) {
            return null;
        }
        return getComponentMeta().getEffectiveOutputTypeDescription(this::getPropertyValueAsString);
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
