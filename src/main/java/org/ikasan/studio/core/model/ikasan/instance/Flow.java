package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowSerializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@JsonSerialize(using = FlowSerializer.class)
public class Flow extends BasicElement {
    private static final Logger LOG = LoggerFactory.getLogger(BasicElement.class);
    // The fields of a Flow will need to be known for serialisation
    public static final String CONSUMER_JSON_TAG = "consumer";
    public static final String TRANSITIONS_JSON_TAG = "transitions";
    public static final String FLOW_ELEMENTS_JSON_TAG = "flowElements";
    public static final String EXCEPTION_RESOLVER_JSON_TAG = "exceptionResolver";

    private FlowElement consumer;
    private FlowRoute flowRoute;
    private ExceptionResolver exceptionResolver;

    /**
     * Used primarily during deserialization.
     */
    public Flow() throws StudioBuildException {
        super (IkasanComponentLibrary.getFLowComponentMeta(IkasanComponentLibrary.DEFAULT_IKASAN_PACK), null);
        LOG.error("STUDIO: Parameterless version of flow called");
    }

    public Flow(String metapackVersion) throws StudioBuildException {
        super (IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        flowRoute = FlowRoute.flowRouteBuilder().flow(this).build();
    }

    // The Builders are used to create test fixtures and occasionally, partially constructed Flows
    // Only the core properties are set in the constructor in order to allow loose coupling between the
    // IDE integration and the underlying version of Ikasan
    @Builder(builderMethodName = "flowBuilder")
    public Flow(
                @NonNull
                String metapackVersion,
                FlowElement consumer,
                FlowRoute flowRoute,
                ExceptionResolver exceptionResolver,
                String name,
                String description) throws StudioBuildException {
        super(IkasanComponentLibrary.getFLowComponentMeta(metapackVersion), null);
        if (consumer != null) {
            if (!consumer.getComponentMeta().isConsumer()) {
                LOG.error("STUDIO: ERROR : Tried to set consumer on " + this + " with a flowElement that is not a consumer " + consumer + ", this will be ignored");
            } else {
                this.consumer = consumer;
            }
        }
        if (flowRoute != null) {
            this.flowRoute = flowRoute;
        } else {
            this.flowRoute = FlowRoute.flowRouteBuilder().flow(this).build();
        }
        this.exceptionResolver = exceptionResolver;
        super.setName(name);
        super.setDescription(description);
    }

    public void removeFlowElement(FlowElement ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null) {
            if (ikasanFlowComponentToBeRemoved.getComponentMeta().isConsumer()) {
                setConsumer(null);
            } else if (ikasanFlowComponentToBeRemoved.getComponentMeta().isExceptionResolver()) {
                setExceptionResolver(null);
            } else if (flowRoute != null) {
                ikasanFlowComponentToBeRemoved.getContainingFlowRoute().removeFlowElement(ikasanFlowComponentToBeRemoved);
            } else {
                LOG.warn("STUDIO: Attempt to remove element " + ikasanFlowComponentToBeRemoved + " because it could not be found in the memory model");
            }
        }
    }

    public boolean hasConsumer() {
        return getConsumer() != null;
    }

    public boolean hasAnyComponents() {
        return
                hasExceptionResolver() || hasConsumer() || hasFlowComponents();
    }

    public boolean hasFlowComponents() {
        return ! flowRoute.isEmpty() || flowRoute.isEmpty();
    }

    /**
     * Does the Flow have a valid exception resolver
     * @return if the flow has a valid exception resolver.
     */
    public boolean hasExceptionResolver() {
        return (exceptionResolver != null);
    }

    public boolean anyFlowRouteHasComponents(FlowRoute flowRoute) {
        if (flowRoute != null) {
            if (flowRoute.getFlowElements() != null && !flowRoute.getFlowElements().isEmpty()) {
                return true;
            }
        }
        if (flowRoute != null && flowRoute.getChildRoutes() != null) {
            for(FlowRoute childRoute : flowRoute.getChildRoutes()) {
                if (anyFlowRouteHasComponents(childRoute)) {
                    return true;
                }
            }
        }
        return false;
    }

    public FlowRoute getFlowRouteContaining(FlowRoute searchRoute, FlowElement targetFlowElement) {
        if (searchRoute != null) {
            if (searchRoute.getFlowElements() != null && !searchRoute.getFlowElements().isEmpty() && searchRoute.getFlowElements().contains(targetFlowElement)) {
                return searchRoute;
            }
        }
        if (searchRoute != null && searchRoute.getChildRoutes() != null) {
            for(FlowRoute childRoute : searchRoute.getChildRoutes()) {
                if (getFlowRouteContaining(childRoute, targetFlowElement) != null) {
                    return childRoute;
                }
            }
        }
        return null;
    }

    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer
     */
    public List<FlowElement> ftlGetConsumerAndFlowElements() {

        List<FlowElement> allElements = new ArrayList<>();
        // Only the default (primary) flowRoute includes the consumer
        if (getConsumer() != null) {
            allElements.add(getConsumer());
        }
        getAllFlowElementsInAnyRoute(allElements, getFlowRoute());
        return allElements;
    }

