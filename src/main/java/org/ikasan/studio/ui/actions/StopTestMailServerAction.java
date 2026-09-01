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
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Companion to {@link StartTestMailServerAction} - an explicit, always-available "Stop" so the test mail server
 * is easy to shut down even if track of it has been lost (the user forgot they started it, or simply doesn't
 * want to go hunting for its terminal tab). Deliberately does not rely on any in-memory reference this plugin
 * might hold to what {@link StartTestMailServerAction} launched - instead it probes the actual configured
 * SMTP address (like Start's own already-running check), and only then looks up the same fixed-title Terminal
 * tab ({@link TestMailServerSupport#TERMINAL_TAB_TITLE}) via the IDE's own tool-window state and closes it,
 * which terminates its foreground process exactly as if the user had closed the tab themselves.
 * -
 * The port probe is checked first and deliberately gates every subsequent step: asking an unopened Terminal
 * tool window for its ContentManager forces IntelliJ to lazily initialise it there and then, which is both
 * unnecessary UI work for what should be a pure no-op, and was observed to log a spurious (harmless, but noisy)
 * "'ToolwindowTitle' toolbar manual update is ignored" warning from IntelliJ's own ActionToolbarImpl when
 * Terminal had never been opened yet this session - avoided entirely by not touching it unless something is
 * actually listening.
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

        String smtpHost = TestMailServerSupport.resolveSmtpHost(flowElement);
        int smtpPort = TestMailServerSupport.resolveSmtpPort(flowElement);
        String smtpAddress = smtpHost + ":" + smtpPort;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.StoppingTestMailServer")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                boolean listening = TestMailServerSupport.isAlreadyListening(smtpHost, smtpPort);
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
        if (existingTab == null) {
            // Something is listening on the configured address, but not in a tab this action recognises - most
            // likely started outside Studio entirely. Nothing safe to close here.
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMailServerRunningButNoTabFound", smtpAddress));
            return;
        }
        try {
            // true (disposeContent) tears down the tab's terminal widget, which kills its shell (and MailHog,
            // the shell's foreground child) exactly as the user's own close-tab gesture would.
            window.getContentManager().removeContent(existingTab, true);
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerStopped"));
        } catch (Exception e) {
            // warn (not error): IntelliJ's logger renders error-level stack traces directly to the user, and
            // this is already surfaced via the popup below - see CLAUDE.md.
            LOG.warn("STUDIO: Could not stop the test mail server", e);
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotStopTestMailServer", errorDetail));
        }
    }
}
