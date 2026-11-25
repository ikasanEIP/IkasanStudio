package org.ikasan.studio.intellij.diagnostics;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.actionSystem.AnAction;
import java.io.InputStream;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;

public class CollectStudioDiagnosticsActionHeavyTest extends HeavyPlatformTestCase {
    public void testImportReportDisplaysTheBuildGeneratedVersionWithoutPluginManagerLookup() throws Exception {
        try (InputStream stream = RuntimeImportDiagnostics.class.getResourceAsStream("/studio/diagnostics/studio-plugin-version.txt")) {
            assertNotNull("Plugin version metadata must be packaged", stream);
            String version = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII).trim();
            assertTrue("Expected a version, not missing metadata", version.matches("[0-9]+[.][0-9]+[.][0-9]+(?:-[A-Za-z0-9.-]+)?"));
            assertTrue(RuntimeImportDiagnostics.get(getProject()).text().contains("Plugin: " + version + "; IDE build:"));
        }
    }
    public void testImportReportIsProjectScopedAndRegisteredDuringIndexing() {
        AnAction action = ActionManager.getInstance().getAction("IkasanStudio.ImportReport");
        assertInstanceOf(action, ShowRuntimeImportReportAction.class);
        assertTrue(action instanceof DumbAware);
        assertNotNull(action.getTemplatePresentation().getText());
        RuntimeImportDiagnostics service = RuntimeImportDiagnostics.get(getProject());
        RuntimeImportReport previous = service.start(); previous.issue("DOCUMENT", "METADATA_HTTP_500");
        assertTrue(service.text().contains("METADATA_HTTP_500"));
        assertNotSame(previous, service.start());
        assertFalse(service.text().contains("METADATA_HTTP_500"));
    }
    public void testRegisteredActionIsAvailableThroughFindActionAndDuringIndexing() {
        AnAction action = ActionManager.getInstance().getAction("IkasanStudio.CollectDiagnostics");
        assertInstanceOf(action, CollectStudioDiagnosticsAction.class);
        assertTrue(action instanceof DumbAware);
        assertEquals(ActionUpdateThread.BGT, action.getActionUpdateThread());
        assertNotNull(action.getTemplatePresentation().getText());
        assertNotNull(action.getTemplatePresentation().getDescription());
    }
}
