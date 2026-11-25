package org.ikasan.studio.intellij.project;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import java.awt.*;
import java.awt.Component;
import java.util.Objects;
import javax.swing.*;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;
import org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics;

public class RuntimeImportReportDialogHeavyTest extends HeavyPlatformTestCase {
    public void testPageSelectionShowsBoundedDetailsAndReportSurvivesDialogClosure() {
        RuntimeImportDiagnostics service = RuntimeImportDiagnostics.get(getProject());
        RuntimeImportReport report = service.start();
        for (int i = 0; i < 30; i++) report.issue("F1/C1", "CONFIGURATION_NOT_SUPPLIED");
        RuntimeImportReportDialog dialog = new RuntimeImportReportDialog(getProject(), null);
        try {
            JComponent panel = Objects.requireNonNull(dialog.createCenterPanel());
            JComboBox<?> selector = Objects.requireNonNull(find(panel, JComboBox.class));
            JTextArea text = Objects.requireNonNull(find(panel, JTextArea.class));
            assertTrue(selector.getItemCount() > 1);
            assertTrue(text.getText().contains("PAGE 1 — SUMMARY"));
            assertFalse(text.isEditable());
            selector.setSelectedIndex(1);
            assertTrue(text.getText().contains("PAGE 2 — DETAILS"));
            assertFalse(text.getText().contains("PAGE 3 — DETAILS"));
            assertEquals(0, text.getCaretPosition());
        } finally { dialog.close(DialogWrapper.OK_EXIT_CODE); }
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
