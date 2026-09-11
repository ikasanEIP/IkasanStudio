package org.ikasan.studio.intellij.execution;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XSourcePosition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DebugPauseTrackerTest {
    private final Disposable parent = () -> {};
    private final Runnable repaint = mock(Runnable.class);
    private final DebugPauseTracker tracker = new DebugPauseTracker(repaint);

    private XDebugSession session(String path) {
        XDebugSession session = mock(XDebugSession.class);
        XSourcePosition position = mock(XSourcePosition.class);
        VirtualFile file = mock(VirtualFile.class);
        when(file.getPath()).thenReturn(path);
        when(position.getFile()).thenReturn(file);
        when(session.getTopFramePosition()).thenReturn(position);
        return session;
    }

    private XDebugSessionListener attach(XDebugSession session) {
        tracker.attach(session, parent);
        ArgumentCaptor<XDebugSessionListener> listener = ArgumentCaptor.forClass(XDebugSessionListener.class);
        verify(session).addSessionListener(listener.capture(), same(parent));
        return listener.getValue();
    }

    @Test
    void pauseHighlightsActualFrameAndResumeOrStepClearsIt() {
        XDebugSession session = session("/project/user/src/main/java/a/Debug.java");
        XDebugSessionListener listener = attach(session);
        assertThat(tracker.hasPausedLocation()).isFalse();
        when(session.isSuspended()).thenReturn(true);
        listener.sessionPaused();
        assertThat(tracker.contains("/project/user/src/main/java/a/Debug.java")).isTrue();
        assertThat(tracker.contains("/other/user/src/main/java/a/Debug.java")).isFalse();
        listener.stackFrameChanged();
        verify(session, never()).getCurrentPosition();
        assertThat(tracker.hasPausedLocation()).isTrue();
        listener.beforeSessionResume();
        assertThat(tracker.hasPausedLocation()).isFalse();
        listener.sessionResumed();
        listener.sessionPaused();
        assertThat(tracker.hasPausedLocation()).isTrue();
        listener.sessionStopped();
        assertThat(tracker.hasPausedLocation()).isFalse();
        verify(repaint, atLeast(4)).run();
    }

    @Test
    void canvasPathMatchesDebuggerPathByTextRatherThanStringIdentity() {
        String debuggerPath = new String("/project/user/src/main/java/a/Debug.java");
        String canvasPath = new String("/project/user/src/main/java/a/Debug.java");
        assertThat(canvasPath).isNotSameAs(debuggerPath);
        XDebugSession session = session(debuggerPath);
        when(session.isSuspended()).thenReturn(true);
        XDebugSessionListener listener = attach(session);
        assertThat(tracker.contains(canvasPath)).isTrue();
        assertThat(tracker.contains(canvasPath.replace("/project/", "/other/"))).isFalse();
        listener.beforeSessionResume();
        assertThat(tracker.contains(canvasPath)).isFalse();
    }

    @Test
    void existingPauseIsRecoveredAndIndependentSessionsRemainHighlighted() {
        XDebugSession first = session("/project/First.java");
        XDebugSession second = session("/project/Second.java");
        when(first.isSuspended()).thenReturn(true);
        when(second.isSuspended()).thenReturn(true);
        XDebugSessionListener firstListener = attach(first);
        attach(second);
        tracker.attach(first, parent);
        verify(first, times(1)).addSessionListener(any(), same(parent));
        firstListener.sessionStopped();
        assertThat(tracker.contains("/project/First.java")).isFalse();
        assertThat(tracker.contains("/project/Second.java")).isTrue();
    }

    @Test
    void sourceResolvedAfterPauseIsPickedUpWithoutFollowingSelectedCaller() {
        XDebugSession session = session("/project/Debug.java");
        XSourcePosition actualPosition = session.getTopFramePosition();
        when(session.isSuspended()).thenReturn(true);
        when(session.getTopFramePosition()).thenReturn(null);
        XDebugSessionListener listener = attach(session);
        assertThat(tracker.hasPausedLocation()).isFalse();
        when(session.getTopFramePosition()).thenReturn(actualPosition);
        listener.stackFrameChanged();
        assertThat(tracker.contains("/project/Debug.java")).isTrue();
        verify(session, never()).getCurrentPosition();
    }

    @Test
    void missingSourceAndLateCallbacksAfterDisposalCannotLeaveAHighlight() {
        XDebugSession session = session("/project/Debug.java");
        when(session.isSuspended()).thenReturn(true);
        when(session.getTopFramePosition()).thenReturn(null);
        XDebugSessionListener listener = attach(session);
        assertThat(tracker.hasPausedLocation()).isFalse();
        tracker.dispose();
        listener.sessionPaused();
        assertThat(tracker.hasPausedLocation()).isFalse();
        assertThat(tracker.contains(null)).isFalse();
    }
}
