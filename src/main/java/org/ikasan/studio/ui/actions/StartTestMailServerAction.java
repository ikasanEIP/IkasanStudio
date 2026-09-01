package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.ui.TerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Downloads (once, cached under PathManager's system directory - no existing per-plugin cache convention in
 * this codebase to follow, see #cacheDirectory) and launches <a href="https://github.com/mailhog/MailHog">MailHog</a>, a
 * small local SMTP server with a web-based inbox UI, bound to an Email Producer's configured
 * mailSmtpHost/mailSmtpPort (falling back to a sane local default if unset) - so the user can see what a flow
 * actually sends without a real mail account.
 * -
 * Launched in an IntelliJ Terminal tab, mirroring LaunchH2Action, rather than a raw Process this plugin would
 * then have to track the lifecycle of - its output stays visible there. See {@link StopTestMailServerAction}
 * for the companion action that stops it, found the same way (by the fixed terminal tab title in
 * {@link TestMailServerSupport}) rather than via any in-memory reference this action might hold - deliberately,
 * so stopping it stays reliable even if the plugin (or the user) has otherwise lost track of it.
 */
public class StartTestMailServerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#StartTestMailServerAction");
    private static final String MAILHOG_VERSION = "v1.0.1";
    private static final String MAILHOG_RELEASE_BASE_URL = "https://github.com/mailhog/MailHog/releases/download/" + MAILHOG_VERSION + "/";
    // Only ever used off-EDT, inside the Task.Backgroundable below.
    private static final Duration DOWNLOAD_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final Project project;
    private final BasicElement ikasanBasicElement;

    public StartTestMailServerAction(Project project, BasicElement ikasanBasicElement) {
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
        String uiUrl = "http://" + TestMailServerSupport.UI_HOST + ":" + TestMailServerSupport.UI_PORT;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.DownloadingTestMailServer")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                if (TestMailServerSupport.isAlreadyListening(smtpHost, smtpPort)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMailServerAlreadyRunning", smtpAddress, uiUrl));
                        BrowserUtil.browse(uiUrl);
                    });
                    return;
                }
                try {
                    Path binary = ensureBinaryDownloaded();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        launchInTerminal(binary, smtpHost, smtpPort);
                        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.StartingTestMailServer", smtpAddress, uiUrl));
                        BrowserUtil.browse(uiUrl);
                    });
                } catch (UnsupportedPlatformException e) {
                    LOG.warn("STUDIO: No test mail server build available for this platform", e);
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.UnsupportedPlatformForTestMailServer",
                                    System.getProperty("os.name"), System.getProperty("os.arch"))));
                } catch (Exception e) {
                    // warn (not error): IntelliJ's logger renders error-level stack traces directly to the
                    // user, and this is already surfaced via the popup below - see CLAUDE.md.
                    LOG.warn("STUDIO: Could not download/start the test mail server", e);
                    String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotDownloadTestMailServer", errorDetail)));
                }
            }
        });
    }

    private Path cacheDirectory() {
        return Path.of(PathManager.getSystemPath(), "ikasan-studio", "mailhog");
    }

    private Path ensureBinaryDownloaded() throws IOException, InterruptedException, UnsupportedPlatformException {
        String assetName = mailHogAssetNameForCurrentPlatform();
        Path cacheDir = cacheDirectory();
        Files.createDirectories(cacheDir);
        Path binary = cacheDir.resolve(assetName);
        if (Files.exists(binary) && Files.size(binary) > 0) {
            return binary;
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(MAILHOG_RELEASE_BASE_URL + assetName)).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(binary));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(binary);
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }
        // No effect on Windows (no executable bit to set); required on Linux/macOS before the terminal can run it.
        //noinspection ResultOfMethodCallIgnored
        binary.toFile().setExecutable(true, false);
        return binary;
    }

    private String mailHogAssetNameForCurrentPlatform() throws UnsupportedPlatformException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean is64Bit = arch.contains("64");
        if (os.contains("win")) {
            return is64Bit ? "MailHog_windows_amd64.exe" : "MailHog_windows_386.exe";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            // MailHog v1.0.1 only ships an amd64 macOS build - Rosetta 2 runs it fine on Apple Silicon.
            return "MailHog_darwin_amd64";
        }
        if (os.contains("nux") || os.contains("nix")) {
            if (arch.contains("arm")) {
                return "MailHog_linux_arm";
            }
            return is64Bit ? "MailHog_linux_amd64" : "MailHog_linux_386";
        }
        throw new UnsupportedPlatformException(os + "/" + arch);
    }

    private void launchInTerminal(Path binary, String smtpHost, int smtpPort) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
        if (window == null) {
            LOG.warn("STUDIO: WARN: Could not find the Terminal tool window to launch the test mail server in");
            return;
        }
        ContentManager contentManager = window.getContentManager();
        Content existingTab = contentManager.findContent(TestMailServerSupport.TERMINAL_TAB_TITLE);
        TerminalWidget terminalWidget;
        if (existingTab != null) {
            // A tab from an earlier click - isAlreadyListening() above already ruled out a live instance, so
            // this is a stale/closed-process tab; reuse it rather than accumulating a new one per click.
            terminalWidget = TerminalToolWindowManager.findWidgetByContent(existingTab);
            contentManager.setSelectedContent(existingTab);
        } else {
            terminalWidget = createTerminalWidget();
        }
        if (terminalWidget == null) {
            LOG.warn("STUDIO: WARN: Could not create or find terminal widget for the test mail server");
            return;
        }
        String quotedBinaryPath = "\"" + binary.toAbsolutePath() + "\"";
        String uiAddress = TestMailServerSupport.UI_HOST + ":" + TestMailServerSupport.UI_PORT;
        // -api-bind-addr set explicitly alongside -ui-bind-addr (both to the same address) even though MailHog
        // defaults it to the same port anyway - matches the exact invocation this feature was validated
        // against manually before being wired into the plugin, so there's no ambiguity if that default ever changes.
        String command = quotedBinaryPath + " -smtp-bind-addr " + smtpHost + ":" + smtpPort
                + " -api-bind-addr " + uiAddress + " -ui-bind-addr " + uiAddress;
        terminalWidget.sendCommandToExecute(command);
    }

    private TerminalWidget createTerminalWidget() {
        try {
            return TerminalToolWindowManager.getInstance(project).createShellWidget(cacheDirectory().toString(), TestMailServerSupport.TERMINAL_TAB_TITLE, true, true);
        } catch (Exception e) {
            LOG.warn("STUDIO: WARN: Failed to create terminal widget for the test mail server: " + e.getMessage(), e);
            return null;
        }
    }

    private static class UnsupportedPlatformException extends Exception {
        UnsupportedPlatformException(String platform) {
            super("Unsupported platform: " + platform);
        }
    }
}
