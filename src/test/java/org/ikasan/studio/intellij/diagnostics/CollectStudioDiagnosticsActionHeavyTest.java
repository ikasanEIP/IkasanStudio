package org.ikasan.studio.intellij.diagnostics;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;

public class CollectStudioDiagnosticsActionHeavyTest extends HeavyPlatformTestCase {
    public void testImportReportIsProjectScopedAndRegisteredDuringIndexing() {
        var action = ActionManager.getInstance().getAction("IkasanStudio.ImportReport");
        assertInstanceOf(action, ShowRuntimeImportReportAction.class);
        assertTrue(action instanceof DumbAware);
        assertNotNull(action.getTemplatePresentation().getText());
        var service = RuntimeImportDiagnostics.get(getProject());
        var previous = service.start(); previous.issue("DOCUMENT", "METADATA_HTTP_500");
        assertTrue(service.text().contains("METADATA_HTTP_500"));
        assertNotSame(previous, service.start());
        assertFalse(service.text().contains("METADATA_HTTP_500"));
    }
    public void testRegisteredActionIsAvailableThroughFindActionAndDuringIndexing() {
        var action = ActionManager.getInstance().getAction("IkasanStudio.CollectDiagnostics");
        assertInstanceOf(action, CollectStudioDiagnosticsAction.class);
        assertTrue(action instanceof DumbAware);
        assertEquals(ActionUpdateThread.BGT, action.getActionUpdateThread());
        assertNotNull(action.getTemplatePresentation().getText());
        assertNotNull(action.getTemplatePresentation().getDescription());
    }
}
