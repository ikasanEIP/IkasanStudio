package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import org.ikasan.studio.core.model.ikasan.instance.FlowErrorStates;
import org.ikasan.studio.core.model.ikasan.instance.FlowRuntimeStatuses;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.ModuleControlClient;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.util.Map;

/**
 * Polls the running module's own Ikasan REST interface (moduleControl) for each flow's state, and flags any flow
 * that has transitioned into "stoppedInError" (as opposed to a clean "stopped") for the canvas to flash red - see
 * {@code DesignerCanvas#paintFlowErrorFlashes}. This mirrors {@link TestMailServerSessionService}'s own
 * Alarm-based polling shape, but polls a real HTTP endpoint rather than a bare TCP probe, so it costs a little
 * more per tick and stays off entirely when {@link IkasanStudioSettings#isFlowErrorMonitoringEnabled()} is false.
 * -
 * Ikasan's rest-module (moduleControl/error endpoints) is pulled in transitively by ikasan-eip-standalone, which
 * every Studio-generated module already depends on unconditionally - so this works out of the box, with no pom
 * changes needed, for any module Studio has ever generated. A module simply not running yet (or any other
 * connectivity failure) shows up here as an unreachable endpoint - handled the same as "nothing to report" (see
 * {@link #pollAndUpdateState}), not logged above info, since that's the overwhelmingly common case between
 * debug sessions.
 * -
 * Also tracks every flow's raw status (not just the error flag) in {@link #flowStatuses}, for the canvas's
 * per-flow status label and transport-control button enablement - see {@code DesignerCanvas#paintFlowTransportControls}.
 */
@Service(Service.Level.PROJECT)
public final class FlowErrorMonitorService implements Disposable {
    private static final Logger LOG = Logger.getInstance(FlowErrorMonitorService.class);
    private static final String STOPPED_IN_ERROR_STATE = "stoppedInError";
    // A REST round-trip is heavier than TestMailServerSessionService's bare TCP probe, so this ticks a little
    // slower than that service's 4s - still prompt enough that a flow going red is noticed within a few seconds.
    private static final int POLL_INTERVAL_MS = 6000;

    private final Project project;
    private final Alarm alarm;
    private final FlowErrorStates errorStates = new FlowErrorStates();
    private final FlowRuntimeStatuses flowStatuses = new FlowRuntimeStatuses();

    public FlowErrorMonitorService(Project project) {
        this.project = project;
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        scheduleNextPoll();
    }

    /** @return the live, poll-maintained set of flows currently flagged as stopped in error - read by DesignerCanvas during paint. */
    public FlowErrorStates getErrorStates() {
        return errorStates;
    }

    /** @return every flow's last-polled raw state - read by DesignerCanvas for the per-flow status label/button enablement. */
    public FlowRuntimeStatuses getFlowStatuses() {
        return flowStatuses;
    }

    /**
     * Polls immediately, bypassing the regular ~6s schedule and {@code IkasanStudioSettings#isFlowErrorMonitoringEnabled()}
     * - called right after {@code FlowTransportControlAction} changes a flow's state, so the canvas reflects the
     * user's own click straight away rather than waiting for the next scheduled tick (or, with background
     * monitoring switched off, not at all). Safe to call from any thread.
     */
    public void pollNow() {
        pollAndUpdateState();
    }

    /**
     * Drops states obtained from the module process that has just terminated. Without this reset the canvas
     * continues to paint the per-flow controls and the final status returned by that no-longer-live process.
     */
    public void clearRuntimeStatuses() {
        if (flowStatuses.clear()) {
            repaintCanvas();
        }
    }

    private void scheduleNextPoll() {
        if (!alarm.isDisposed()) {
            alarm.addRequest(this::pollAndReschedule, POLL_INTERVAL_MS);
        }
    }

    private void pollAndReschedule() {
        if (IkasanStudioSettings.isFlowErrorMonitoringEnabled()) {
            pollAndUpdateState();
        }
        scheduleNextPoll();
    }

    private void pollAndUpdateState() {
        Module ikasanModule = project.isDisposed() ? null : project.getService(UiContext.class).getIkasanModule();
        if (ikasanModule == null || ikasanModule.getIdentity() == null || ikasanModule.getPort() == null) {
            return;
        }
        Map<String, String> flowStates;
        try {
            flowStates = ModuleControlClient.fetchFlowStates(ikasanModule);
        } catch (Exception e) {
            // Expected whenever the module simply isn't running (or hasn't started listening yet) - not worth
            // more than a debug-level trace, per CLAUDE.md's "never log above warn" rule for IntelliJ's logger.
            LOG.debug("STUDIO: flow error monitor could not reach moduleControl REST endpoint: " + e);
            return;
        }
        boolean changed = false;
        for (Map.Entry<String, String> entry : flowStates.entrySet()) {
            String flowName = entry.getKey();
            if (flowStatuses.update(flowName, entry.getValue())) {
                changed = true;
            }
            boolean nowInError = STOPPED_IN_ERROR_STATE.equals(entry.getValue());
            if (nowInError) {
                if (!errorStates.isFlagged(flowName)) {
                    String summary = ModuleControlClient.fetchLatestErrorSummary(ikasanModule, flowName);
                    errorStates.flag(flowName, new FlowErrorStates.ErrorInfo(summary, System.currentTimeMillis()));
                    changed = true;
                    notifyNewError(flowName, summary);
                }
            } else if (errorStates.clear(flowName)) {
                changed = true;
            }
        }
        if (changed) {
            repaintCanvas();
        }
    }

    private void notifyNewError(String flowName, String summary) {
        String message = "Flow '" + flowName + "' has stopped in error" + (summary != null ? ": " + summary : ".");
        Runnable show = () -> {
            if (!project.isDisposed()) {
                StudioUIUtils.displayIdeaErrorMessage(project, message);
            }
        };
        runOnEdt(show);
    }

    private void repaintCanvas() {
        Runnable repaint = () -> {
            if (!project.isDisposed()) {
                DesignerCanvas canvas = project.getService(UiContext.class).getDesignerCanvas();
                if (canvas != null) {
                    canvas.repaint();
                }
            }
        };
        runOnEdt(repaint);
    }

    private void runOnEdt(Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

    @Override
    public void dispose() {
    }
}
