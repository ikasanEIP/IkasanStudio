package org.ikasan.studio.core.model.command;

import org.ikasan.studio.core.model.ikasan.instance.*;

import java.util.ArrayList;
import java.util.List;

import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.ROUTE_NAMES;

/**
 * An atomic, reversible relocation of an existing flow element.  The element is never cloned, so its
 * identity, configured properties and user-code association survive a canvas move unchanged.
 */
public final class FlowElementMove {
    private FlowElementMove() { }

    public static MoveResult move(FlowElement element, Flow targetFlow, FlowRoute targetRoute, int targetIndex) {
        if (element == null || targetFlow == null || element.getContainingFlow() == null) {
            return MoveResult.rejected("The component is not attached to a flow.");
        }
        if (element.getComponentMeta().isInternalEndpoint()) {
            return MoveResult.rejected("Router endpoints cannot be moved independently.");
        }

        Location source = Location.capture(element);
        if (source == null) {
            return MoveResult.rejected("The component's current position could not be resolved.");
        }
        if (element.getComponentMeta().isExceptionResolver()) {
            targetRoute = null;
        } else if (!element.getComponentMeta().isConsumer() && targetRoute == null) {
            return MoveResult.rejected("Drop the component into a flow route.");
        }
        if (targetRoute != null && targetRoute.getFlow() != targetFlow) {
            return MoveResult.rejected("The target route does not belong to the target flow.");
        }
        if (element.getComponentMeta().isRouter() && targetRoute != null && isDescendant(source.routerBranches, targetRoute)) {
            return MoveResult.rejected("A router cannot be moved into one of its own branches.");
        }

        int requestedIndex = targetIndex;
        // Canvas insertion indices are measured against the still-intact route. Once the source is removed,
        // every destination to its right shifts left by one.
        if (source.route == targetRoute && source.index >= 0 && source.index < requestedIndex) {
            requestedIndex--;
        }
        source.detach(element);
        String issue = issueAtDestination(element, targetFlow, targetRoute, requestedIndex);
        if (!issue.isEmpty()) {
            source.attach(element);
            return MoveResult.rejected(issue);
        }

        Location destination = Location.destination(element, targetFlow, targetRoute, requestedIndex, source.routerBranches);
        destination.attach(element);
        if (source.samePositionAs(destination)) {
            return new MoveResult(true, false, source, destination, "");
        }
        return new MoveResult(true, true, source, destination, "");
    }

    private static String issueAtDestination(FlowElement element, Flow flow, FlowRoute route, int index) {
        String issue = flow.issueCausedByAdding(element.getComponentMeta(), route);
        if (route != null) {
            issue += route.issueCausedByAdding(element.getComponentMeta());
            List<FlowElement> elements = route.getFlowElements();
            int bounded = Math.max(0, Math.min(index, elements.size()));
            List<FlowElement> proposed = new ArrayList<>(elements);
            proposed.add(bounded, element);
            for (int i = 0; i < proposed.size(); i++) {
                FlowElement proposedElement = proposed.get(i);
                if ((proposedElement.getComponentMeta().isProducer() || proposedElement.getComponentMeta().isRouter())
                        && i != proposed.size() - 1) {
                    issue += proposedElement.getComponentMeta().isRouter()
                            ? "A router must remain the last component in its route. "
                            : "A producer must remain the last component in its route. ";
                    break;
                }
                if (proposedElement.getComponentMeta().isInternalEndpoint() && i != 0) {
                    issue += "A router endpoint must remain first in its route. ";
                    break;
                }
            }
        }
        return issue;
    }

    private static boolean isDescendant(List<FlowRoute> roots, FlowRoute candidate) {
        for (FlowRoute root : roots) {
            if (root == candidate || isDescendant(root.getChildRoutes(), candidate)) return true;
        }
        return false;
    }

    public static final class MoveResult {
        private final boolean accepted;
        private final boolean changed;
        private final Location source;
        private final Location destination;
        private final String issue;

        private MoveResult(boolean accepted, boolean changed, Location source, Location destination, String issue) {
            this.accepted = accepted;
            this.changed = changed;
            this.source = source;
            this.destination = destination;
            this.issue = issue;
        }

