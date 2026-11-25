package org.ikasan.studio.intellij.project;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics;
import javax.swing.*;
import java.awt.*;
import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;

public class RuntimeImportReportDialogHeavyTest extends HeavyPlatformTestCase {
    public void testPageSelectionShowsBoundedDetailsAndReportSurvivesDialogClosure() {
        RuntimeImportDiagnostics service = RuntimeImportDiagnostics.get(getProject());
        RuntimeImportReport report = service.start();
        for (int i = 0; i < 30; i++) report.issue("F1/C1", "CONFIGURATION_NOT_SUPPLIED");
        RuntimeImportReportDialog dialog = new RuntimeImportReportDialog(getProject(), null);
        try {
            JComponent panel = dialog.createCenterPanel();
            JComboBox selector = find(panel, JComboBox.class);
            JTextArea text = find(panel, JTextArea.class);
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
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T match = find(nested, type);
                if (match != null) return match;
            }
        }
        return null;
    }
}
