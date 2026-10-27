package org.ikasan.studio.intellij.diagnostics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;

/** Latest attempt only, project scoped and memory only. No source documents retained. */
@Service(Service.Level.PROJECT)
public final class RuntimeImportDiagnostics {
    private volatile RuntimeImportReport report = new RuntimeImportReport();
    public RuntimeImportReport start() { var next = new RuntimeImportReport(); report = next; return next; }
    public RuntimeImportReport current() { return report; }
    public String text() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId("com.github.ikasaneip.ikasanstudio"));
        return report.render(plugin == null ? null : plugin.getVersion(), ApplicationInfo.getInstance().getBuild().asStringWithoutProductCode(), buildIdentity());
    }
    private static String buildIdentity() {
        try (var stream = RuntimeImportDiagnostics.class.getResourceAsStream("/studio/diagnostics/studio-import-build-id.txt")) {
            return stream == null ? null : new String(stream.readNBytes(65), java.nio.charset.StandardCharsets.US_ASCII).trim();
        } catch (java.io.IOException ignored) { return null; }
    }
    public static RuntimeImportDiagnostics get(Project project) { return project.getService(RuntimeImportDiagnostics.class); }
}
