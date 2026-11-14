package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.importer.SourceModuleImport;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.nio.file.Files;
import java.util.*;

public class ImportSourceModuleDialogHeavyTest extends HeavyPlatformTestCase {
    public void testMissingDestinationIsLabelledAndCopyGuidanceTracksStagingAfterSorting() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var first = base.files().get(0);
            var second = base.files().get(1);
            var files = List.of(
                    new SourceModuleImport.FileChoice(first.source(), first.destination(), first.bytes(), null, false, false, "New"),
                    new SourceModuleImport.FileChoice(second.source(), second.destination(), second.bytes(), "", false, false, "Existing empty file"));
            dialog.acceptAnalysis(new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), files,
                    base.beans(), base.properties(), base.dependencies(), base.notes(), base.originalPom(), base.generatedInputs(),
                    base.managedFiles(), base.supportInputs(), base.identity()), base.sourceRoot());
            var table = (JTable) field(dialog, "table");
            var model = table.getModel();
            assertTrue(model.getValueAt(0, 2).toString().startsWith(
                    org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.new", first.destination())));
            assertFalse(model.getValueAt(1, 2).toString().startsWith(
                    org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.new", second.destination())));
            table.getRowSorter().toggleSortOrder(1);
            int row = table.convertRowIndexToView(0);
            table.setRowSelectionInterval(row, row);
            var details = (JTextArea) field(dialog, "fileDetails");
            assertTrue(details.getText().contains(org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.newHelp")));
            int destinationColumn = table.convertColumnIndexToView(2);
            var label = (JLabel) table.prepareRenderer(table.getCellRenderer(row, destinationColumn), row, destinationColumn);
            assertTrue(label.getToolTipText().contains(first.destination()));
            assertTrue(label.getToolTipText().contains(org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.newHelp")));
            model.setValueAt(Boolean.TRUE, 0, 0);
            assertTrue(details.getText().contains(org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.newSelected")));
            model.setValueAt(Boolean.FALSE, 0, 0);
            assertTrue(details.getText().contains(org.ikasan.studio.ui.StudioBundle.message("sourceImport.file.newHelp")));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

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
                List.of(), bytes, Map.of(), List.of(), Map.of(), identity(original, destination, "Example", "Example"));
        var dialog = new ImportSourceModuleDialog(getProject());
        try {
            dialog.acceptAnalysis(analysis, original);
            var table = (JTable) field(dialog, "table"); TableModel model = table.getModel();
            ((javax.swing.table.AbstractTableModel) model).fireTableDataChanged();
            assertEquals(5, model.getRowCount());
            assertEquals(Boolean.FALSE, model.getValueAt(0, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(1, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(2, 0));
            assertEquals(Boolean.TRUE, model.getValueAt(3, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(4, 0));
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.dependency.manual"), model.getValueAt(4, 2));
            assertFalse(model.isCellEditable(4, 0));
            int manualRow = table.convertRowIndexToView(4);
            var checkbox = table.getCellRenderer(manualRow, 1).getTableCellRendererComponent(table, Boolean.FALSE, false, false, manualRow, 1);
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
    public void testOriginalColumnsAndSortedComparisonsOfferEditableSupport() throws Exception {
        var dialog = previewDialog();
        try {
            var table = (JTable) field(dialog, "table");
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.evidence"), table.getColumnName(0));
            assertEquals("user/B.java", table.getModel().getValueAt(0, 2));
            table.getRowSorter().toggleSortOrder(1);
            table.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(table), "original A", "destination A");
            var support = (JTable) field(dialog, "supportTable");
            assertEquals(1, support.getRowCount());
            assertFalse(support.isCellEditable(0, 0)); assertTrue(support.isCellEditable(0, 1));
            support.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(support), "original support", "destination support");
            assertTrue(((JTextArea) field(dialog, "supportNotice")).isVisible());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.support.title"), ((JTabbedPane) field(dialog, "reviewTabs")).getTitleAt(1));
            assertEquals(2, table.getRowCount());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testDoubleClickAndEnterOpenDiffsWithoutChangingImportSelections() throws Exception {
        var dialog = previewDialog();
        var previews = new java.util.ArrayList<com.intellij.diff.requests.SimpleDiffRequest>();
        try (var service = org.mockito.Mockito.mockStatic(com.intellij.diff.DiffManager.class);
             var dialogs = org.mockito.Mockito.mockConstruction(SourceComparisonDialog.class,
                     (mock, context) -> previews.add((com.intellij.diff.requests.SimpleDiffRequest) context.arguments().get(2)))) {
            var manager = org.mockito.Mockito.mock(com.intellij.diff.DiffManager.class);
            service.when(com.intellij.diff.DiffManager::getInstance).thenReturn(manager);
            var table = (JTable) field(dialog, "table"); table.setSize(800, 200); table.doLayout();
            table.getRowSorter().toggleSortOrder(1);
            doubleClick(table, 1);
            org.mockito.Mockito.verifyNoInteractions(manager);
            doubleClick(table, 0);
            table.getActionMap().get("studio.compareSource").actionPerformed(new java.awt.event.ActionEvent(table, 0, "compare"));
            var support = (JTable) field(dialog, "supportTable"); support.setSize(800, 200); support.doLayout();
            doubleClick(support, 0);
            var requests = org.mockito.ArgumentCaptor.forClass(com.intellij.diff.requests.DiffRequest.class);
            org.mockito.Mockito.verify(manager, org.mockito.Mockito.never()).showDiff(org.mockito.Mockito.eq(getProject()), requests.capture());
            assertEquals(3, dialogs.constructed().size());
            assertEquals(3, previews.size());
            assertDiff(previews.get(0), "original A", "destination A");
            assertDiff(previews.get(2), "original support", "destination support");
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testComparisonSaveCommitsPreviewAndCancelDiscardsIt() throws Exception {
        var parent = previewDialog();
        try {
            var table = (JTable) field(parent, "table"); table.setRowSelectionInterval(0, 0);
            var request = requiredComparison(parent.comparison(table));
            var destination = ((com.intellij.diff.contents.DocumentContent) request.getContents().get(1)).getDocument();
            var cancelled = new SourceComparisonDialog(getProject(), parent.getContentPane(), request);
            var cancelledPreview = ((com.intellij.diff.contents.DocumentContent) field(cancelled, "preview")).getDocument();
            com.intellij.openapi.application.WriteAction.run(() -> cancelledPreview.setText("discard me"));
            cancelled.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertEquals("destination B", destination.getText());
            assertTrue(((Set<?>) field(parent, "selectedFiles")).isEmpty());
            var saved = new SourceComparisonDialog(getProject(), parent.getContentPane(), request);
            var savedPreview = ((com.intellij.diff.contents.DocumentContent) field(saved, "preview")).getDocument();
            com.intellij.openapi.application.WriteAction.run(() -> savedPreview.setText("keep me"));
            saved.doOKAction();
            assertEquals("keep me", destination.getText());
            assertTrue(((Set<?>) field(parent, "selectedFiles")).contains("src/B.java"));
        } finally { parent.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testSupportCopiesAndPartialEditsCanBeReverted() throws Exception {
        var dialog = previewDialog();
        try {
            var support = (JTable) field(dialog, "supportTable");
            support.setRowSelectionInterval(0, 0);
            var right = ((com.intellij.diff.contents.DocumentContent) requiredComparison(dialog.comparison(support)).getContents().get(1)).getDocument();
            assertTrue(right.isWritable());
            support.getModel().setValueAt(Boolean.TRUE, 0, 1);
            assertEquals("original support", right.getText());
            assertEquals(Boolean.TRUE, support.getValueAt(0, 1));
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            assertEquals("destination support", right.getText());
            com.intellij.openapi.application.WriteAction.run(() -> right.setText("destination support with debug note"));
            assertEquals(Boolean.TRUE, support.getValueAt(0, 1));
            var validation = dialog.getClass().getDeclaredMethod("doValidate"); validation.setAccessible(true);
            assertNull(validation.invoke(dialog));
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            assertEquals("destination support", right.getText());
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testGenerationPreservesExplicitlyCustomisedSupportFiles() throws Exception {
        var root = Files.createTempDirectory("studio-custom-support-generation");
        String debug = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
        String copy = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
        Files.createDirectories(root.resolve(debug).getParent()); Files.writeString(root.resolve(debug), "custom debug");
        Files.createDirectories(root.resolve(copy).getParent()); Files.writeString(root.resolve(copy), "custom copy");
        var marker = root.resolve(SourceModuleImport.SUPPORT_CUSTOMISATIONS);
        Files.createDirectories(marker.getParent()); Files.writeString(marker, debug + "\n" + copy + "\n");
        var project = org.mockito.Mockito.mock(com.intellij.openapi.project.Project.class);
        org.mockito.Mockito.when(project.getBasePath()).thenReturn(root.toString());
        try (var files = org.mockito.Mockito.mockStatic(org.ikasan.studio.intellij.project.StudioProjectFiles.class)) {
            var synchronizer = new org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer(project);
            var generate = synchronizer.getClass().getDeclaredMethod("saveDebugSupportClasses", com.intellij.openapi.project.Project.class,
                    org.ikasan.studio.core.model.ikasan.instance.Module.class);
            generate.setAccessible(true); generate.invoke(synchronizer, project, null);
            files.verifyNoInteractions();
            assertEquals("custom debug", Files.readString(root.resolve(debug)));
            assertEquals("custom copy", Files.readString(root.resolve(copy)));
        }
    }

    public void testSupportActionsAppearWithTheSelectedRowAndUpdateAfterCopyAndRevert() throws Exception {
        var dialog = previewDialog();
        try {
            var details = (JTextArea) field(dialog, "supportDetails");
            var pane = (JScrollPane) field(dialog, "supportDetailsPane");
            var support = (JTable) field(dialog, "supportTable");
            support.setRowSelectionInterval(0, 0);
            String comparison = org.ikasan.studio.ui.StudioBundle.message("sourceImport.reviewAction.SUPPORT_COMPARE", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java");
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Support action missing", () -> details.getText().contains(comparison), 10);
            assertTrue(pane.isVisible());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            support.getModel().setValueAt(Boolean.TRUE, 0, 1);
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Completed action not removed", () -> !pane.isVisible(), 10);
            assertEquals(0, support.getSelectedRow());
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Reverted action not restored", () -> details.getText().contains(comparison), 10);
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testBeanConfirmationsAreHiddenButWiringDecisionsAndScanProblemsRemainVisible() throws Exception {
        var dialog = previewDialog();
        try {
            var original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var wiring = original.files().get(1);
            var files = List.of(original.files().get(0), new SourceModuleImport.FileChoice(wiring.source(), wiring.destination(),
                    wiring.bytes(), wiring.previous(), false, true, "Review selected configuration"));
            var analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), files,
                    List.of(new SourceModuleImport.BeanRequirement("aBean", "A", "generated/Factory.java", 12, List.of("src/A.java:3 — @Bean makeA returns A")),
                            new SourceModuleImport.BeanRequirement("libraryBean", "example.LibraryType", "generated/Factory.java", 20, List.of())),
                    original.properties(), original.dependencies(), List.of("Cannot parse source; review manually: src/Broken.java"),
                    original.originalPom(), original.generatedInputs(), original.managedFiles(), original.supportInputs(), original.identity());
            dialog.acceptAnalysis(analysis, original.sourceRoot());
            var table = (JTable) field(dialog, "table");
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Unmatched scan issue missing", () -> table.getRowCount() == 3, 10);
            table.getRowSorter().toggleSortOrder(1);
            int fileRow = table.convertRowIndexToView(1);
            assertFalse(String.valueOf(table.getModel().getValueAt(1, 2)).contains(org.ikasan.studio.ui.StudioBundle.message("sourceImport.inline.attention")));
            table.setRowSelectionInterval(fileRow, fileRow);
            var details = (JTextArea) field(dialog, "fileDetails");
            assertFalse(details.getText().contains("aBean"));
            table.getModel().setValueAt(Boolean.TRUE, 1, 0);
            String wiringMessage = org.ikasan.studio.ui.StudioBundle.message("sourceImport.reviewAction.WIRING", wiring.destination());
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Selected wiring decision hidden", () -> details.getText().contains(wiringMessage), 10);
            int issueRow = table.convertRowIndexToView(2);
            table.setRowSelectionInterval(issueRow, issueRow);
            assertFalse(table.isCellEditable(issueRow, 1));
            assertNull(dialog.comparison(table));
            assertTrue(details.getText().contains("src/Broken.java"));
            ((JTextField) field(dialog, "source")).setText("/different");
            assertEquals(0, table.getRowCount());
            assertFalse(((JScrollPane) field(dialog, "fileDetailsPane")).isVisible());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testCopyAllAndRevertRestoreTheDestinationPreview() throws Exception {
        var dialog = previewDialog();
        try {
            var table = (JTable) field(dialog, "table");
            table.setRowSelectionInterval(0, 0);
            var request = requiredComparison(dialog.comparison(table));
            var right = ((com.intellij.diff.contents.DocumentContent) request.getContents().get(1)).getDocument();
            assertEquals("destination B", right.getText());
            table.getModel().setValueAt(Boolean.TRUE, 0, 0);
            assertEquals("original B", right.getText());
            assertEquals(Boolean.TRUE, table.getModel().getValueAt(0, 0));
            var validation = dialog.getClass().getDeclaredMethod("doValidate"); validation.setAccessible(true);
            assertNull(validation.invoke(dialog));
            table.getModel().setValueAt(Boolean.FALSE, 0, 0);
            assertEquals("destination B", right.getText());
            assertEquals(Boolean.FALSE, table.getModel().getValueAt(0, 0));
            com.intellij.openapi.application.WriteAction.run(() -> right.setText("partial B"));
            assertEquals(Boolean.TRUE, table.getModel().getValueAt(0, 0));
            table.getModel().setValueAt(Boolean.FALSE, 0, 0);
            assertEquals("destination B", right.getText());
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testPartialEditsAreStagedShareTheSamePreviewAndAreDiscardedOnCheckoutChange() throws Exception {
        var dialog = previewDialog();
        try {
            var table = (JTable) field(dialog, "table"); table.setRowSelectionInterval(1, 1);
            var request = requiredComparison(dialog.comparison(table));
            var right = (com.intellij.diff.contents.DocumentContent) request.getContents().get(1);
            assertTrue(right.getDocument().isWritable());
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("destination A\noriginal A"));
            assertEquals("destination A\noriginal A", ((Map<?, ?>) field(dialog, "stagedJava")).get("src/A.java"));
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).contains("src/A.java"));
            table.setRowSelectionInterval(1, 1);
            assertSame(right, requiredComparison(dialog.comparison(table)).getContents().get(1));
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("destination A"));
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
            assertFalse(((Set<?>) field(dialog, "selectedFiles")).contains("src/A.java"));
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("chosen merge"));
            ((JTextField) field(dialog, "source")).setText("/different-checkout");
            assertFalse(right.getDocument().isWritable());
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
            assertNull(field(dialog, "analysis")); assertFalse(dialog.isOKActionEnabled());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testEnteringAPathAnalysesAutomaticallyAndWrongPathsRemainAnInlineRecoverableState() throws Exception {
        var original = Files.createTempDirectory("studio-auto-identity");
        var destination = java.nio.file.Path.of(java.util.Objects.requireNonNull(getProject().getBasePath()));
        var model = destination.resolve("generated/src/main/model/model.json");
        var factory = destination.resolve("generated/src/main/java/studio/Factory.java");
        var pom = destination.resolve("pom.xml");
        Files.createDirectories(model.getParent()); Files.createDirectories(factory.getParent());
        Files.writeString(model, "{\"name\":\"Orders\"}");
        Files.writeString(factory, "package studio; class Factory { @javax.annotation.Resource org.customer.Custom custom; }");
        String pomText = "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>different-artifact</artifactId><version>1</version></project>";
        Files.writeString(pom, pomText);
        Files.createDirectories(original.resolve("src/main/java/org/customer"));
        Files.createDirectories(original.resolve("src/main/resources"));
        Files.writeString(original.resolve("pom.xml"), pomText);
        Files.writeString(original.resolve("src/main/java/org/customer/Custom.java"), "package org.customer; public class Custom {} ");
        Files.writeString(original.resolve("src/main/resources/application.properties"), "module.name=Orders\n");
        var dialog = new ImportSourceModuleDialog(getProject());
        try {
            var input = (JTextField) field(dialog, "source");
            input.setText(original.toString()); input.postActionEvent();
            var analysis = (SourceModuleImport.Analysis) field(dialog, "analysis");
            assertNotNull(analysis);
            assertFalse(((JTextArea) field(dialog, "identitySummary")).isVisible());
            assertEquals(org.ikasan.studio.core.importer.SourceCheckoutIdentity.Status.MATCH, analysis.identity().status());
            assertTrue(dialog.isOKActionEnabled());
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
            input.setText(original.resolve("missing").toString()); input.postActionEvent();
            assertNull(field(dialog, "analysis")); assertFalse(dialog.isOKActionEnabled());
            assertTrue(((JTextArea) field(dialog, "identitySummary")).isVisible());
            assertFalse(((JTextArea) field(dialog, "identitySummary")).getText().isBlank());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(model); Files.deleteIfExists(factory); Files.deleteIfExists(pom);
            com.intellij.openapi.util.io.FileUtil.delete(original.toFile());
        }
    }

    public void testStagedEditsRetainDestinationLineEndings() throws Exception {
        var dialog = previewDialog();
        try {
            var original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), List.of(
                    new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("old source\r\n"),
                            encoded("keep destination\r\n"), false, false, "Existing")), List.of(), List.of(), List.of(), List.of(), "",
                    Map.of(), List.of(), Map.of(), original.identity());
            dialog.acceptAnalysis(analysis, original.sourceRoot());
            var table = (JTable) field(dialog, "table"); table.setRowSelectionInterval(0, 0);
            var content = (com.intellij.diff.contents.DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1);
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(() -> content.getDocument().setText("keep destination\nselected debug\n"));
            assertEquals("keep destination\r\nselected debug\r\n", ((Map<?, ?>) field(dialog, "stagedJava")).get("src/A.java"));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testReactorChooserReturnsOnlyTheExplicitlySelectedCandidate() {
        var one = new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate("one", "artifact-one", List.of("one"), List.of(), false);
        var two = new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate("two", "artifact-two", List.of("two"), List.of(), false);
        var inspection = new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Inspection(java.nio.file.Path.of("/source"),
                java.nio.file.Path.of("/destination"), "Expected", List.of(one, two), List.of("one", "two"), List.of(), List.of());
        assertNull(inspection.automaticCandidate());
        var chooser = new SourceModuleSelectionDialog(getProject(), null, inspection);
        try {
            var candidates = chooser.getPreferredFocusedComponent();
            assertNotNull(candidates);
            ((JComboBox<?>) candidates).setSelectedIndex(1);
            assertEquals("two", chooser.selectedDirectory());
        } finally { chooser.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testIdenticalSupportIsHiddenButCommentsAndSortedComparisonsRemainAvailable() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            String copy = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
            String debug = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
            var support = List.of(
                    new SourceModuleImport.ManagedFile("src/A.java", copy, encoded("class A {}"), encoded("class A {}"), SourceModuleImport.SupportStatus.DESTINATION_DIFFERS),
                    new SourceModuleImport.ManagedFile("src/Z.java", debug, encoded("// useful debug note\nclass Z {}"), encoded("class Z {}"), SourceModuleImport.SupportStatus.SUPPLIED));
            dialog.acceptAnalysis(withSupport(base, support), base.sourceRoot());
            var table = (JTable) field(dialog, "supportTable");
            var show = (JCheckBox) field(dialog, "showIdenticalSupport");
            assertFalse(show.isSelected()); assertEquals(1, table.getRowCount());
            table.getRowSorter().toggleSortOrder(0);
            table.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(table), "// useful debug note\nclass Z {}", "class Z {}");
            show.doClick(); assertEquals(2, table.getRowCount());
            assertEquals(1, table.convertRowIndexToModel(table.getSelectedRow()));
            show.doClick(); assertEquals(1, table.getRowCount());
            assertEquals(1, table.convertRowIndexToModel(table.getSelectedRow()));
            assertDiff(dialog.comparison(table), "// useful debug note\nclass Z {}", "class Z {}");
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testSupportVerificationProblemsAndMissingDestinationsAreNeverHidden() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            String destination = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
            for (var status : List.of(SourceModuleImport.SupportStatus.UNVERIFIED, SourceModuleImport.SupportStatus.DESTINATION_MISSING)) {
                var support = new SourceModuleImport.ManagedFile("src/Debug.java", destination, encoded("class Debug {}"),
                        status == SourceModuleImport.SupportStatus.DESTINATION_MISSING ? null : encoded("class Debug {}"), status);
                dialog.acceptAnalysis(withSupport(base, List.of(support)), base.sourceRoot());
                assertEquals(1, ((JTable) field(dialog, "supportTable")).getRowCount());
            }
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testAllIdenticalSupportHasAnEmptyStateAndStagedEditsRemainVisibleUntilReverted() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var support = new SourceModuleImport.ManagedFile("src/Debug.java", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java",
                    encoded("class Debug {}"), encoded("class Debug {}"), SourceModuleImport.SupportStatus.SUPPLIED);
            dialog.acceptAnalysis(withSupport(base, List.of(support)), base.sourceRoot());
            var table = (com.intellij.ui.table.JBTable) field(dialog, "supportTable");
            var show = (JCheckBox) field(dialog, "showIdenticalSupport");
            assertEquals(0, table.getRowCount());
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.support.allIdentical"), table.getEmptyText().getText());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            show.doClick(); assertEquals(1, table.getRowCount());
            table.setRowSelectionInterval(0, 0);
            var right = ((com.intellij.diff.contents.DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1)).getDocument();
            com.intellij.openapi.application.WriteAction.run(() -> right.setText("// copied debug note\nclass Debug {}"));
            show.doClick(); assertEquals(1, table.getRowCount());
            assertEquals(Boolean.TRUE, table.getValueAt(0, 1));
            assertTrue(table.editCellAt(0, 1));
            ((JButton) table.getEditorComponent()).doClick();
            assertEquals(0, table.getRowCount());
            assertFalse(table.isEditing());
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testIdenticalClassesAreHiddenWhileDependenciesAndStagedEditsRemainVisible() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var dependency = new org.apache.maven.model.Dependency(); dependency.setGroupId("customer"); dependency.setArtifactId("client");
            var files = List.of(
                    new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("class A {}"), encoded("class A {}"), false, false, ""),
                    new SourceModuleImport.FileChoice("src/B.java", "user/B.java", encoded("// debug\nclass B {}"), encoded("class B {}"), false, false, ""));
            var analysis = new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), files, List.of(), List.of(),
                    List.of(new SourceModuleImport.DependencyChoice("customer:client:jar:", dependency, true, "Original declaration")),
                    List.of(), base.originalPom(), base.generatedInputs(), List.of(), base.supportInputs(), base.identity());
            dialog.acceptAnalysis(analysis, base.sourceRoot());
            var table = (com.intellij.ui.table.JBTable) field(dialog, "table");
            var show = (JCheckBox) field(dialog, "showIdenticalFiles");
            assertFalse(show.isSelected()); assertEquals(2, table.getRowCount());
            table.getRowSorter().toggleSortOrder(1);
            int changed = table.convertRowIndexToView(1); table.setRowSelectionInterval(changed, changed);
            assertDiff(dialog.comparison(table), "// debug\nclass B {}", "class B {}");
            show.doClick(); assertEquals(3, table.getRowCount());
            int identical = table.convertRowIndexToView(0); table.setRowSelectionInterval(identical, identical);
            var right = ((com.intellij.diff.contents.DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1)).getDocument();
            com.intellij.openapi.application.WriteAction.run(() -> right.setText("// staged\nclass A {}"));
            show.doClick(); assertEquals(3, table.getRowCount());
            identical = table.convertRowIndexToView(0);
            assertTrue(table.editCellAt(identical, 1)); ((JButton) table.getEditorComponent()).doClick();
            assertEquals(2, table.getRowCount()); assertEquals(-1, table.convertRowIndexToView(0));
            assertFalse(table.isEditing());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testBeanConfirmationDoesNotCreateAnExtraRowForAnIdenticalClass() throws Exception {
        var dialog = previewDialog();
        try {
            var base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            var file = new SourceModuleImport.FileChoice("src/A.java", "user/src/main/java/A.java", encoded("class A {}"), encoded("class A {}"), false, false, "");
            var analysis = new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), List.of(file),
                    List.of(new SourceModuleImport.BeanRequirement("aBean", "A", "generated/Factory.java", 1, List.of())),
                    List.of(), List.of(), List.of(), base.originalPom(), base.generatedInputs(), List.of(), base.supportInputs(), base.identity());
            dialog.acceptAnalysis(analysis, base.sourceRoot());
            var table = (JTable) field(dialog, "table");
            var attach = dialog.getClass().getDeclaredMethod("attachActions", List.class);
            attach.setAccessible(true);
            var actions = SourceModuleImport.reviewActions(analysis, Set.of(), Set.of());
            assertTrue(actions.stream().anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN));
            attach.invoke(dialog, actions);
            assertEquals(0, table.getRowCount());
            ((JCheckBox) field(dialog, "showIdenticalFiles")).doClick();
            assertEquals(1, table.getRowCount());
            table.setRowSelectionInterval(0, 0);
            assertFalse(((JTextArea) field(dialog, "fileDetails")).getText().contains("aBean"));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private SourceModuleImport.Analysis withSupport(SourceModuleImport.Analysis base, List<SourceModuleImport.ManagedFile> support) {
        return new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), base.files(), base.beans(), base.properties(),
                base.dependencies(), base.notes(), base.originalPom(), base.generatedInputs(), support, base.supportInputs(), base.identity());
    }

    private ImportSourceModuleDialog previewDialog() {
        var dialog = new ImportSourceModuleDialog(getProject());
        var analysis = new SourceModuleImport.Analysis(java.nio.file.Path.of("/original"), java.nio.file.Path.of("/destination"), List.of(
                new SourceModuleImport.FileChoice("src/B.java", "user/B.java", encoded("original B"), encoded("destination B"), false, false, "Review"),
                new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("original A"), encoded("destination A"), false, false, "Review")),
                List.of(), List.of(), List.of(), List.of(), "", Map.of(),
                List.of(new SourceModuleImport.ManagedFile("src/DebugTransitionComponent.java", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java",
                        encoded("original support"), encoded("destination support"), SourceModuleImport.SupportStatus.ORIGINAL_DIFFERS)), Map.of(),
                identity(java.nio.file.Path.of("/original"), java.nio.file.Path.of("/destination"), "Example", "Example"));
        dialog.acceptAnalysis(analysis, analysis.sourceRoot());
        return dialog;
    }
    private String encoded(String text) { return Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private void assertDiff(com.intellij.diff.requests.SimpleDiffRequest request, String original, String destination) {
        assertNotNull(request);
        assertEquals(original, ((com.intellij.diff.contents.DocumentContent) request.getContents().get(0)).getDocument().getText());
        assertEquals(destination, ((com.intellij.diff.contents.DocumentContent) request.getContents().get(1)).getDocument().getText());
        assertFalse(((com.intellij.diff.contents.DocumentContent) request.getContents().get(0)).getDocument().isWritable());
        if (Boolean.TRUE.equals(request.getUserData(com.intellij.diff.util.DiffUserDataKeys.FORCE_READ_ONLY)))
            assertFalse(((com.intellij.diff.contents.DocumentContent) request.getContents().get(1)).getDocument().isWritable());
    }
    private void doubleClick(JTable table, int column) {
        var bounds = table.getCellRect(0, column, true);
        var event = new java.awt.event.MouseEvent(table, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2, 2, false, java.awt.event.MouseEvent.BUTTON1);
        for (var listener : table.getMouseListeners())
            if (listener.getClass().getEnclosingClass() == ImportSourceModuleDialog.class) listener.mouseClicked(event);
    }

    private org.ikasan.studio.core.importer.SourceCheckoutIdentity.Check identity(java.nio.file.Path source,
            java.nio.file.Path destination, String expected, String actual) {
        var candidate = new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate("", "artifact-hint", List.of(""),
                actual.isEmpty() ? List.of() : List.of(new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Declaration("application.properties", actual)), false);
        var inspection = new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Inspection(source, destination, expected,
                List.of(candidate), List.of(""), List.of(), List.of());
        return new org.ikasan.studio.core.importer.SourceCheckoutIdentity.Check(inspection, candidate, List.of());
    }

    public void testMismatchAndUnverifiedCheckoutRequireApprovalBeforeDefaultSelectionAndImport() throws Exception {
        var dialog = previewDialog();
        try {
            var original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            for (String actual : List.of("WrongModule", "")) {
                var check = identity(original.sourceRoot(), original.destinationRoot(), "ExpectedModule", actual);
                var analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), List.of(
                        new SourceModuleImport.FileChoice("src/New.java", "user/New.java", encoded("source"), null, true, false, "New")),
                        List.of(), List.of(), List.of(), List.of(), "", Map.of(), List.of(), Map.of(), check);
                dialog.acceptAnalysis(analysis, original.sourceRoot());
                var approval = (JCheckBox) field(dialog, "acceptIdentity");
                var table = (JTable) field(dialog, "table");
                assertTrue(approval.isVisible()); assertFalse(approval.isSelected());
                assertFalse(dialog.isOKActionEnabled());
                assertFalse(table.isCellEditable(0, 1)); assertEquals(Boolean.FALSE, table.getValueAt(0, 1));
                var validate = dialog.getClass().getDeclaredMethod("doValidate"); validate.setAccessible(true);
                assertNotNull(validate.invoke(dialog));
                assertTrue(((JTextArea) field(dialog, "identitySummary")).getText().contains("ExpectedModule"));
                approval.doClick();
                assertTrue(dialog.isOKActionEnabled()); assertTrue(table.isCellEditable(0, 1));
                assertEquals(Boolean.FALSE, table.getValueAt(0, 1)); assertNull(validate.invoke(dialog));
                approval.doClick();
                assertFalse(dialog.isOKActionEnabled()); assertEquals(Boolean.FALSE, table.getValueAt(0, 1));
            }
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private com.intellij.diff.requests.SimpleDiffRequest requiredComparison(
            com.intellij.diff.requests.SimpleDiffRequest request) {
        assertNotNull(request);
        return Objects.requireNonNull(request);
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }

}
