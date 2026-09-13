package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Recovers process identity from this project's terminal, never from a process name alone. */
final class MailHarnessProcessRecovery {
    private MailHarnessProcessRecovery() { }

    /** Called on the EDT; process enumeration happens later in the background task. */
    static ProcessHandle terminalShell(Project project) {
        var window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
        if (window == null) return null;
        var content = window.getContentManager().findContent(TestMailServerSupport.TERMINAL_TAB_TITLE);
        if (content == null) return null;
        var widget = TerminalToolWindowManager.findWidgetByContent(content);
        if (widget == null) return null;
        var shell = ShellTerminalWidget.asShellJediTermWidget(widget);
        if (shell == null || shell.getProcessTtyConnector() == null) return null;
        // IntelliJ's PTY process exposes pid(), but does not implement Process.toHandle().
        return ProcessHandle.of(shell.getProcessTtyConnector().getProcess().pid()).orElse(null);
    }

    static ProcessHandle findHarness(ProcessHandle shell, String host, int port) {
        if (shell == null || !shell.isAlive()) return null;
        Path cache = Path.of(PathManager.getSystemPath(), "ikasan-studio", "mailhog");
        try (var descendants = shell.descendants()) {
            List<ProcessHandle> matches = descendants.filter(process -> {
                var info = process.info();
                return matchesLaunch(cache, info.command().orElse(""), info.arguments().orElse(new String[0]), host, port);
            }).limit(2).toList();
            return matches.size() == 1 ? matches.get(0) : null;
        }
    }

    static boolean matchesLaunch(Path cache, String command, String[] arguments, String host, int port) {
        if (command.isBlank()) return false;
        Path binary = Path.of(command).toAbsolutePath().normalize();
        if (!cache.toAbsolutePath().normalize().equals(binary.getParent())) return false;
        if (!List.of("MailHog_linux_amd64", "MailHog_linux_386", "MailHog_linux_arm", "MailHog_darwin_amd64",
                "MailHog_windows_amd64.exe", "MailHog_windows_386.exe").contains(binary.getFileName().toString())) return false;
        var args = Arrays.asList(arguments);
        return address(args, "-smtp-bind-addr").equals(normalize(host + ":" + port))
                && address(args, "-api-bind-addr").equals(TestMailServerSupport.UI_HOST + ":" + TestMailServerSupport.UI_PORT)
                && address(args, "-ui-bind-addr").equals(TestMailServerSupport.UI_HOST + ":" + TestMailServerSupport.UI_PORT);
    }

    private static String address(List<String> args, String flag) {
        int index = args.indexOf(flag);
        return index >= 0 && index + 1 < args.size() ? normalize(args.get(index + 1)) : "";
    }

    private static String normalize(String address) {
        return address.startsWith("localhost:") ? "127.0.0.1:" + address.substring("localhost:".length()) : address;
    }
}
