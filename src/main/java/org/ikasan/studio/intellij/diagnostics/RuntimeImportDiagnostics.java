package org.ikasan.studio.intellij.diagnostics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationInfo;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;

/** Latest attempt only, project scoped and memory only. No source documents retained. */
@Service(Service.Level.PROJECT)
public final class RuntimeImportDiagnostics {
    private volatile RuntimeImportReport report = new RuntimeImportReport();
    public RuntimeImportReport start() { var next = new RuntimeImportReport(); report = next; return next; }
    public RuntimeImportReport current() { return report; }
    public String text() {
        return report.render(buildMetadata("studio-plugin-version.txt"),
                ApplicationInfo.getInstance().getBuild().asStringWithoutProductCode(), buildMetadata("studio-import-build-id.txt"));
    }
    private static String buildMetadata(String name) {
        try (var stream = RuntimeImportDiagnostics.class.getResourceAsStream("/studio/diagnostics/" + name)) {
            return stream == null ? null : new String(stream.readNBytes(121), java.nio.charset.StandardCharsets.US_ASCII).trim();
        } catch (java.io.IOException ignored) { return null; }
    }
    public static RuntimeImportDiagnostics get(Project project) { return project.getService(RuntimeImportDiagnostics.class); }
}
