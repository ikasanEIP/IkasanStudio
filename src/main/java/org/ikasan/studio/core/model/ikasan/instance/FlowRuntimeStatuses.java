package org.ikasan.studio.core.model.ikasan.instance;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks each flow's most recently polled raw Ikasan state (see org.ikasan.spec.flow.Flow's RUNNING/STOPPED/
 * PAUSED/STOPPED_IN_ERROR/RECOVERING constants) - backs the canvas's per-flow status label and transport-control
 * button enablement (see {@code DesignerCanvas#paintFlowTransportControls}). Fills the same
 * "framework-independent, plain-JUnit-testable state" role {@link FlowErrorStates} fills for the error-flash
 * feature - kept as a separate class since FlowErrorStates' whole contract is "is this flow newly/no-longer in
 * error" (transition semantics), whereas this is just a simple last-known-value cache with no semantics of its
 * own beyond "what did the last poll say".
 * -
 * Owned by {@code org.ikasan.studio.ui.intellij.FlowErrorMonitorService}, which is the only thing that ever
 * calls {@link #update}; the canvas only ever reads.
 */
public final class FlowRuntimeStatuses {
    private final ConcurrentMap<String, String> rawStateByFlow = new ConcurrentHashMap<>();

    /** @return true if this flow's raw state actually changed (or is newly known) since the last update - a no-op update returns false. */
    public boolean update(String flowName, String rawState) {
        String previous = rawState == null ? rawStateByFlow.remove(flowName) : rawStateByFlow.put(flowName, rawState);
        return !Objects.equals(previous, rawState);
    }

    public String getRawState(String flowName) {
        return rawStateByFlow.get(flowName);
    }

    /** Clears every last-known runtime state when the module process is no longer running. */
    public boolean clear() {
        if (rawStateByFlow.isEmpty()) {
            return false;
        }
        rawStateByFlow.clear();
        return true;
    }
}
