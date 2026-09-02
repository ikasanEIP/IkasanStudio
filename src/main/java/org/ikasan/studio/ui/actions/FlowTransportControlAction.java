package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.FlowControlOperation;
import org.ikasan.studio.integration.ikasan.ModuleControlClient;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.runtime.FlowErrorMonitorService;

import java.net.ConnectException;

/**
 * Fires one of the 4 {@link FlowTransportAction}s against the running module's moduleControl REST endpoint - the
 * click handler behind the canvas's per-flow Start/Stop/Pause/Start-Paused buttons (see
 * {@code DesignerCanvas#paintFlowTransportControls}). Mirrors {@code TriggerScheduledConsumerAction}'s
 * Task.Backgroundable shape: the HTTP call happens off the EDT, and on success triggers one immediate
 * {@link FlowErrorMonitorService} poll - deliberately NOT gated by
 * {@code IkasanStudioSettings#isFlowErrorMonitoringEnabled()}, the same "one-off check at the moment of the
 * click, regardless of whether background live polling is on" precedent already established for the Test Mail
 * Server Start/Stop actions - so the status label/button state reflects the user's own click immediately rather
 * than waiting out the ~6s background poll interval (or, with monitoring off entirely, never updating at all).
 */
public final class FlowTransportControlAction {
    private static final Logger LOG = Logger.getInstance("#FlowTransportControlAction");

    private FlowTransportControlAction() {
    }

    public static void fire(Project project, String flowName, FlowTransportAction action, String rawState) {
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || module.getIdentity() == null) {
            return;
        }
        FlowControlOperation operation = action.resolveOperation(rawState);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.ChangingFlowState", flowName)) {
            // Deliberately not @NotNull-annotated - see TriggerScheduledConsumerAction's identical comment.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    ModuleControlClient.changeFlowState(module, flowName, operation);
                } catch (ConnectException e) {
                    LOG.warn("STUDIO: Could not change flow state for " + flowName + " - module not yet accepting connections", e);
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ModuleNotYetAcceptingConnections")));
                    return;
                } catch (Exception e) {
                    // warn (not error): IntelliJ's logger renders error-level stack traces directly to the
                    // user, and this is already surfaced via the popup below - see CLAUDE.md.
                    LOG.warn("STUDIO: Could not change flow state for " + flowName + " to " + operation.getWireValue(), e);
                    String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotChangeFlowState", flowName, errorDetail)));
                    return;
                }
                // Already off the EDT here (Task.Backgroundable) - pollNow() does its own HTTP call and EDT
                // hop for the repaint, so nothing further to dispatch.
                project.getService(FlowErrorMonitorService.class).pollNow();
            }
        });
    }
}
