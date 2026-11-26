package org.ikasan.studio.intellij.importer;

import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Check;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Declaration;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Inspection;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Status;
import org.ikasan.studio.core.importer.SourceModuleImport.Analysis;
import org.ikasan.studio.core.importer.SourceModuleImport.FileChoice;
import org.ikasan.studio.core.importer.SourceModuleImport.ManagedFile;
import org.ikasan.studio.core.importer.SourceModuleImport.ReviewAction;
import org.ikasan.studio.core.importer.SourceModuleImport.SpringRegistration;
import org.ikasan.studio.core.importer.SourceModuleImport.SupportStatus;
import org.ikasan.studio.core.importer.SourceModuleImport;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ImportSourceModuleDialogHeavyTest extends HeavyPlatformTestCase {
    public void testMissingDestinationIsLabelledAndCopyGuidanceTracksStagingAfterSorting() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            FileChoice first = base.files().get(0);
            FileChoice second = base.files().get(1);
            List<FileChoice> files = List.of(
                    new SourceModuleImport.FileChoice(first.source(), first.destination(), first.bytes(), null, false, false, "New"),
                    new SourceModuleImport.FileChoice(second.source(), second.destination(), second.bytes(), "", false, false, "Existing empty file"));
            dialog.acceptAnalysis(new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), files,
                    base.beans(), base.properties(), base.dependencies(), base.notes(), base.originalPom(), base.generatedInputs(),
                    base.managedFiles(), base.supportInputs(), base.identity()), base.sourceRoot());
            JTable table = (JTable) field(dialog, "table");
            TableModel model = table.getModel();
            assertTrue(model.getValueAt(0, 2).toString().startsWith(
                    StudioBundle.message("sourceImport.file.new", first.destination())));
            assertFalse(model.getValueAt(1, 2).toString().startsWith(
                    StudioBundle.message("sourceImport.file.new", second.destination())));
            table.getRowSorter().toggleSortOrder(1);
            int row = table.convertRowIndexToView(0);
            table.setRowSelectionInterval(row, row);
            JTextArea details = (JTextArea) field(dialog, "fileDetails");
            assertTrue(details.getText().contains(StudioBundle.message("sourceImport.file.newHelp")));
            int destinationColumn = table.convertColumnIndexToView(2);
            JLabel label = (JLabel) table.prepareRenderer(table.getCellRenderer(row, destinationColumn), row, destinationColumn);
            assertTrue(label.getToolTipText().contains(first.destination()));
            assertTrue(label.getToolTipText().contains(StudioBundle.message("sourceImport.file.newHelp")));
            model.setValueAt(Boolean.TRUE, 0, 0);
            assertTrue(details.getText().contains(StudioBundle.message("sourceImport.file.newSelected")));
            model.setValueAt(Boolean.FALSE, 0, 0);
            assertTrue(details.getText().contains(StudioBundle.message("sourceImport.file.newHelp")));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testSourceReviewHonoursThePropertiesStepChosenInTheParentJourney() throws Exception {
        for (boolean selected : new boolean[]{false, true}) {
            ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject(), selected);
            try { assertEquals(selected, ((JCheckBox) field(dialog, "properties")).isSelected()); }
            finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
        }
    }

    public void testAnalysisDefaultsNewCodeAndDependenciesButProtectsExistingCodeWiringAndManualItemsAndCancelDoesNotWrite() throws Exception {
        Path root = Files.createTempDirectory("studio-source-wizard");
        Path original = root.resolve("original"); Path destination = root.resolve("studio");
        Files.createDirectories(original); Files.createDirectories(destination);
        String bytes = Base64.getEncoder().encodeToString("class Bespoke {}".getBytes(StandardCharsets.UTF_8));
        Dependency dependency = new Dependency(); dependency.setGroupId("customer"); dependency.setArtifactId("client"); dependency.setVersion("1");
        Dependency local = dependency.clone(); local.setArtifactId("local"); local.setScope("system"); local.setSystemPath("/original/local.jar");
        Analysis analysis = new SourceModuleImport.Analysis(original, destination, List.of(
                new SourceModuleImport.FileChoice("src/main/java/Bespoke.java", "user/src/main/java/Bespoke.java", bytes, null, true, false, "Matched"),
                new SourceModuleImport.FileChoice("src/main/java/Config.java", "user/src/main/java/Config.java", bytes, null, false, true, "Review wiring"),
                new SourceModuleImport.FileChoice("src/main/java/Existing.java", "user/src/main/java/Existing.java", bytes, bytes, true, false, "Existing")),
                List.of(), List.of(), List.of(new SourceModuleImport.DependencyChoice("customer:client:jar:", dependency, true, "Original declaration"),
                        new SourceModuleImport.DependencyChoice("customer:local:jar:", local, false, "Non-portable system path")),
                List.of(), bytes, Map.of(), List.of(), Map.of(), identity(original, destination, "Example", "Example"));
        ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject());
        try {
            dialog.acceptAnalysis(analysis, original);
            JTable table = (JTable) field(dialog, "table"); TableModel model = table.getModel();
            ((AbstractTableModel) model).fireTableDataChanged();
            assertEquals(5, model.getRowCount());
            assertEquals(Boolean.FALSE, model.getValueAt(0, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(1, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(2, 0));
            assertEquals(Boolean.TRUE, model.getValueAt(3, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(4, 0));
            assertEquals(StudioBundle.message("sourceImport.dependency.manual"), model.getValueAt(4, 2));
            assertFalse(model.isCellEditable(4, 0));
            int manualRow = table.convertRowIndexToView(4);
            Component checkbox = table.getCellRenderer(manualRow, 1).getTableCellRendererComponent(table, Boolean.FALSE, false, false, manualRow, 1);
            assertFalse(checkbox.isEnabled());
            model.setValueAt(Boolean.TRUE, 0, 0);
            assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
            Method validate = dialog.getClass().getDeclaredMethod("doValidate"); validate.setAccessible(true);
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
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            JTable table = (JTable) field(dialog, "table");
            assertEquals(StudioBundle.message("sourceImport.evidence"), table.getColumnName(0));
            assertEquals("user/B.java", table.getModel().getValueAt(0, 2));
            table.getRowSorter().toggleSortOrder(1);
            table.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(table), "original A", "destination A");
            JTable support = (JTable) field(dialog, "supportTable");
            assertEquals(1, support.getRowCount());
            assertFalse(support.isCellEditable(0, 0)); assertTrue(support.isCellEditable(0, 1));
            support.setRowSelectionInterval(0, 0);
            assertDiff(dialog.comparison(support), "original support", "destination support");
            assertTrue(((JTextArea) field(dialog, "supportNotice")).isVisible());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            assertEquals(StudioBundle.message("sourceImport.support.title"), ((JTabbedPane) field(dialog, "reviewTabs")).getTitleAt(1));
            assertEquals(2, table.getRowCount());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testDoubleClickAndEnterOpenDiffsWithoutChangingImportSelections() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        ArrayList<SimpleDiffRequest> previews = new ArrayList<>();
        try (MockedStatic<DiffManager> service = Mockito.mockStatic(DiffManager.class);
             MockedConstruction<SourceComparisonDialog> dialogs = Mockito.mockConstruction(SourceComparisonDialog.class,
                     (mock, context) -> previews.add((SimpleDiffRequest) context.arguments().get(2)))) {
            DiffManager manager = Mockito.mock(DiffManager.class);
            service.when(DiffManager::getInstance).thenReturn(manager);
            JTable table = (JTable) field(dialog, "table"); table.setSize(800, 200); table.doLayout();
            table.getRowSorter().toggleSortOrder(1);
            doubleClick(table, 1);
            Mockito.verifyNoInteractions(manager);
            doubleClick(table, 0);
            table.getActionMap().get("studio.compareSource").actionPerformed(new ActionEvent(table, 0, "compare"));
            JTable support = (JTable) field(dialog, "supportTable"); support.setSize(800, 200); support.doLayout();
            doubleClick(support, 0);
            ArgumentCaptor<DiffRequest> requests = ArgumentCaptor.forClass(DiffRequest.class);
            Mockito.verify(manager, Mockito.never()).showDiff(Mockito.eq(getProject()), requests.capture());
            assertEquals(3, dialogs.constructed().size());
            assertEquals(3, previews.size());
            assertDiff(previews.get(0), "original A", "destination A");
            assertDiff(previews.get(2), "original support", "destination support");
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testComparisonSaveCommitsPreviewAndCancelDiscardsIt() throws Exception {
        ImportSourceModuleDialog parent = previewDialog();
        try {
            JTable table = (JTable) field(parent, "table"); table.setRowSelectionInterval(0, 0);
            SimpleDiffRequest request = requiredComparison(parent.comparison(table));
            Document destination = ((DocumentContent) request.getContents().get(1)).getDocument();
            SourceComparisonDialog cancelled = new SourceComparisonDialog(getProject(), parent.getContentPane(), request);
            Document cancelledPreview = ((DocumentContent) field(cancelled, "preview")).getDocument();
            WriteAction.run(() -> cancelledPreview.setText("discard me"));
            cancelled.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertEquals("destination B", destination.getText());
            assertTrue(((Set<?>) field(parent, "selectedFiles")).isEmpty());
            SourceComparisonDialog saved = new SourceComparisonDialog(getProject(), parent.getContentPane(), request);
            Document savedPreview = ((DocumentContent) field(saved, "preview")).getDocument();
            WriteAction.run(() -> savedPreview.setText("keep me"));
            saved.doOKAction();
            assertEquals("keep me", destination.getText());
            assertTrue(((Set<?>) field(parent, "selectedFiles")).contains("src/B.java"));
        } finally { parent.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testSupportCopiesAndPartialEditsCanBeReverted() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            JTable support = (JTable) field(dialog, "supportTable");
            support.setRowSelectionInterval(0, 0);
            Document right = ((DocumentContent) requiredComparison(dialog.comparison(support)).getContents().get(1)).getDocument();
            assertTrue(right.isWritable());
            support.getModel().setValueAt(Boolean.TRUE, 0, 1);
            assertEquals("original support", right.getText());
            assertEquals(Boolean.TRUE, support.getValueAt(0, 1));
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            assertEquals("destination support", right.getText());
            WriteAction.run(() -> right.setText("destination support with debug note"));
            assertEquals(Boolean.TRUE, support.getValueAt(0, 1));
            Method validation = dialog.getClass().getDeclaredMethod("doValidate"); validation.setAccessible(true);
            assertNull(validation.invoke(dialog));
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            assertEquals("destination support", right.getText());
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testGenerationPreservesExplicitlyCustomisedSupportFiles() throws Exception {
        Path root = Files.createTempDirectory("studio-custom-support-generation");
        String debug = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
        String copy = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
        Files.createDirectories(root.resolve(debug).getParent()); Files.writeString(root.resolve(debug), "custom debug");
        Files.createDirectories(root.resolve(copy).getParent()); Files.writeString(root.resolve(copy), "custom copy");
        Path marker = root.resolve(SourceModuleImport.SUPPORT_CUSTOMISATIONS);
        Files.createDirectories(marker.getParent()); Files.writeString(marker, debug + "\n" + copy + "\n");
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn(root.toString());
        try (MockedStatic<StudioProjectFiles> files = Mockito.mockStatic(StudioProjectFiles.class)) {
            files.when(() -> StudioProjectFiles.getApplicationBasePath(project)).thenReturn(root.toString());
            GeneratedProjectSynchronizer synchronizer = new GeneratedProjectSynchronizer(project);
            Method generate = synchronizer.getClass().getDeclaredMethod("saveDebugSupportClasses", Project.class,
                    Module.class);
            generate.setAccessible(true); generate.invoke(synchronizer, project, null);
            files.verify(() -> StudioProjectFiles.getApplicationBasePath(project), Mockito.times(2));
            files.verifyNoMoreInteractions();
            assertEquals("custom debug", Files.readString(root.resolve(debug)));
            assertEquals("custom copy", Files.readString(root.resolve(copy)));
        }
    }

    public void testSupportActionsAppearWithTheSelectedRowAndUpdateAfterCopyAndRevert() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            JTextArea details = (JTextArea) field(dialog, "supportDetails");
            JScrollPane pane = (JScrollPane) field(dialog, "supportDetailsPane");
            JTable support = (JTable) field(dialog, "supportTable");
            support.setRowSelectionInterval(0, 0);
            String comparison = StudioBundle.message("sourceImport.reviewAction.SUPPORT_COMPARE", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java");
            PlatformTestUtil.waitWithEventsDispatching("Support action missing", () -> details.getText().contains(comparison), 10);
            assertTrue(pane.isVisible());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            support.getModel().setValueAt(Boolean.TRUE, 0, 1);
            PlatformTestUtil.waitWithEventsDispatching("Completed action not removed", () -> !pane.isVisible(), 10);
            assertEquals(0, support.getSelectedRow());
            support.getModel().setValueAt(Boolean.FALSE, 0, 1);
            PlatformTestUtil.waitWithEventsDispatching("Reverted action not restored", () -> details.getText().contains(comparison), 10);
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testBeanConfirmationsAreHiddenButWiringDecisionsAndScanProblemsRemainVisible() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            FileChoice wiring = original.files().get(1);
            List<FileChoice> files = List.of(original.files().get(0), new SourceModuleImport.FileChoice(wiring.source(), wiring.destination(),
                    wiring.bytes(), wiring.previous(), false, true, "Review selected configuration"));
            Analysis analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), files,
                    List.of(new SourceModuleImport.BeanRequirement("aBean", "A", "generated/Factory.java", 12, List.of("src/A.java:3 — @Bean makeA returns A")),
                            new SourceModuleImport.BeanRequirement("libraryBean", "example.LibraryType", "generated/Factory.java", 20, List.of())),
                    original.properties(), original.dependencies(), List.of("Cannot parse source; review manually: src/Broken.java"),
                    original.originalPom(), original.generatedInputs(), original.managedFiles(), original.supportInputs(), original.identity());
            dialog.acceptAnalysis(analysis, original.sourceRoot());
            JTable table = (JTable) field(dialog, "table");
            PlatformTestUtil.waitWithEventsDispatching("Unmatched scan issue missing", () -> table.getRowCount() == 3, 10);
            table.getRowSorter().toggleSortOrder(1);
            int fileRow = table.convertRowIndexToView(1);
            assertFalse(String.valueOf(table.getModel().getValueAt(1, 2)).contains(StudioBundle.message("sourceImport.inline.attention")));
            table.setRowSelectionInterval(fileRow, fileRow);
            JTextArea details = (JTextArea) field(dialog, "fileDetails");
            assertFalse(details.getText().contains("aBean"));
            table.getModel().setValueAt(Boolean.TRUE, 1, 0);
            String wiringMessage = StudioBundle.message("sourceImport.reviewAction.WIRING", wiring.destination());
            PlatformTestUtil.waitWithEventsDispatching("Selected wiring decision hidden", () -> details.getText().contains(wiringMessage), 10);
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
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            JTable table = (JTable) field(dialog, "table");
            table.setRowSelectionInterval(0, 0);
            SimpleDiffRequest request = requiredComparison(dialog.comparison(table));
            Document right = ((DocumentContent) request.getContents().get(1)).getDocument();
            assertEquals("destination B", right.getText());
            table.getModel().setValueAt(Boolean.TRUE, 0, 0);
            assertEquals("original B", right.getText());
            assertEquals(Boolean.TRUE, table.getModel().getValueAt(0, 0));
            Method validation = dialog.getClass().getDeclaredMethod("doValidate"); validation.setAccessible(true);
            assertNull(validation.invoke(dialog));
            table.getModel().setValueAt(Boolean.FALSE, 0, 0);
            assertEquals("destination B", right.getText());
            assertEquals(Boolean.FALSE, table.getModel().getValueAt(0, 0));
            WriteAction.run(() -> right.setText("partial B"));
            assertEquals(Boolean.TRUE, table.getModel().getValueAt(0, 0));
            table.getModel().setValueAt(Boolean.FALSE, 0, 0);
            assertEquals("destination B", right.getText());
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testPartialEditsAreStagedShareTheSamePreviewAndAreDiscardedOnCheckoutChange() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            JTable table = (JTable) field(dialog, "table"); table.setRowSelectionInterval(1, 1);
            SimpleDiffRequest request = requiredComparison(dialog.comparison(table));
            DocumentContent right = (DocumentContent) request.getContents().get(1);
            assertTrue(right.getDocument().isWritable());
            ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("destination A\noriginal A"));
            assertEquals("destination A\noriginal A", ((Map<?, ?>) field(dialog, "stagedJava")).get("src/A.java"));
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).contains("src/A.java"));
            table.setRowSelectionInterval(1, 1);
            assertSame(right, requiredComparison(dialog.comparison(table)).getContents().get(1));
            ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("destination A"));
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
            assertFalse(((Set<?>) field(dialog, "selectedFiles")).contains("src/A.java"));
            ApplicationManager.getApplication().runWriteAction(() -> right.getDocument().setText("chosen merge"));
            ((JTextField) field(dialog, "source")).setText("/different-checkout");
            assertFalse(right.getDocument().isWritable());
            assertTrue(((Map<?, ?>) field(dialog, "stagedJava")).isEmpty());
            assertNull(field(dialog, "analysis")); assertFalse(dialog.isOKActionEnabled());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testEnteringAPathAnalysesAutomaticallyAndWrongPathsRemainAnInlineRecoverableState() throws Exception {
        Path original = Files.createTempDirectory("studio-auto-identity");
        Path destination = Path.of(Objects.requireNonNull(getProject().getBasePath()));
        Path model = destination.resolve("generated/src/main/model/model.json");
        Path factory = destination.resolve("generated/src/main/java/studio/Factory.java");
        Path pom = destination.resolve("pom.xml");
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
        ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject());
        try {
            JTextField input = (JTextField) field(dialog, "source");
            input.setText(original.toString()); input.postActionEvent();
            Analysis analysis = (SourceModuleImport.Analysis) field(dialog, "analysis");
            assertNotNull(analysis);
            assertFalse(((JTextArea) field(dialog, "identitySummary")).isVisible());
            assertEquals(Status.MATCH, analysis.identity().status());
            assertTrue(dialog.isOKActionEnabled());
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
            input.setText(original.resolve("missing").toString()); input.postActionEvent();
            assertNull(field(dialog, "analysis")); assertFalse(dialog.isOKActionEnabled());
            assertTrue(((JTextArea) field(dialog, "identitySummary")).isVisible());
            assertFalse(((JTextArea) field(dialog, "identitySummary")).getText().isBlank());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(model); Files.deleteIfExists(factory); Files.deleteIfExists(pom);
            FileUtil.delete(original.toFile());
        }
    }

    public void testStagedEditsRetainDestinationLineEndings() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            Analysis analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), List.of(
                    new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("old source\r\n"),
                            encoded("keep destination\r\n"), false, false, "Existing")), List.of(), List.of(), List.of(), List.of(), "",
                    Map.of(), List.of(), Map.of(), original.identity());
            dialog.acceptAnalysis(analysis, original.sourceRoot());
            JTable table = (JTable) field(dialog, "table"); table.setRowSelectionInterval(0, 0);
            DocumentContent content = (DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1);
            ApplicationManager.getApplication().runWriteAction(() -> content.getDocument().setText("keep destination\nselected debug\n"));
            assertEquals("keep destination\r\nselected debug\r\n", ((Map<?, ?>) field(dialog, "stagedJava")).get("src/A.java"));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testReactorChooserReturnsOnlyTheExplicitlySelectedCandidate() {
        Candidate one = new Candidate("one", "artifact-one", List.of("one"), List.of(), false);
        Candidate two = new Candidate("two", "artifact-two", List.of("two"), List.of(), false);
        Inspection inspection = new Inspection(Path.of("/source"),
                Path.of("/destination"), "Expected", List.of(one, two), List.of("one", "two"), List.of(), List.of());
        assertNull(inspection.automaticCandidate());
        SourceModuleSelectionDialog chooser = new SourceModuleSelectionDialog(getProject(), null, inspection);
        try {
            JComponent candidates = chooser.getPreferredFocusedComponent();
            assertNotNull(candidates);
            ((JComboBox<?>) candidates).setSelectedIndex(1);
            assertEquals("two", chooser.selectedDirectory());
        } finally { chooser.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testIdenticalSupportIsHiddenButCommentsAndSortedComparisonsRemainAvailable() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            String copy = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
            String debug = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
            List<ManagedFile> support = List.of(
                    new SourceModuleImport.ManagedFile("src/A.java", copy, encoded("class A {}"), encoded("class A {}"), SourceModuleImport.SupportStatus.DESTINATION_DIFFERS),
                    new SourceModuleImport.ManagedFile("src/Z.java", debug, encoded("// useful debug note\nclass Z {}"), encoded("class Z {}"), SourceModuleImport.SupportStatus.SUPPLIED));
            dialog.acceptAnalysis(withSupport(base, support), base.sourceRoot());
            JTable table = (JTable) field(dialog, "supportTable");
            JCheckBox show = (JCheckBox) field(dialog, "showIdenticalSupport");
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
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            String destination = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
            for (SupportStatus status : List.of(SourceModuleImport.SupportStatus.UNVERIFIED, SourceModuleImport.SupportStatus.DESTINATION_MISSING)) {
                ManagedFile support = new SourceModuleImport.ManagedFile("src/Debug.java", destination, encoded("class Debug {}"),
                        status == SourceModuleImport.SupportStatus.DESTINATION_MISSING ? null : encoded("class Debug {}"), status);
                dialog.acceptAnalysis(withSupport(base, List.of(support)), base.sourceRoot());
                assertEquals(1, ((JTable) field(dialog, "supportTable")).getRowCount());
            }
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testAllIdenticalSupportHasAnEmptyStateAndStagedEditsRemainVisibleUntilReverted() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            ManagedFile support = new SourceModuleImport.ManagedFile("src/Debug.java", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java",
                    encoded("class Debug {}"), encoded("class Debug {}"), SourceModuleImport.SupportStatus.SUPPLIED);
            dialog.acceptAnalysis(withSupport(base, List.of(support)), base.sourceRoot());
            JBTable table = (JBTable) field(dialog, "supportTable");
            JCheckBox show = (JCheckBox) field(dialog, "showIdenticalSupport");
            assertEquals(0, table.getRowCount());
            assertEquals(StudioBundle.message("sourceImport.support.allIdentical"), table.getEmptyText().getText());
            assertEquals(2, ((JTabbedPane) field(dialog, "reviewTabs")).getTabCount());
            show.doClick(); assertEquals(1, table.getRowCount());
            table.setRowSelectionInterval(0, 0);
            Document right = ((DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1)).getDocument();
            WriteAction.run(() -> right.setText("// copied debug note\nclass Debug {}"));
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
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            Dependency dependency = new Dependency(); dependency.setGroupId("customer"); dependency.setArtifactId("client");
            List<FileChoice> files = List.of(
                    new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("class A {}"), encoded("class A {}"), false, false, ""),
                    new SourceModuleImport.FileChoice("src/B.java", "user/B.java", encoded("// debug\nclass B {}"), encoded("class B {}"), false, false, ""));
            Analysis analysis = new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), files, List.of(), List.of(),
                    List.of(new SourceModuleImport.DependencyChoice("customer:client:jar:", dependency, true, "Original declaration")),
                    List.of(), base.originalPom(), base.generatedInputs(), List.of(), base.supportInputs(), base.identity());
            dialog.acceptAnalysis(analysis, base.sourceRoot());
            JBTable table = (JBTable) field(dialog, "table");
            JCheckBox show = (JCheckBox) field(dialog, "showIdenticalFiles");
            assertFalse(show.isSelected()); assertEquals(2, table.getRowCount());
            table.getRowSorter().toggleSortOrder(1);
            int changed = table.convertRowIndexToView(1); table.setRowSelectionInterval(changed, changed);
            assertDiff(dialog.comparison(table), "// debug\nclass B {}", "class B {}");
            show.doClick(); assertEquals(3, table.getRowCount());
            int identical = table.convertRowIndexToView(0); table.setRowSelectionInterval(identical, identical);
            Document right = ((DocumentContent) requiredComparison(dialog.comparison(table)).getContents().get(1)).getDocument();
            WriteAction.run(() -> right.setText("// staged\nclass A {}"));
            show.doClick(); assertEquals(3, table.getRowCount());
            identical = table.convertRowIndexToView(0);
            assertTrue(table.editCellAt(identical, 1)); ((JButton) table.getEditorComponent()).doClick();
            assertEquals(2, table.getRowCount()); assertEquals(-1, table.convertRowIndexToView(0));
            assertFalse(table.isEditing());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testBeanConfirmationDoesNotCreateAnExtraRowForAnIdenticalClass() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis base = (SourceModuleImport.Analysis) field(dialog, "analysis");
            FileChoice file = new SourceModuleImport.FileChoice("src/A.java", "user/src/main/java/A.java", encoded("class A {}"), encoded("class A {}"), false, false, "");
            Analysis analysis = new SourceModuleImport.Analysis(base.sourceRoot(), base.destinationRoot(), List.of(file),
                    List.of(new SourceModuleImport.BeanRequirement("aBean", "A", "generated/Factory.java", 1, List.of())),
                    List.of(), List.of(), List.of(), base.originalPom(), base.generatedInputs(), List.of(), base.supportInputs(), base.identity());
            dialog.acceptAnalysis(analysis, base.sourceRoot());
            JTable table = (JTable) field(dialog, "table");
            Method attach = dialog.getClass().getDeclaredMethod("attachActions", List.class);
            attach.setAccessible(true);
            List<ReviewAction> actions = SourceModuleImport.reviewActions(analysis, Set.of(), Set.of());
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

    @SuppressWarnings("unchecked")
    public void testSpringRegistrationIsOfferedAfterCopyAndCanBeStagedAndReverted() throws Exception {
        Path destination = Files.createTempDirectory("studio-registration-preview");
        ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject());
        String source = "src/main/java/org/original/Copied.java";
        Analysis analysis = new SourceModuleImport.Analysis(Path.of("/original"), destination,
                List.of(new SourceModuleImport.FileChoice(source, "user/src/main/java/org/original/Copied.java",
                        encoded("package org.original; @org.springframework.stereotype.Component public class Copied {}"),
                        null, true, false, "Referenced component")),
                List.of(new SourceModuleImport.BeanRequirement("copied", "org.original.Copied", "generated/Factory.java", 1, List.of())),
                List.of(), List.of(), List.of(), "", Map.of("generated/Application.java", encoded(
                        "package org.ikasan.studio.boot; @org.springframework.context.annotation.ComponentScan(\"org.ikasan.studio.boot\") class Application {}")),
                List.of(), Map.of(), identity(Path.of("/original"), destination, "Example", "Example"));
        try {
            dialog.acceptAnalysis(analysis, analysis.sourceRoot());
            JPanel panel = (JPanel) field(dialog, "registrationPanel");
            assertFalse(panel.isVisible());
            ((Set<String>) field(dialog, "selectedFiles")).add(source);
            Method refresh = dialog.getClass().getDeclaredMethod("refreshReview"); refresh.setAccessible(true); refresh.invoke(dialog);
            PlatformTestUtil.waitWithEventsDispatching("Spring registration was not offered", panel::isVisible, 10);
            SpringRegistration candidate = (SourceModuleImport.SpringRegistration) field(dialog, "registrationCandidate");
            JButton reviewButton = (JButton) field(dialog, "registrationButton");
            assertNotNull(reviewButton.getClientProperty("ikasan.studio.attentionPulseTimer"));
            assertEquals(List.of("org.original.Copied"), candidate.components());
            assertFalse(Files.exists(destination.resolve(candidate.destination())));
            Field approved = dialog.getClass().getDeclaredField("approvedRegistration"); approved.setAccessible(true); approved.set(dialog, candidate);
            refresh.invoke(dialog);
            JButton button = (JButton) field(dialog, "registrationButton");
            PlatformTestUtil.waitWithEventsDispatching("Staged registration was not shown",
                    () -> button.getText().equals(StudioBundle.message("sourceImport.registration.revert")), 10);
            assertTrue(((List<SourceModuleImport.ReviewAction>) field(dialog, "standaloneIssues")).isEmpty());
            assertNull(button.getClientProperty("ikasan.studio.attentionPulseTimer"));
            button.doClick();
            PlatformTestUtil.waitWithEventsDispatching("Registration revert did not restore the scan decision",
                    () -> fieldValue(dialog, "approvedRegistration") == null
                            && !((List<?>) fieldValue(dialog, "standaloneIssues")).isEmpty(), 10);
            assertNotNull(button.getClientProperty("ikasan.studio.attentionPulseTimer"));
            assertFalse(Files.exists(destination.resolve(candidate.destination())));
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
        assertNull(((JButton) field(dialog, "registrationButton")).getClientProperty("ikasan.studio.attentionPulseTimer"));
    }

    @SuppressWarnings("unchecked") // The reflected fields have these generic types in ImportSourceModuleDialog.
    public void testCopyAllNewStagesOnlyMissingFilesAndPreservesExistingSelectionsAndEdits() throws Exception {
        ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject());
        Path original = Path.of("/original");
        Path destination = Files.createTempDirectory("studio-copy-all-new");
        List<FileChoice> files = List.of(
                new SourceModuleImport.FileChoice("src/Z.java", "user/Z.java", encoded("class Z {}"), null, false, false, ""),
                new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("class A {}"), null, false, false, ""),
                new SourceModuleImport.FileChoice("src/C.java", "user/C.java", encoded("class C {}"), encoded("class C { int existing; }"), false, false, ""),
                new SourceModuleImport.FileChoice("src/new.properties", "user/new.properties", encoded("key=value"), null, false, false, ""));
        Analysis analysis = new SourceModuleImport.Analysis(original, destination, files, List.of(), List.of(), List.of(), List.of(), "",
                Map.of(), List.of(), Map.of(), identity(original, destination, "Example", "Example"));
        try {
            dialog.acceptAnalysis(analysis, original);
            Set<String> selected = (Set<String>) field(dialog, "selectedFiles"); selected.add("src/A.java");
            Map<String,String> edits = (Map<String, String>) field(dialog, "stagedJava"); edits.put("src/A.java", "class A { int reviewed; }");
            Set<String> dependencies = (Set<String>) field(dialog, "selectedDependencies"); dependencies.add("already-selected");
            JTable table = (JTable) field(dialog, "table"); table.getRowSorter().toggleSortOrder(1);
            JButton button = (JButton) field(dialog, "copyAllNewButton"); assertTrue(button.isEnabled()); button.doClick();
            assertEquals(Set.of("src/Z.java", "src/A.java", "src/new.properties"), selected);
            assertEquals("class A { int reviewed; }", edits.get("src/A.java"));
            assertEquals(Set.of("already-selected"), dependencies);
            assertFalse(button.isEnabled());
            assertFalse(Files.exists(destination.resolve("user")));
            // The individual row action can still revert one file, re-enabling the bulk action.
            table.getModel().setValueAt(false, 0, 0);
            assertTrue(button.isEnabled());
            dialog.acceptAnalysis(new SourceModuleImport.Analysis(original, destination, files, List.of(), List.of(), List.of(), List.of(), "",
                    Map.of(), List.of(), Map.of(), identity(original, destination, "Example", "Other")), original);
            assertFalse(button.isEnabled());
            Method bulk = dialog.getClass().getDeclaredMethod("copyAllNewFiles"); bulk.setAccessible(true); bulk.invoke(dialog);
            assertTrue(((Set<?>) field(dialog, "selectedFiles")).isEmpty());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private Object fieldValue(Object target, String name) {
        try { return field(target, name); } catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private ImportSourceModuleDialog previewDialog() {
        ImportSourceModuleDialog dialog = new ImportSourceModuleDialog(getProject());
        Analysis analysis = new SourceModuleImport.Analysis(Path.of("/original"), Path.of("/destination"), List.of(
                new SourceModuleImport.FileChoice("src/B.java", "user/B.java", encoded("original B"), encoded("destination B"), false, false, "Review"),
                new SourceModuleImport.FileChoice("src/A.java", "user/A.java", encoded("original A"), encoded("destination A"), false, false, "Review")),
                List.of(), List.of(), List.of(), List.of(), "", Map.of(),
                List.of(new SourceModuleImport.ManagedFile("src/DebugTransitionComponent.java", "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java",
                        encoded("original support"), encoded("destination support"), SourceModuleImport.SupportStatus.ORIGINAL_DIFFERS)), Map.of(),
                identity(Path.of("/original"), Path.of("/destination"), "Example", "Example"));
        dialog.acceptAnalysis(analysis, analysis.sourceRoot());
        return dialog;
    }
    private String encoded(String text) { return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }
    private void assertDiff(SimpleDiffRequest request, String original, String destination) {
        assertNotNull(request);
        assertEquals(original, ((DocumentContent) request.getContents().get(0)).getDocument().getText());
        assertEquals(destination, ((DocumentContent) request.getContents().get(1)).getDocument().getText());
        assertFalse(((DocumentContent) request.getContents().get(0)).getDocument().isWritable());
        if (Boolean.TRUE.equals(request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY)))
            assertFalse(((DocumentContent) request.getContents().get(1)).getDocument().isWritable());
    }
    private void doubleClick(JTable table, int column) {
        Rectangle bounds = table.getCellRect(0, column, true);
        MouseEvent event = new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2, 2, false, MouseEvent.BUTTON1);
        for (MouseListener listener : table.getMouseListeners())
            if (listener.getClass().getEnclosingClass() == ImportSourceModuleDialog.class) listener.mouseClicked(event);
    }

    private Check identity(Path source,
            Path destination, String expected, String actual) {
        Candidate candidate = new Candidate("", "artifact-hint", List.of(""),
                actual.isEmpty() ? List.of() : List.of(new Declaration("application.properties", actual)), false);
        Inspection inspection = new Inspection(source, destination, expected,
                List.of(candidate), List.of(""), List.of(), List.of());
        return new Check(inspection, candidate, List.of());
    }

    public void testMismatchAndUnverifiedCheckoutRequireApprovalBeforeDefaultSelectionAndImport() throws Exception {
        ImportSourceModuleDialog dialog = previewDialog();
        try {
            Analysis original = (SourceModuleImport.Analysis) field(dialog, "analysis");
            for (String actual : List.of("WrongModule", "")) {
                Check check = identity(original.sourceRoot(), original.destinationRoot(), "ExpectedModule", actual);
                Analysis analysis = new SourceModuleImport.Analysis(original.sourceRoot(), original.destinationRoot(), List.of(
                        new SourceModuleImport.FileChoice("src/New.java", "user/New.java", encoded("source"), null, true, false, "New")),
                        List.of(), List.of(), List.of(), List.of(), "", Map.of(), List.of(), Map.of(), check);
                dialog.acceptAnalysis(analysis, original.sourceRoot());
                JCheckBox approval = (JCheckBox) field(dialog, "acceptIdentity");
                JTable table = (JTable) field(dialog, "table");
                assertTrue(approval.isVisible()); assertFalse(approval.isSelected());
                assertFalse(dialog.isOKActionEnabled());
                assertFalse(table.isCellEditable(0, 1)); assertEquals(Boolean.FALSE, table.getValueAt(0, 1));
                Method validate = dialog.getClass().getDeclaredMethod("doValidate"); validate.setAccessible(true);
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

    private SimpleDiffRequest requiredComparison(
            SimpleDiffRequest request) {
        assertNotNull(request);
        return Objects.requireNonNull(request);
    }

    private Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }

}