        private static MoveResult rejected(String issue) {
            return new MoveResult(false, false, null, null, issue);
        }

        public boolean accepted() { return accepted; }
        public boolean changed() { return changed; }
        public String issue() { return issue; }
        public void undo(FlowElement element) { if (changed) { destination.detach(element); source.attach(element); } }
        public void redo(FlowElement element) { if (changed) { source.detach(element); destination.attach(element); } }
    }

    private static final class Location {
        private final Flow flow;
        private final FlowRoute route;
        private final int index;
        private final boolean consumer;
        private final boolean exceptionResolver;
        private final List<FlowRoute> routerBranches;

        private Location(Flow flow, FlowRoute route, int index, boolean consumer, boolean exceptionResolver,
                         List<FlowRoute> routerBranches) {
            this.flow = flow;
            this.route = route;
            this.index = index;
            this.consumer = consumer;
            this.exceptionResolver = exceptionResolver;
            this.routerBranches = routerBranches;
        }

        private static Location capture(FlowElement element) {
            Flow flow = element.getContainingFlow();
            if (flow.getConsumer() == element) return new Location(flow, null, -1, true, false, List.of());
            if (flow.getExceptionResolver() == element) return new Location(flow, null, -1, false, true, List.of());
            FlowRoute route = element.getContainingFlowRoute();
            if (route == null || !route.getFlowElements().contains(element)) return null;
            List<FlowRoute> branches = new ArrayList<>();
            if (element.getComponentMeta().isRouter() && element.getPropertyValue(ROUTE_NAMES) instanceof List<?> names) {
                for (Object name : names) {
                    if (name instanceof String routeName) {
                        // A router owns the direct child routes created for its routeNames. A recursive
                        // lookup could accidentally capture a same-named branch belonging to a nested router.
                        route.getChildRoutes().stream()
                                .filter(child -> routeName.equals(child.getRouteName()))
                                .findFirst()
                                .ifPresent(branches::add);
                    }
                }
            }
            return new Location(flow, route, route.getFlowElements().indexOf(element), false, false, branches);
        }

        private static Location destination(FlowElement element, Flow flow, FlowRoute route, int index,
                                            List<FlowRoute> branches) {
            if (element.getComponentMeta().isConsumer()) return new Location(flow, null, -1, true, false, List.of());
            if (element.getComponentMeta().isExceptionResolver()) return new Location(flow, null, -1, false, true, List.of());
            return new Location(flow, route, Math.max(0, Math.min(index, route.getFlowElements().size())),
                    false, false, branches);
        }

        private void detach(FlowElement element) {
            if (consumer) flow.setConsumer(null);
            else if (exceptionResolver) flow.setExceptionResolver(null);
            else {
                route.getFlowElements().remove(element);
                route.getChildRoutes().removeAll(routerBranches);
            }
        }

        private void attach(FlowElement element) {
            element.setContainingFlow(flow);
            if (consumer) {
                element.setContainingFlowRoute(flow.getFlowRoute());
                flow.setConsumer(element);
            } else if (exceptionResolver) {
                element.setContainingFlowRoute(null);
                flow.setExceptionResolver((ExceptionResolver) element);
            } else {
                element.setContainingFlowRoute(route);
                route.getFlowElements().add(Math.max(0, Math.min(index, route.getFlowElements().size())), element);
                route.getChildRoutes().addAll(routerBranches);
                for (FlowRoute branch : routerBranches) updateFlow(branch, flow);
            }
        }

        private static void updateFlow(FlowRoute route, Flow flow) {
            route.setFlow(flow);
            for (FlowElement child : route.getFlowElements()) {
                child.setContainingFlow(flow);
                child.setContainingFlowRoute(route);
            }
            for (FlowRoute childRoute : route.getChildRoutes()) updateFlow(childRoute, flow);
        }

        private boolean samePositionAs(Location other) {
            return flow == other.flow && route == other.route && index == other.index
                    && consumer == other.consumer && exceptionResolver == other.exceptionResolver;
        }
    }
}
