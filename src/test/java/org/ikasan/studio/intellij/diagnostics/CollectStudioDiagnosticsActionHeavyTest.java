package org.ikasan.studio.intellij.diagnostics;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;

public class CollectStudioDiagnosticsActionHeavyTest extends HeavyPlatformTestCase {
    public void testRegisteredActionIsAvailableThroughFindActionAndDuringIndexing() {
        var action = ActionManager.getInstance().getAction("IkasanStudio.CollectDiagnostics");
        assertInstanceOf(action, CollectStudioDiagnosticsAction.class);
        assertTrue(action instanceof DumbAware);
        assertEquals(ActionUpdateThread.BGT, action.getActionUpdateThread());
        assertNotNull(action.getTemplatePresentation().getText());
        assertNotNull(action.getTemplatePresentation().getDescription());
    }
}
