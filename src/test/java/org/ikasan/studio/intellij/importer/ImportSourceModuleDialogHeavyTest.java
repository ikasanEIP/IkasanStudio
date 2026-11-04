package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.importer.SourceModuleImport;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.nio.file.Files;
import java.util.*;

public class ImportSourceModuleDialogHeavyTest extends HeavyPlatformTestCase {
    public void testSourceReviewHonoursThePropertiesStepChosenInTheParentJourney() throws Exception {
        for (boolean selected : new boolean[]{false, true}) {
            var dialog = new ImportSourceModuleDialog(getProject(), selected);
            try { assertEquals(selected, ((JCheckBox) field(dialog, "properties")).isSelected()); }
            finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
        }
    }

    public void testAnalysisDefaultsNewCodeAndDependenciesButProtectsExistingCodeWiringAndManualItemsAndCancelDoesNotWrite() throws Exception {
        var root = Files.createTempDirectory("studio-source-wizard");
        var original = root.resolve("original"); var destination = root.resolve("studio");
        Files.createDirectories(original); Files.createDirectories(destination);
        String bytes = Base64.getEncoder().encodeToString("class Bespoke {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var dependency = new org.apache.maven.model.Dependency(); dependency.setGroupId("customer"); dependency.setArtifactId("client"); dependency.setVersion("1");
        var local = dependency.clone(); local.setArtifactId("local"); local.setScope("system"); local.setSystemPath("/original/local.jar");
        var analysis = new SourceModuleImport.Analysis(original, destination, List.of(
                new SourceModuleImport.FileChoice("src/main/java/Bespoke.java", "user/src/main/java/Bespoke.java", bytes, null, true, false, "Matched"),
                new SourceModuleImport.FileChoice("src/main/java/Config.java", "user/src/main/java/Config.java", bytes, null, false, true, "Review wiring"),
                new SourceModuleImport.FileChoice("src/main/java/Existing.java", "user/src/main/java/Existing.java", bytes, bytes, true, false, "Existing")),
                List.of(), List.of(), List.of(new SourceModuleImport.DependencyChoice("customer:client:jar:", dependency, true, "Original declaration"),
                        new SourceModuleImport.DependencyChoice("customer:local:jar:", local, false, "Non-portable system path")),
                List.of(), bytes, Map.of());
        var dialog = new ImportSourceModuleDialog(getProject());
        try {
            dialog.acceptAnalysis(analysis, original);
            var table = (JTable) field(dialog, "table"); TableModel model = table.getModel();
            ((javax.swing.table.AbstractTableModel) model).fireTableDataChanged();
            assertEquals(5, model.getRowCount());
            assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(1, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(2, 0));
            assertEquals(Boolean.TRUE, model.getValueAt(3, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(4, 0));
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.dependency.manual"), model.getValueAt(4, 2));
            assertFalse(model.isCellEditable(4, 0));
            var checkbox = table.getCellRenderer(4, 0).getTableCellRendererComponent(table, Boolean.FALSE, false, false, 4, 0);
            assertFalse(checkbox.isEnabled());
            model.setValueAt(Boolean.TRUE, 0, 0);
            assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
            var validate = dialog.getClass().getDeclaredMethod("doValidate"); validate.setAccessible(true);
            assertNull(validate.invoke(dialog));
            ((JTextField) field(dialog, "source")).setText(root.resolve("different").toString());
            assertNotNull(validate.invoke(dialog));
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertFalse(Files.exists(destination.resolve("user")));
        } finally {
            if (!dialog.isDisposed()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(destination); Files.deleteIfExists(original); Files.deleteIfExists(root);
        }
    }
    public void testOriginalColumnsAndSortedReadOnlyComparisonsForSourceAndSupport() throws Exception {
        var dialog = previewDialog();
        try {
            var table = (JTable) field(dialog, "table");
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.evidence"), table.getColumnName(1));
            assertEquals("user/B.java", table.getModel().getValueAt(0, 2));
            table.getRowSorter().toggleSortOrder(1);
            table.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(table), "original A", "destination A");
            var support = (JTable) field(dialog, "supportTable");
            assertEquals(1, support.getRowCount());
            assertFalse(support.isCellEditable(0, 0)); assertFalse(support.isCellEditable(0, 1));
            support.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(support), "original support", "destination support");
            assertTrue(((JTextArea) field(dialog, "supportNotice")).isVisible());
            assertEquals(3, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            assertEquals(2, table.getRowCount());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testDoubleClickAndEnterOpenDiffsWithoutChangingImportSelections() throws Exception {
        var dialog = previewDialog();
        try (var service = org.mockito.Mockito.mockStatic(com.intellij.diff.DiffManager.class)) {
            var manager = org.mockito.Mockito.mock(com.intellij.diff.DiffManager.class);
            service.when(com.intellij.diff.DiffManager::getInstance).thenReturn(manager);
            var table = (JTable) field(dialog, "table"); table.setSize(800, 200); table.doLayout();
            table.getRowSorter().toggleSortOrder(1);
            doubleClick(table, 0);
            org.mockito.Mockito.verifyNoInteractions(manager);
            doubleClick(table, 1);
            table.getActionMap().get("studio.compareSource").actionPerformed(new java.awt.event.ActionEvent(table, 0, "compare"));
            var support = (JTable) field(dialog, "supportTable"); support.setSize(800, 200); support.doLayout();
            doubleClick(support, 0);
            var requests = org.mockito.ArgumentCaptor.forClass(com.intellij.diff.requests.DiffRequest.class);
            org.mockito.Mockito.verify(manager, org.mockito.Mockito.times(3)).showDiff(org.mockito.Mockito.eq(getProject()), requests.capture());
            assertDiff((com.intellij.diff.requests.SimpleDiffRequest) requests.getAllValues().get(0), "original A", "destination A");
            assertDiff((com.intellij.diff.requests.SimpleDiffRequest) requests.getAllValues().get(2), "original support", "destination support");
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private ImportSourceModuleDialog previewDialog() throws Exception {
        var dialog = new ImportSourceModuleDialog(getProject());
        var analysis = new SourceModuleImport.Analysis(java.nio.file.Path.of("/original"), java.nio.file.Path.of("/destination"), List.of(
                new SourceModuleImport.FileChoice("src/B.java", "user/B.java", encoded("original B"), encoded("destination B"), false, false, "Review"),
                new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("original A"), encoded("destination A"), false, false, "Review")),
                List.of(), List.of(), List.of(), List.of(), "", Map.of(),
                List.of(new SourceModuleImport.ManagedFile("src/DebugTransitionComponent.java", "user/DebugTransitionComponent.java",
                        encoded("original support"), encoded("destination support"), SourceModuleImport.SupportStatus.ORIGINAL_DIFFERS)), Map.of());
        setAnalysis(dialog, analysis);
        ((javax.swing.table.AbstractTableModel) ((JTable) field(dialog, "table")).getModel()).fireTableDataChanged();
        var refresh = dialog.getClass().getDeclaredMethod("refreshReport"); refresh.setAccessible(true); refresh.invoke(dialog);
        return dialog;
    }
    private String encoded(String text) { return Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private void assertDiff(com.intellij.diff.requests.SimpleDiffRequest request, String original, String destination) {
        assertNotNull(request);
        assertEquals(original, ((com.intellij.diff.contents.DocumentContent) request.getContents().get(0)).getDocument().getText());
        assertEquals(destination, ((com.intellij.diff.contents.DocumentContent) request.getContents().get(1)).getDocument().getText());
        assertEquals(Boolean.TRUE, request.getUserData(com.intellij.diff.util.DiffUserDataKeys.FORCE_READ_ONLY));
    }
    private void doubleClick(JTable table, int column) {
        var bounds = table.getCellRect(0, column, true);
        var event = new java.awt.event.MouseEvent(table, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2, 2, false, java.awt.event.MouseEvent.BUTTON1);
        for (var listener : table.getMouseListeners())
            if (listener.getClass().getEnclosingClass() == ImportSourceModuleDialog.class) listener.mouseClicked(event);
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private void setAnalysis(Object owner, SourceModuleImport.Analysis value) throws Exception {
        var field = owner.getClass().getDeclaredField("analysis"); field.setAccessible(true); field.set(owner, value);
    }
}
