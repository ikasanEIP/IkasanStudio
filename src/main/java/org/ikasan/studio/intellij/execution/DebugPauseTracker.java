package org.ikasan.studio.intellij.execution;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XSourcePosition;

import java.util.IdentityHashMap;
import java.util.Map;

/** Project-owned pause locations. Selected editor files and selected caller frames do not affect these. */
final class DebugPauseTracker {
    private static final Logger LOG = Logger.getInstance(DebugPauseTracker.class);
    private final Map<XDebugSession, String> locations = new IdentityHashMap<>();
    private final Runnable changed;
    private boolean disposed;
    private long pauseRevision;

    DebugPauseTracker(Runnable changed) {
        this.changed = changed;
    }

    synchronized void attach(XDebugSession session, Disposable parent) {
        if (disposed || locations.containsKey(session)) return;
        locations.put(session, null);
        session.addSessionListener(new XDebugSessionListener() {
            @Override public void sessionPaused() { update(session); }
            // A later frame notification can supply a source that was unavailable at suspension.
            // Always re-read the top frame, never the caller selected in the debugger UI.
            @Override public void stackFrameChanged() { update(session); }
            @Override public void beforeSessionResume() { clear(session, false); }
            @Override public void sessionResumed() { clear(session, false); }
            @Override public void sessionStopped() { clear(session, true); }
        }, parent);
        update(session);
    }

    private synchronized void update(XDebugSession session) {
        if (disposed || !locations.containsKey(session)) return;
        XSourcePosition position = session.isSuspended() ? session.getTopFramePosition() : null;
        String source = position == null ? null : position.getFile().getPath();
        String previous = locations.put(session, source);
        if (source != null && !java.util.Objects.equals(previous, source)) pauseRevision++;
        if (!java.util.Objects.equals(previous, source) || session.isSuspended() && source == null) {
            LOG.info("STUDIO: Debug pause source=" + source + " session=" + session.getSessionName());
        }
        changed.run();
    }

    synchronized void clear(XDebugSession session, boolean stopped) {
        if (disposed || !locations.containsKey(session)) return;
        if (stopped) locations.remove(session);
        else locations.put(session, null);
        changed.run();
    }

    synchronized boolean contains(String sourcePath) {
        // Session keys require identity, but source paths require text equality. IdentityHashMap's
        // containsValue would reject an equal path assembled separately by the canvas.
        return sourcePath != null && locations.values().stream().anyMatch(sourcePath::equals);
    }

    synchronized java.util.List<String> sourcePaths() {
        return locations.values().stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    synchronized long getPauseRevision() { return pauseRevision; }

    synchronized boolean hasPausedLocation() {
        return locations.values().stream().anyMatch(java.util.Objects::nonNull);
    }

    synchronized void dispose() {
        disposed = true;
        locations.clear();
    }
}
