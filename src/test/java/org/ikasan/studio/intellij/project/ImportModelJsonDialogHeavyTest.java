package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ImportModelJsonDialogHeavyTest extends HeavyPlatformTestCase {
    public void testRuntimePackChoicesArriveWhileImportDialogIsModal() throws Exception {
        var release = new CountDownLatch(1);
        var dialog = new ImportModelJsonDialog(getProject(), () -> {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Pack discovery was not released");
            } catch (InterruptedException failure) { throw new AssertionError(failure); }
            return List.of("V3.3.9", "V4.1.6");
        });
        var marker = new Object();
        var packs = (JComboBox<?>) field(dialog, "runtimePack");
        ((JCheckBox) field(dialog, "runtimeMode")).setSelected(true);
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
            dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testDiscoveryFailureIsReportedWhileImportDialogIsModal() throws Exception {
        var release = new CountDownLatch(1);
        var shown = new java.util.concurrent.atomic.AtomicInteger();
        var previous = com.intellij.openapi.ui.TestDialogManager.setTestDialog(message -> {
            shown.incrementAndGet();
            return com.intellij.openapi.ui.Messages.OK;
        });
        var dialog = new ImportModelJsonDialog(getProject(), () -> {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Pack discovery was not released");
            } catch (InterruptedException failure) { throw new AssertionError(failure); }
            throw new IllegalStateException("Pack discovery failed");
        });
        var marker = new Object();
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
            dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
            com.intellij.openapi.ui.TestDialogManager.setTestDialog(previous);
        }
    }

    public void testFetchedDocumentsPopulateOnlyThePreviewAndEnableRuntimeMode() throws Exception {
        var original = getProject().getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule();
        var dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V4.1.6"));
        try (var files = org.mockito.Mockito.mockStatic(StudioProjectFiles.class)) {
            var documents = new org.ikasan.studio.integration.ikasan.RuntimeImportClient.Documents(
                    "{\"name\":\"Fetched\",\"flows\":[]}", "[]");
            dialog.applyFetchedDocuments(documents);
            assertTrue(((JCheckBox) field(dialog, "runtimeMode")).isSelected());
            assertEquals(documents.moduleJson(), ((JTextArea) field(dialog, "jsonArea")).getText());
            assertEquals("[]", ((JTextArea) field(dialog, "configurationArea")).getText());
            assertSame(original, getProject().getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule());
            files.verifyNoInteractions();
            dialog.applyFetchedDocuments(null);
            assertEquals(documents.moduleJson(), ((JTextArea) field(dialog, "jsonArea")).getText());
        } finally { dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testPatchRecommendationRequiresConsentBeforeChangingSelection() throws Exception {
        var dialog = new ImportModelJsonDialog(getProject(), () -> List.of("V3.3.9", "V4.1.6"));
        var packs = (JComboBox<?>) field(dialog, "runtimePack");
        var original = getProject().getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule();
        var messages = new java.util.ArrayList<String>();
        var previous = com.intellij.openapi.ui.TestDialogManager.setTestDialog(message -> {
            messages.add(message);
            return com.intellij.openapi.ui.Messages.NO;
        });
        try {
            PlatformTestUtil.waitWithEventsDispatching("Pack choices not loaded", () -> packs.getItemCount() == 2, 10);
            String json = "{\"name\":\"Demo\",\"ikasanVersion\":\"4.1.3\",\"flows\":[]}";
            try {
                dialog.selectRuntimePack(json);
                fail("Patch upgrade needs explicit acceptance");
            } catch (com.intellij.openapi.progress.ProcessCanceledException expected) { }
            assertEquals("V3.3.9", packs.getSelectedItem());
            assertSame(original, getProject().getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule());
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).contains("4.1.3"));
            assertTrue(messages.get(0).contains("4.1.6"));
            com.intellij.openapi.ui.TestDialogManager.setTestDialog(message -> com.intellij.openapi.ui.Messages.YES);
            assertEquals("V4.1.6", dialog.selectRuntimePack(json));
            assertEquals("V4.1.6", packs.getSelectedItem());
        } finally {
            dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
            com.intellij.openapi.ui.TestDialogManager.setTestDialog(previous);
        }
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
