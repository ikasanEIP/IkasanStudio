package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;

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
            Matcher matcher = REFERENCE.matcher(tests.getProperty(key));
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
    static Status inspect(Path root) throws Exception {
        Path tests = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Path application = root.resolve("generated/src/main/resources/application.properties");
        List<String> broken = Files.isRegularFile(tests) && Files.isRegularFile(application)
                ? brokenReferences(Files.readString(tests), Files.readString(application)) : List.of();
        Path support = root.resolve(FlowTestScaffold.SUPPORT_PATH);
        Path model = root.resolve("generated/src/main/model/model.json");
        boolean stale = false;
        if (Files.isRegularFile(support) && Files.isRegularFile(model)) {
            Module module = ComponentIO.validatePersistedModuleJson(
                    Files.readString(model), "flow test support check", false);
            stale = FlowTestScaffold.supportNeedsRefresh(
                    Files.readString(support), module);
        }
        return new Status(broken, stale);
    }

    public static void checkAfterRename(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            CanvasPanel panel = project.getService(UiContext.class).getCanvasPanel();
            if (panel != null) panel.refreshTestPropertyWarning();
        });
    }
}
