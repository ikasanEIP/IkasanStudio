package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.intellij.runtime.TestMailServerSessionService;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Companion to {@link StartTestMailServerAction} - an explicit, always-available "Stop" so the test mail server
 * is easy to shut down even if track of it has been lost (the user forgot they started it, or simply doesn't
 * want to go hunting for its terminal tab). Deliberately does not rely on any in-memory reference this plugin
 * might hold to what {@link StartTestMailServerAction} launched - instead it probes the actual configured
 * SMTP address (like Start's own already-running check), falling back to "is any Studio-launched instance
 * running at all" (see {@link TestMailServerSupport#findAnyLaunchedProcess}) so a harness that's still running
 * under a different address than the component is currently configured with (e.g. its mailSmtpPort was edited
 * after Start was clicked) is still found rather than reported as not running. Once something is confirmed
 * running, it looks up the same fixed-title Terminal tab ({@link TestMailServerSupport#TERMINAL_TAB_TITLE}) via
 * the IDE's own tool-window state and closes it, which terminates its foreground process exactly as if the
 * user had closed the tab themselves.
 * -
 * The listening/running check is deliberately done first and gates every subsequent step: asking an unopened
 * Terminal tool window for its ContentManager forces IntelliJ to lazily initialise it there and then, which is
 * both unnecessary UI work for what should be a pure no-op, and was observed to log a spurious (harmless, but
 * noisy) "'ToolwindowTitle' toolbar manual update is ignored" warning from IntelliJ's own ActionToolbarImpl
 * when Terminal had never been opened yet this session - avoided entirely by not touching it unless something
 * is actually running.
 */
public class StopTestMailServerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#StopTestMailServerAction");

    private final Project project;
    private final BasicElement ikasanBasicElement;

    public StopTestMailServerAction(Project project, BasicElement ikasanBasicElement) {
        this.project = project;
        this.ikasanBasicElement = ikasanBasicElement;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (!(ikasanBasicElement instanceof FlowElement flowElement) || !flowElement.getComponentMeta().supportsTestMailServer()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMailServerCanOnlyBeUsedOnEmailProducer"));
            return;
        }

        String smtpHost = TestMailServerLinks.resolveSmtpHost(flowElement);
        int smtpPort = TestMailServerLinks.resolveSmtpPort(flowElement);
        String smtpAddress = smtpHost + ":" + smtpPort;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.StoppingTestMailServer")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                // The exact-address probe alone misses a harness that's still running but was started before
                // the component's mailSmtpHost/mailSmtpPort was edited to something else - the live process is
                // still bound to whatever address it actually started with. Since at most one Studio-launched
                // instance can ever be running at a time (see StartTestMailServerAction's own pre-flight
                // check), falling back to "is any Studio-launched instance running at all" is unambiguous and
                // catches that case instead of falsely reporting "not running".
                boolean listening = TestMailServerSupport.isAlreadyListening(smtpHost, smtpPort)
                        || TestMailServerSupport.findAnyLaunchedProcess().isPresent();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!listening) {
                        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerNotRunning"));
                    } else {
                        stopViaTerminalTab(smtpAddress);
                    }
                });
            }
        });
    }

    /**
     * Only ever called once the SMTP probe above has confirmed something is actually listening - see the class
     * javadoc for why the Terminal tool window is otherwise left untouched.
     */
    private void stopViaTerminalTab(String smtpAddress) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
        Content existingTab = window == null ? null : window.getContentManager().findContent(TestMailServerSupport.TERMINAL_TAB_TITLE);
        if (existingTab != null) {
            try {
                // true (disposeContent) tears down the tab's terminal widget, which kills its shell (and
                // MailHog, the shell's foreground child) exactly as the user's own close-tab gesture would.
                window.getContentManager().removeContent(existingTab, true);
                project.getService(TestMailServerSessionService.class).pollNow();
                StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerStopped"));
            } catch (Exception e) {
                // warn (not error): IntelliJ's logger renders error-level stack traces directly to the user, and
                // this is already surfaced via the popup below - see CLAUDE.md.
                LOG.warn("STUDIO: Could not stop the test mail server", e);
                String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotStopTestMailServer", errorDetail));
            }
            return;
        }
        // No matching tab in *this* window's Terminal tool window - each IDE window has its own independent
        // ToolWindowManager, so Start having been run from a different Studio window looks identical from here
        // to "started outside Studio entirely" (the SMTP probe above is a plain TCP connect, not scoped to any
        // window). Before giving up, look for the actual OS process by the exact command line
        // StartTestMailServerAction launched it with - that's real OS state, not window-scoped, so it finds a
        // same-machine, different-window instance while still leaving a genuinely external process alone.
        if (stopByMatchingProcessHandle(smtpAddress)) {
            project.getService(TestMailServerSessionService.class).pollNow();
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerStopped"));
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMailServerRunningButNoTabFound", smtpAddress));
        }
    }

    /**
     * @return true if a process StartTestMailServerAction launched was found and asked to terminate, false if
     *         none matched (including when command-line introspection isn't permitted on this platform/JVM) -
     *         callers should fall back to treating this the same as "nothing recognised" in that case, rather
     *         than assume it worked. Deliberately doesn't know the launch-command syntax itself - that stays
     *         confined to TestMailServerSupport/StartTestMailServerAction so this action stays agnostic to
     *         whichever tool is actually being launched underneath.
     *         -
     *         Tries the exact configured address first, then falls back to any Studio-launched instance
     *         regardless of address (safe - see {@link TestMailServerSupport#findAnyLaunchedProcess}) so a
     *         harness that's still running under an address the component was edited away from after starting
     *         it is still found, not just one matching a different IDE window at the *same* address.
     */
    private boolean stopByMatchingProcessHandle(String smtpAddress) {
        return TestMailServerSupport.findLaunchedProcess(smtpAddress)
                .or(TestMailServerSupport::findAnyLaunchedProcess)
                .map(handle -> {
                    handle.destroy();
                    return true;
                })
                .orElse(false);
    }
}
