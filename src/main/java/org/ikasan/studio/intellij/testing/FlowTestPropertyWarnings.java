package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Advisory check after successful renames; file IO never runs on the EDT. */
public final class FlowTestPropertyWarnings {
    private static final Pattern REFERENCE = Pattern.compile("\\$\\{([^{}:]+)}");
    private FlowTestPropertyWarnings() { }

    static List<String> brokenReferences(String testText, String applicationText) throws Exception {
        Properties tests = new Properties();
        Properties application = new Properties();
        tests.load(new StringReader(testText));
        application.load(new StringReader(applicationText));
        List<String> broken = new ArrayList<>();
        for (String key : tests.stringPropertyNames().stream().sorted().toList()) {
            var matcher = REFERENCE.matcher(tests.getProperty(key));
            while (matcher.find()) {
                String reference = matcher.group(1);
                // Restrict this rename advisory to generated component configuration namespaces.
                // Environment variables, Spring settings and placeholders with defaults are not rename errors.
                if (reference.matches("(?:jms|ftp|sftp|email|localfile)\\..+")
                        && !tests.containsKey(reference) && !application.containsKey(reference)) {
                    broken.add(key + " → " + reference);
                }
            }
        }
        return List.copyOf(broken);
    }

    record Status(List<String> brokenReferences, boolean staleSupport) {
        boolean needsRefresh() { return staleSupport || !brokenReferences.isEmpty(); }
    }

    /** Reads saved state only; callers must use a background thread. */
    static Status inspect(java.nio.file.Path root) throws Exception {
        var tests = root.resolve(org.ikasan.studio.core.generator.FlowTestScaffold.TEST_PROPERTIES_PATH);
        var application = root.resolve("generated/src/main/resources/application.properties");
        List<String> broken = java.nio.file.Files.isRegularFile(tests) && java.nio.file.Files.isRegularFile(application)
                ? brokenReferences(java.nio.file.Files.readString(tests), java.nio.file.Files.readString(application)) : List.of();
        var support = root.resolve(org.ikasan.studio.core.generator.FlowTestScaffold.SUPPORT_PATH);
        var model = root.resolve("generated/src/main/model/model.json");
        boolean stale = false;
        if (java.nio.file.Files.isRegularFile(support) && java.nio.file.Files.isRegularFile(model)) {
            var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(
                    java.nio.file.Files.readString(model), "flow test support check", false);
            stale = org.ikasan.studio.core.generator.FlowTestScaffold.supportNeedsRefresh(
                    java.nio.file.Files.readString(support), module);
        }
        return new Status(broken, stale);
    }

    public static void checkAfterRename(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            var panel = project.getService(org.ikasan.studio.ui.UiContext.class).getCanvasPanel();
            if (panel != null) panel.refreshTestPropertyWarning();
        });
    }
}
