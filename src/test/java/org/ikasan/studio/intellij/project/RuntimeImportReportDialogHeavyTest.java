package org.ikasan.studio.intellij.project;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics;
import javax.swing.*;
import java.awt.*;

public class RuntimeImportReportDialogHeavyTest extends HeavyPlatformTestCase {
    public void testPageSelectionShowsBoundedDetailsAndReportSurvivesDialogClosure() {
        var service = RuntimeImportDiagnostics.get(getProject());
        var report = service.start();
        for (int i = 0; i < 30; i++) report.issue("F1/C1", "CONFIGURATION_NOT_SUPPLIED");
        var dialog = new RuntimeImportReportDialog(getProject(), null);
        try {
            var panel = dialog.createCenterPanel();
            var selector = find(panel, JComboBox.class);
            var text = find(panel, JTextArea.class);
            assertTrue(selector.getItemCount() > 1);
            assertTrue(text.getText().contains("PAGE 1 — SUMMARY"));
            assertFalse(text.isEditable());
            selector.setSelectedIndex(1);
            assertTrue(text.getText().contains("PAGE 2 — DETAILS"));
            assertFalse(text.getText().contains("PAGE 3 — DETAILS"));
            assertEquals(0, text.getCaretPosition());
        } finally { dialog.close(com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE); }
        assertSame(report, service.current());
        assertTrue(service.text().contains("IMPORT-030"));
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (var child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T match = find(nested, type);
                if (match != null) return match;
            }
        }
        return null;
    }
}
