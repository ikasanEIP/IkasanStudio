package org.ikasan.studio.ui.actions;

import com.intellij.ui.JBColor;
import org.ikasan.studio.integration.ikasan.FlowControlOperation;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import java.awt.Color;

/**
 * The 4 flow transport-control actions Ikasan's moduleControl REST endpoint accepts (see
 * {@code ModuleControlClient#changeFlowState}) - mirrors the same start/stop/pause/start-paused controls
 * available from IntelliJ's own Run/Debug console, exposed instead as small buttons to the left of each flow on
 * the canvas (see {@code DesignerCanvas#paintFlowTransportControls}), so the console never has to be opened just
 * to control or check on a single flow.
 */
public enum FlowTransportAction {
    START("Start flow"),
    STOP("Stop flow"),
    PAUSE("Pause flow"),
    START_PAUSE("Start flow, paused");

    private final String tooltip;

    FlowTransportAction(String tooltip) {
        this.tooltip = tooltip;
    }


    /**
     * The actual wire action to send for this button given the flow's current raw state. Only START is
     * context-sensitive: ModuleControlApplication distinguishes "start" (a stopped/erroring flow) from a 5th
     * action, "resume" (moduleService.resumeFlow, distinct from startFlow), for bringing a *paused* flow back to
     * running - calling "start" on a paused flow is not the transition the user asked for. Rather than add a
     * genuine 5th button beyond the 4 icons the user specified, the START button (already the only one enabled
     * from "paused" alongside STOP - see {@link #isEnabledFor}) sends "resume" in that one case, since visually
     * it's the same "make it go" action the user expects the play-triangle icon to perform.
     */
    public FlowControlOperation resolveOperation(String rawState) {
        if (this == START && "paused".equals(rawState)) {
            return FlowControlOperation.RESUME;
        }
        return switch (this) {
            case START -> FlowControlOperation.START;
            case STOP -> FlowControlOperation.STOP;
            case PAUSE -> FlowControlOperation.PAUSE;
            case START_PAUSE -> FlowControlOperation.START_PAUSE;
        };
    }

    public String getTooltip() {
        return tooltip;
    }

    // Fixed, theme-independent red/orange - deliberately NOT ThemeAwareColors.getUrgentColor()/getWarningColor().
    // Both of those try generic "Component.borderColor"/"Separator.separatorColor" UIManager keys before their
    // own semantic fallback, and in at least one real theme that resolved to a muted, near-grey border colour -
    // the Stop/Pause/Start-Paused buttons all read as "disabled grey" even while genuinely enabled, the same
    // class of bug the blue Start button had before ThemeAwareColors#getSuccessColor() was tightened. A small
    // transport-control icon needs to read as unambiguously red/orange/green regardless of the active theme's
    // accent colour, so these bypass UIManager lookup entirely.
    private static final Color STOP_RED = new JBColor(new Color(211, 47, 47), new Color(255, 107, 107));
    private static final Color PAUSE_ORANGE = new JBColor(new Color(230, 126, 14), new Color(255, 170, 60));

    /** The fixed red used for the Stop button, reused by {@code DesignerCanvas} for the "Stopped in Error" status text's flash colour. */
    public static Color stopRed() {
        return STOP_RED;
    }

    /** The fixed orange used for the Pause/Start-Paused buttons, reused by {@code DesignerCanvas} for the "Paused" status text's colour. */
    public static Color pauseOrange() {
        return PAUSE_ORANGE;
    }

    /**
     * The button's colour when enabled - green for Start, red for Stop, orange for both Pause and Start-Paused
     * (grouped with Pause since both actions land the flow in a paused state, not a running one). Disabled
     * buttons don't use this - see {@code DesignerCanvas#paintFlowTransportControls}, which substitutes
     * {@link ThemeAwareColors#getDisabledTextColor()} instead whenever {@link #isEnabledFor} is false.
     */
    public Color getColor() {
        return switch (this) {
            case START -> ThemeAwareColors.getSuccessColor();
            case STOP -> STOP_RED;
            case PAUSE, START_PAUSE -> PAUSE_ORANGE;
        };
    }

    /**
     * Which of the 4 actions make sense from a flow's current raw Ikasan state (see
     * {@code org.ikasan.spec.flow.Flow}'s RUNNING/STOPPED/PAUSED/STOPPED_IN_ERROR/RECOVERING constants), per the
     * user's own explicit spec for this feature: Running enables Stop+Pause only; Paused enables Start+Stop only;
     * Stopped AND Stopped-in-Error are treated identically, enabling Start+Start-Paused only (deliberately NOT
     * Stop - a flow that's already stopped, in error or otherwise, has nothing left for Stop to do). "recovering"
     * and any unrecognised/null state disable every button, since a transitional state is exactly when a
     * conflicting extra click is most likely to make things worse. Not a faithful reproduction of the server-side
     * state machine, which ModuleControlApplication doesn't expose for querying - the REST call remains the
     * actual authority; the server would reject a genuinely invalid transition with its own FORBIDDEN response,
     * surfaced the same way any other REST failure is (see FlowTransportControlAction).
     */
    public static boolean isEnabledFor(FlowTransportAction action, String rawState) {
        if (rawState == null) {
            return false;
        }
        return switch (rawState) {
            case "running" -> action == STOP || action == PAUSE;
            case "paused" -> action == STOP || action == START;
            case "stopped", "stoppedInError" -> action == START || action == START_PAUSE;
            default -> false;
        };
    }
}