    public List<FlowElement> getAllFlowElementsInAnyRoute(List<FlowElement> flowElementsList, FlowRoute flowRoute) {
        if (flowRoute != null) {
            List<FlowElement> thisRouteFlowElementList = flowRoute.getFlowElements();
            if (thisRouteFlowElementList != null && !thisRouteFlowElementList.isEmpty()) {
                flowElementsList.addAll(thisRouteFlowElementList);
            }
        }
        if (flowRoute != null && flowRoute.getChildRoutes() != null) {
            for(FlowRoute childRoute : flowRoute.getChildRoutes()) {
                getAllFlowElementsInAnyRoute(flowElementsList, childRoute);
            }
        }
        return flowElementsList;
    }

    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer, excluding any endpoints
     */
    public List<FlowElement> ftlGetAllFlowElementsInAnyRouteNoEndpoints() {

        List<FlowElement> allElements = ftlGetConsumerAndFlowElements();
        allElements = allElements.stream()
            .filter(flowElement -> !flowElement.componentMeta.isEndpoint())
            .toList();
        return allElements;
    }

    /**
     * This method is used by FreeMarker, the IDE may incorrectly identify it as unused.
     * @return A list of all non-null flow elements, including the consumer, excluding any endpoints
     */
    public List<FlowElement> getFlowElementsNoExternalEndPoints() {

        List<FlowElement> allElements = ftlGetConsumerAndFlowElements();
        allElements = allElements.stream()
            .filter(x-> ! x.componentMeta.isEndpoint() || x.componentMeta.isInternalEndpoint())
            .toList();
        return allElements;
    }

    /**
     * Determine the current state of the flow for completeness
     * @return A status string
     */
    @JsonIgnore
    public String getFlowIntegrityStatus() {
        String status = "";
        if (! hasConsumer()) {
            status += "The flow needs a consumer";
        }
        if (flowRoute != null) {
            status += flowRoute.getFlowIntegrityStatus();
        }
        if (!status.isEmpty()) {
            status += " to be complete.";
        }
        return status;
    }

    /**
     * If the component can be added to the flow, return an empty string otherwise state the reason why
     * @param newComponent to be added
     * @return reason why the component can not be added or empty string if there is no problem.
     */
    public String issueCausedByAdding(ComponentMeta newComponent, FlowRoute targetRoute) {
        String reason = "";
        if (newComponent.isFlow()) {
            reason += "You can add a flow to a module but not inside another flow. ";
        } else if (getConsumer() != null && newComponent.isConsumer()) {
            reason += "The flow cannot have more then one consumer. ";
        } else if (getExceptionResolver() != null && newComponent.isExceptionResolver()) {
            reason += "The flow cannot have more then one exception resolver. ";
        } else if (targetRoute == null && flowRoute != null && !flowRoute.isEmpty()) {
            reason += "Please drop into route. ";
        }
        return reason;
    }

    /**
     * The intent is to clone the existing flow but to a different meta-pack metapackVersion.
     * @param metapackVersion for cloned flow
     * @return the cloned module with the new meta pack version
     * @throws StudioBuildException when cloning is not possible.
     */
    public Flow cloneToVersion(String metapackVersion) throws StudioBuildException {
        if (metapackVersion == null || metapackVersion.isBlank()) {
            LOG.error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
            return null;
        }
        Flow clonedFlow = new Flow(metapackVersion);

        if (this.flowRoute != null) {
            clonedFlow.setFlowRoute(this.flowRoute.cloneToVersion(metapackVersion, clonedFlow));
        } else {
            clonedFlow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(clonedFlow).build());
        }
        if (this.getConsumer() != null) {
            clonedFlow.setConsumer(this.getConsumer().cloneToVersion(metapackVersion, clonedFlow, clonedFlow.getFlowRoute()));
        }
        if (this.getExceptionResolver() != null) {
            clonedFlow.setExceptionResolver(this.getExceptionResolver().cloneToVersion(metapackVersion, clonedFlow));
        }
        return clonedFlow;
    }

    @Override
    public String toSimpleString() {

        return getIdentity() + "[" + super.toSimpleString() +
                " consumer=" + (consumer!=null ? consumer.toSimpleString() : null) + "\n" +
                ", exceptionResolver=" + (exceptionResolver!=null ? exceptionResolver.toSimpleString() : null) + "\n" +
                ", flowRoute [" + (flowRoute!=null ? flowRoute.toSimpleString() : null) + "]" + "\n" +
                ']';
    }
}
