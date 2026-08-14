package org.ikasan.studio.core.model.ikasan.instance;

import java.util.List;

/**
 * Captures exactly what {@link Flow#removeFlowElement} / {@link FlowRoute#removeFlowElement} removed, and
 * from where, so the removal can be precisely reversed. Neither removal touches any field on the removed
 * objects themselves (they're only unlinked from a parent list, or nulled off {@code Flow}), so putting the
 * same instances back where {@code elementIndex}/{@code ChildRouteRemoval.index} say they came from fully
 * restores the prior structure.
 */
public record FlowElementRemoval(
        FlowElement removedElement,
        Flow owningFlow,
        FlowRoute sourceRoute,      // null when removedElement was the flow's consumer or exceptionResolver
        int elementIndex,           // index within sourceRoute.getFlowElements() at removal time; -1 if not applicable
        boolean wasConsumer,
        boolean wasExceptionResolver,
        List<ChildRouteRemoval> removedChildRoutes  // non-empty only when removing a router also removed matched child routes
) {
    public record ChildRouteRemoval(FlowRoute parentRoute, FlowRoute removedRoute, int index) {}

    /**
     * Reverses this removal, putting the element (and any cascaded child routes, for a removed router) back
     * exactly where they were.
     */
    public void undo() {
        if (wasConsumer) {
            owningFlow.setConsumer(removedElement);
        } else if (wasExceptionResolver) {
            owningFlow.setExceptionResolver((ExceptionResolver) removedElement);
        } else if (sourceRoute != null) {
            List<FlowElement> elements = sourceRoute.getFlowElements();
            int insertAt = Math.min(Math.max(elementIndex, 0), elements.size());
            elements.add(insertAt, removedElement);
            for (ChildRouteRemoval childRouteRemoval : removedChildRoutes) {
                List<FlowRoute> childRoutes = childRouteRemoval.parentRoute().getChildRoutes();
                int childInsertAt = Math.min(Math.max(childRouteRemoval.index(), 0), childRoutes.size());
                childRoutes.add(childInsertAt, childRouteRemoval.removedRoute());
            }
        }
    }

    /**
     * Re-applies this removal (used for redo).
     */
    public void redo() {
        if (wasConsumer) {
            owningFlow.setConsumer(null);
        } else if (wasExceptionResolver) {
            owningFlow.setExceptionResolver(null);
        } else if (sourceRoute != null) {
            sourceRoute.getFlowElements().remove(removedElement);
            for (ChildRouteRemoval childRouteRemoval : removedChildRoutes) {
                childRouteRemoval.parentRoute().getChildRoutes().remove(childRouteRemoval.removedRoute());
            }
        }
    }
}
