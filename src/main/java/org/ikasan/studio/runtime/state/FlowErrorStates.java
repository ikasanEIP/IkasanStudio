package org.ikasan.studio.runtime.state;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which flows (by name) are currently known to have stopped in error, for the canvas's flashing red
 * outline (see {@code DesignerCanvas#paintFlowErrorFlashes}) - fills the same "framework-independent state,
 * kept plain-JUnit testable" role for the flow-error-monitoring feature that {@link org.ikasan.studio.core.model.analysis.TestMailServerLinks} fills
 * for the Test Mail Server feature, just tracking live poll results rather than computing a static grouping.
 * -
 * Owned by {@code org.ikasan.studio.intellij.runtime.FlowErrorMonitorService}, which is the only thing that ever
 * calls {@link #flag} / {@link #clear} (on each REST poll tick); the canvas only ever reads.
 */
public final class FlowErrorStates {
    /** Holds both the concise hover summary and the full selectable/copyable diagnostic report. */
    public record ErrorInfo(String summary, String details, long detectedAtMillis) {
        public ErrorInfo(String summary, long detectedAtMillis) {
            this(summary, null, detectedAtMillis);
        }
    }

    private final Map<String, ErrorInfo> flaggedFlows = new ConcurrentHashMap<>();

    /** @return true if this flow name wasn't already flagged - i.e. this is a newly-detected error, not one already known about. */
    public boolean flag(String flowName, ErrorInfo info) {
        return flaggedFlows.put(flowName, info) == null;
    }

    /** @return true if this flow name WAS flagged and has now been cleared - i.e. state actually changed (a no-op clear returns false). */
    public boolean clear(String flowName) {
        return flaggedFlows.remove(flowName) != null;
    }

    public boolean isFlagged(String flowName) {
        return flaggedFlows.containsKey(flowName);
    }

    public ErrorInfo getError(String flowName) {
        return flaggedFlows.get(flowName);
    }

    public Set<String> flaggedFlowNames() {
        return flaggedFlows.keySet();
    }

    public boolean hasAnyFlagged() {
        return !flaggedFlows.isEmpty();
    }
}
