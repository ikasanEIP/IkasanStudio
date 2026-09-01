package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.TestMailServerLinks;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.TestMailServerSupport;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which (mailSmtpHost, mailSmtpPort) addresses a locally-launched test mail server is actually
 * listening on, for the canvas's shared "Test Mail Server" node (see {@code DesignerCanvas} - the node/lines
 * are painted only for an address this service currently reports as listening, and disappear the tick after
 * it stops).
 * -
 * Unlike {@link IkasanDebugSessionService}, there's no OS process handle or ExecutionListener to observe here
 * - StartTestMailServerAction/StopTestMailServerAction launch/stop MailHog via a plain Terminal tab (see their
 * own javadoc for why), so this polls the actual TCP address on a background timer instead - the same live
 * probe Start/Stop already use to check "is one already running" before acting
 * ({@link TestMailServerSupport#isAlreadyListening}). Start/Stop also call {@link #pollNow()} right after
 * acting, so the canvas updates promptly rather than waiting out the full poll interval.
 * -
 * The poll only ever checks addresses actually configured on an Email Producer somewhere in the current
 * module (via {@link TestMailServerLinks#findLinks}), so it costs nothing extra for a module with no Email
 * Producer at all, and never polls a stale address left over from an edited/removed component.
 */
@Service(Service.Level.PROJECT)
public final class TestMailServerSessionService implements Disposable {
    // A few seconds is a reasonable balance between "the canvas node appears/disappears promptly" and "don't
    // hammer localhost sockets" - Start/Stop's own pollNow() covers the common case of wanting near-instant
    // feedback right after clicking, so this interval only matters for external changes (e.g. the user closing
    // the terminal tab by hand) that Start/Stop can't know about.
    private static final int POLL_INTERVAL_MS = 4000;

    private final Project project;
    private final Alarm alarm;
    private volatile Set<String> listeningAddresses = Set.of();

    public TestMailServerSessionService(Project project) {
        this.project = project;
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        scheduleNextPoll();
    }

    /** @return true if a test mail server is currently confirmed listening on this exact address. */
    public boolean isListening(String host, int port) {
        return listeningAddresses.contains(host + ":" + port);
    }

    /**
     * Forces an immediate, unconditional re-check - called by StartTestMailServerAction/StopTestMailServerAction
     * right after acting, for prompt canvas feedback. Unlike the scheduled tick, this always actually probes,
     * regardless of {@link IkasanStudioSettings#isTestMailServerLivePollingEnabled()}: with that setting off,
     * a one-off check right when the user clicks Start/Stop is exactly the "rely on the click" behaviour the
     * setting promises, not something it should also suppress.
     */
    public void pollNow() {
        if (!alarm.isDisposed()) {
            alarm.cancelAllRequests();
            alarm.addRequest(() -> {
                probeAndUpdateState();
                scheduleNextPoll();
            }, 0);
        }
    }

    private void scheduleNextPoll() {
        if (!alarm.isDisposed()) {
            alarm.addRequest(this::pollAndReschedule, POLL_INTERVAL_MS);
        }
    }

    /**
     * The scheduled (timer-driven) tick - unlike {@link #pollNow()}, this respects
     * {@link IkasanStudioSettings#isTestMailServerLivePollingEnabled()}: when the user has switched live
     * polling off (see that setting's own doc for why - e.g. mailSmtpHost pointed somewhere non-local/slow,
     * or local security software intercepting the probe), this tick does no work at all - no address lookup,
     * no socket calls - and state is then driven purely by Start/Stop's own pollNow() calls. The timer itself
     * keeps ticking regardless (at negligible cost, see below) purely so re-enabling the setting later starts
     * working again immediately, with no service restart needed.
     */
    private void pollAndReschedule() {
        if (IkasanStudioSettings.isTestMailServerLivePollingEnabled()) {
            probeAndUpdateState();
        }
        scheduleNextPoll();
    }

    private void probeAndUpdateState() {
        Set<String> nowListening = new HashSet<>();
        Module ikasanModule = project.isDisposed() ? null : project.getService(UiContext.class).getIkasanModule();
        if (ikasanModule != null) {
            List<TestMailServerLinks.Link> links = TestMailServerLinks.findLinks(ikasanModule);
            for (TestMailServerLinks.Link link : links) {
                if (TestMailServerSupport.isAlreadyListening(link.host(), link.port())) {
                    nowListening.add(link.address());
                }
            }
        }
        boolean changed = !nowListening.equals(listeningAddresses);
        listeningAddresses = nowListening;
        if (changed) {
            repaintCanvas();
        }
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
        if (ApplicationManager.getApplication().isDispatchThread()) {
            repaint.run();
        } else {
            ApplicationManager.getApplication().invokeLater(repaint);
        }
    }

    @Override
    public void dispose() {
        listeningAddresses = Set.of();
    }
}
