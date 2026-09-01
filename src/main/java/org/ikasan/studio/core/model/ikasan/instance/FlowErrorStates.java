package org.ikasan.studio.core.model.ikasan.instance;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which flows (by name) are currently known to have stopped in error, for the canvas's flashing red
 * outline (see {@code DesignerCanvas#paintFlowErrorFlashes}) - fills the same "framework-independent state,
 * kept plain-JUnit testable" role for the flow-error-monitoring feature that {@link TestMailServerLinks} fills
 * for the Test Mail Server feature, just tracking live poll results rather than computing a static grouping.
 * -
 * Owned by {@code org.ikasan.studio.ui.intellij.FlowErrorMonitorService}, which is the only thing that ever
 * calls {@link #flag} / {@link #clear} (on each REST poll tick); the canvas only ever reads.
 */
public final class FlowErrorStates {
    /** @param summary a short human-readable description of the error (from the moduleControl REST error log), or null if unavailable. */
    public record ErrorInfo(String summary, long detectedAtMillis) {}

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
