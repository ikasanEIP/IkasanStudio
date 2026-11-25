package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.RuntimeImportClient.Documents;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

// Test-only modality simulation: LaterInvocator is never included in the distributed plugin.
@SuppressWarnings("JetBrainsInternalApiUsage")
public class ImportModelJsonDialogHeavyTest extends HeavyPlatformTestCase {
    public void testFetchedVersionSelectsMatchingPackBeforeReview() throws Exception {
        assertFetchedVersionSelection(false);
    }

    public void testFetchedVersionSelectsMatchingPackAfterDelayedDiscovery() throws Exception {
        assertFetchedVersionSelection(true);
    }

    private void assertFetchedVersionSelection(boolean delayedDiscovery) throws Exception {
        CountDownLatch release = new CountDownLatch(delayedDiscovery ? 1 : 0);
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Pack discovery was not released");
            } catch (InterruptedException failure) { throw new AssertionError(failure); }
            return List.of("V3.3.9", "V4.1.6");
        });
        try {
            JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
            if (!delayedDiscovery) {
                PlatformTestUtil.waitWithEventsDispatching("Packs did not load", () -> packs.getItemCount() == 2, 10);
                assertEquals("V3.3.9", packs.getSelectedItem());
            }
            dialog.applyFetchedDocuments(new Documents(
                    "{\"ikasanVersion\":\"4.1.6\",\"flows\":[]}", "[]"));
            release.countDown();
            PlatformTestUtil.waitWithEventsDispatching("Fetched version was not selected",
                    () -> "V4.1.6".equals(packs.getSelectedItem()), 10);
        } finally {
            release.countDown();
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testAcquisitionAndFollowUpStepsAppearOnlyWhenApplicable() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            JCheckBox mode = (JCheckBox) field(dialog, "runtimeMode");
            JTextArea json = (JTextArea) field(dialog, "jsonArea");
            JTextArea configuration = (JTextArea) field(dialog, "configurationArea");
            JPanel row = (JPanel) field(dialog, "acquisitionRow");
            JPanel documents = (JPanel) field(dialog, "documentPanel");
            JPanel followUp = (JPanel) field(dialog, "optionalSteps");
            JButton pasteConfig = (JButton) field(dialog, "pasteConfiguration");
            assertFalse(row.isVisible()); assertTrue(documents.isVisible()); assertFalse(followUp.isVisible());
            assertTrue(json.isEditable());
            json.setText("studio model preview");
            assertTrue(followUp.isVisible());
            ((JCheckBox) field(dialog, "importSource")).setSelected(true);
            ((JCheckBox) field(dialog, "importProperties")).setSelected(true);
            mode.doClick();
            assertTrue(row.isVisible()); assertFalse(documents.isVisible()); assertFalse(followUp.isVisible());
            assertFalse(dialog.isOKActionEnabled()); assertFalse(pasteConfig.isEnabled());
            ((JButton) field(dialog, "pasteMetadata")).doClick();
            assertTrue(documents.isVisible()); assertTrue(json.isEditable()); assertTrue(followUp.isVisible());
            assertTrue(pasteConfig.isEnabled());
            pasteConfig.doClick();
            assertTrue(configuration.isEditable());
            assertEquals(1, ((JTabbedPane) field(dialog, "documents")).getSelectedIndex());
            json.setText("");
            assertFalse(pasteConfig.isEnabled()); assertFalse(followUp.isVisible()); assertFalse(dialog.isOKActionEnabled());
            assertEquals(0, ((JTabbedPane) field(dialog, "documents")).getSelectedIndex());
            mode.doClick(); json.setText("studio model preview");
            assertFalse(row.isVisible()); assertTrue(followUp.isVisible());
            assertTrue(((JCheckBox) field(dialog, "importSource")).isSelected());
            assertTrue(((JCheckBox) field(dialog, "importProperties")).isSelected());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testFetchedDocumentsAreReadOnlyUntilManualInputIsChosen() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            dialog.applyFetchedDocuments(new Documents("metadata", "configuration"));
            JTextArea json = (JTextArea) field(dialog, "jsonArea");
            JTextArea configuration = (JTextArea) field(dialog, "configurationArea");
            assertFalse(json.isEditable()); assertFalse(configuration.isEditable());
            assertTrue(((JPanel) field(dialog, "documentPanel")).isVisible());
            ((JButton) field(dialog, "pasteMetadata")).doClick();
            assertTrue(json.isEditable()); assertTrue(configuration.isEditable());
            assertEquals("metadata", json.getText()); assertEquals("configuration", configuration.getText());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testWizardSelectsRuntimeAndPostGenerationSourceReviewWithoutChangingTheModel() throws Exception {
        Module original = getProject().getService(UiContext.class).getIkasanModule();
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            dialog.enableSourceWizard();
            assertTrue(((JCheckBox) field(dialog, "runtimeMode")).isSelected());
            assertTrue(((JCheckBox) field(dialog, "importSource")).isSelected());
            assertSame(original, getProject().getService(UiContext.class).getIkasanModule());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testRuntimePackChoicesArriveWhileImportDialogIsModal() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Pack discovery was not released");
            } catch (InterruptedException failure) { throw new AssertionError(failure); }
            return List.of("V3.3.9", "V4.1.6");
        });
        Object marker = new Object();
        JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
        ((JCheckBox) field(dialog, "runtimeMode")).doClick();
        ((JButton) field(dialog, "pasteMetadata")).doClick();
        ((JTextArea) field(dialog, "jsonArea")).setText("{\"module\":{}}");
        assertFalse(dialog.isOKActionEnabled());
        LaterInvocator.enterModal(marker);
        try {
            release.countDown();
            PlatformTestUtil.waitWithEventsDispatching("Pack choices remained blocked behind modal import dialog",
                    () -> packs.getItemCount() == 2, 10);
            assertEquals("V3.3.9", packs.getSelectedItem());
            assertTrue(dialog.isOKActionEnabled());
        } finally {
            release.countDown();
            LaterInvocator.leaveModal(marker);
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testDiscoveryFailureIsReportedWhileImportDialogIsModal() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger shown = new AtomicInteger();
        TestDialog previous = TestDialogManager.setTestDialog(message -> {
            shown.incrementAndGet();
            return Messages.OK;
        });
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Pack discovery was not released");
            } catch (InterruptedException failure) { throw new AssertionError(failure); }
            throw new IllegalStateException("Pack discovery failed");
        });
        Object marker = new Object();
        LaterInvocator.enterModal(marker);
        try {
            release.countDown();
            PlatformTestUtil.waitWithEventsDispatching("Pack loading error remained blocked behind modal dialog",
                    () -> shown.get() == 1, 10);
            assertEquals(0, ((JComboBox<?>) field(dialog, "runtimePack")).getItemCount());
            assertFalse(dialog.isDisposed());
        } finally {
            release.countDown();
            LaterInvocator.leaveModal(marker);
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            TestDialogManager.setTestDialog(previous);
        }
    }

    public void testFetchedDocumentsPopulateOnlyThePreviewAndEnableRuntimeMode() throws Exception {
        Module original = getProject().getService(UiContext.class).getIkasanModule();
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try (MockedStatic<StudioProjectFiles> files = Mockito.mockStatic(StudioProjectFiles.class)) {
            Documents documents = new Documents(
                    "{\"name\":\"Fetched\",\"flows\":[]}", "[]");
            dialog.applyFetchedDocuments(documents);
            assertTrue(((JCheckBox) field(dialog, "runtimeMode")).isSelected());
            assertEquals(documents.moduleJson(), ((JTextArea) field(dialog, "jsonArea")).getText());
            assertEquals("[]", ((JTextArea) field(dialog, "configurationArea")).getText());
            assertSame(original, getProject().getService(UiContext.class).getIkasanModule());
            files.verifyNoInteractions();
            dialog.applyFetchedDocuments(null);
            assertEquals(documents.moduleJson(), ((JTextArea) field(dialog, "jsonArea")).getText());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testImportModeShowsOnlyRelevantDocumentsAndPreservesPreviewOnSwitch() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            JTextArea json = (JTextArea) field(dialog, "jsonArea");
            JTextArea configuration = (JTextArea) field(dialog, "configurationArea");
            JCheckBox mode = (JCheckBox) field(dialog, "runtimeMode");
            JTabbedPane tabs = (JTabbedPane) field(dialog, "documents");
            assertNotNull(tabs);
            assertEquals(0, tabs.getTabCount());
            assertNull(SwingUtilities.getAncestorOfClass(JTabbedPane.class, json));
            json.setText("module preview");
            configuration.setText("configuration preview");
            mode.doClick();
            assertEquals(2, tabs.getTabCount());
            assertEquals(StudioBundle.message("import.runtime.moduleTab"), tabs.getTitleAt(0));
            tabs.setSelectedIndex(1);
            mode.doClick();
            assertEquals(0, tabs.getTabCount());
            assertNull(SwingUtilities.getAncestorOfClass(JTabbedPane.class, json));
            mode.doClick();
            assertEquals(2, tabs.getTabCount());
            assertEquals("module preview", json.getText());
            assertEquals("configuration preview", configuration.getText());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testLargeFetchedJsonDoesNotGrowDialogAndOpensAtTheStart() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            JTextArea module = (JTextArea) field(dialog, "jsonArea");
            JTextArea configuration = (JTextArea) field(dialog, "configurationArea");
            JTabbedPane tabs = (JTabbedPane) field(dialog, "documents");
            dialog.applyFetchedDocuments(new Documents("{}", "[]"));
            Container panel = tabs.getParent();
            Dimension before = panel.getPreferredSize();
            Dimension viewportBefore = module.getPreferredScrollableViewportSize();
            Dimension configurationBefore = configuration.getPreferredScrollableViewportSize();
            String large = "{\"description\":\"" + "x".repeat(30000) + "\",\"flows\":[" + "{} ,\n".repeat(3000) + "{}]}";
            dialog.applyFetchedDocuments(new Documents(large, large));
            assertEquals(before, panel.getPreferredSize());
            assertEquals(viewportBefore, module.getPreferredScrollableViewportSize());
            assertEquals(configurationBefore, configuration.getPreferredScrollableViewportSize());
            assertEquals(0, module.getCaretPosition());
            assertEquals(0, configuration.getCaretPosition());
            assertEquals(large, module.getText());
            assertEquals(large, configuration.getText());
            assertTrue(module.getColumns() > 0);
            assertTrue(configuration.getColumns() > 0);
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testPatchRecommendationRequiresConsentBeforeChangingSelection() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V4.1.6"));
        JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
        Module original = getProject().getService(UiContext.class).getIkasanModule();
        ArrayList<String> messages = new ArrayList<>();
        TestDialog previous = TestDialogManager.setTestDialog(message -> {
            messages.add(message);
            return Messages.NO;
        });
        try {
            PlatformTestUtil.waitWithEventsDispatching("Pack choices not loaded", () -> packs.getItemCount() == 2, 10);
            String json = "{\"name\":\"Demo\",\"ikasanVersion\":\"4.1.3\",\"flows\":[]}";
            try {
                dialog.selectRuntimePack(json);
                fail("Patch upgrade needs explicit acceptance");
            } catch (ProcessCanceledException expected) { }
            assertEquals("V3.3.9", packs.getSelectedItem());
            assertSame(original, getProject().getService(UiContext.class).getIkasanModule());
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).contains("4.1.3"));
            assertTrue(messages.get(0).contains("4.1.6"));
            TestDialogManager.setTestDialog(message -> Messages.YES);
            assertEquals("V4.1.6", dialog.selectRuntimePack(json));
            assertEquals("V4.1.6", packs.getSelectedItem());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            TestDialogManager.setTestDialog(previous);
        }
    }

    public void testSnapshotToNewerMinorRequiresConsentBeforeChangingSelection() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V5.1.0"));
        JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
        Module original = getProject().getService(UiContext.class).getIkasanModule();
        ArrayList<String> messages = new ArrayList<>();
        TestDialog previous = TestDialogManager.setTestDialog(message -> {
            messages.add(message);
            return Messages.NO;
        });
        try {
            PlatformTestUtil.waitWithEventsDispatching("Pack choices not loaded", () -> packs.getItemCount() == 2, 10);
            String json = "{\"name\":\"Demo\",\"ikasanVersion\":\"5.0.1-SNAPSHOT\",\"flows\":[]}";
            try {
                dialog.selectRuntimePack(json);
                fail("Same-major upgrade needs explicit acceptance");
            } catch (ProcessCanceledException expected) { }
            assertEquals("V3.3.9", packs.getSelectedItem());
            assertSame(original, getProject().getService(UiContext.class).getIkasanModule());
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).contains("5.0.1-SNAPSHOT"));
            assertTrue(messages.get(0).contains("5.1.0"));
            TestDialogManager.setTestDialog(message -> Messages.YES);
            assertEquals("V5.1.0", dialog.selectRuntimePack(json));
            assertEquals("V5.1.0", packs.getSelectedItem());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            TestDialogManager.setTestDialog(previous);
        }
    }

    public void testFetchedPatchShowsProposalBeforeReviewButStillRequiresConsent() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V4.1.6"));
        TestDialog previous = TestDialogManager.setTestDialog(message -> Messages.NO);
        try {
            JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
            JTextField detected = (JTextField) field(dialog, "detectedVersion");
            Module original = getProject().getService(UiContext.class).getIkasanModule();
            String json = "{\"name\":\"Demo\",\"ikasanVersion\":\"4.1.3\",\"flows\":[]}";
            dialog.applyFetchedDocuments(new Documents(json, "[]"));
            PlatformTestUtil.waitWithEventsDispatching("Patch proposal not displayed", () ->
                    "4.1.3".equals(detected.getText()) && "V4.1.6".equals(packs.getSelectedItem()), 10);
            assertFalse(detected.isEditable());
            String hint = ((JTextArea) field(dialog, "versionHelp")).getText();
            assertTrue(hint.contains("4.1.3")); assertTrue(hint.contains("4.1.6"));
            try { dialog.selectRuntimePack(json); fail("A proposed patch must still require consent"); }
            catch (ProcessCanceledException expected) { }
            assertSame(original, getProject().getService(UiContext.class).getIkasanModule());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            TestDialogManager.setTestDialog(previous);
        }
    }

    public void testManualMetadataPreservesExplicitTargetAndDestinationPackage() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V4.1.6"));
        try {
            JComboBox<?> packs = (JComboBox<?>) field(dialog, "runtimePack");
            PlatformTestUtil.waitWithEventsDispatching("Packs not loaded", () -> packs.getItemCount() == 2, 10);
            ((JCheckBox) field(dialog, "runtimeMode")).doClick();
            ((JButton) field(dialog, "pasteMetadata")).doClick();
            JTextArea json = (JTextArea) field(dialog, "jsonArea");
            JTextField detected = (JTextField) field(dialog, "detectedVersion");
            json.setText("{\"ikasanVersion\":\"4.1.6\",\"flows\":[]}");
            PlatformTestUtil.waitWithEventsDispatching("Manual version not detected", () ->
                    "4.1.6".equals(detected.getText()) && "V4.1.6".equals(packs.getSelectedItem()), 10);
            packs.setSelectedItem("V3.3.9");
            JTextField pkg = (JTextField) field(dialog, "applicationPackage");
            pkg.setText("com.example.destination");
            json.setText("{\"ikasanVersion\":\"4.1.3\",\"flows\":[]}");
            PlatformTestUtil.waitWithEventsDispatching("Edited source not detected", () -> "4.1.3".equals(detected.getText()), 10);
            assertEquals("V3.3.9", packs.getSelectedItem());
            assertEquals("com.example.destination", pkg.getText());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testUnsupportedVersionIsDisplayedWithoutGuessingAnUnrelatedTarget() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V4.1.6"));
        try {
            dialog.applyFetchedDocuments(new Documents(
                    "{\"ikasanVersion\":\"4.2.0\",\"flows\":[]}", "[]"));
            JTextField detected = (JTextField) field(dialog, "detectedVersion");
            PlatformTestUtil.waitWithEventsDispatching("Unsupported version not displayed", () -> "4.2.0".equals(detected.getText()), 10);
            assertEquals(StudioBundle.message("import.runtime.versionUnsupported", "4.2.0", ""),
                    ((JTextArea) field(dialog, "versionHelp")).getText());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testExplanatoryTextCanBeFocusedAndSelectedWithoutEditing() throws Exception {
        ImportModelJsonDialog dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try {
            for (String name : List.of("journeyHelp", "sourceHelp", "runtimeDefaultsHelp", "versionHelp")) {
                JTextArea text = (JTextArea) field(dialog, name);
                assertFalse(name + " must remain read-only", text.isEditable());
                assertTrue(name + " must allow selection and keyboard copy", text.isFocusable());
                text.selectAll();
                assertEquals(text.getText(), text.getSelectedText());
            }
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
