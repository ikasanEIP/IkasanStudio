package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.vfs.VirtualFile;

public class GenerateFlowTestSavePromptTest extends BasePlatformTestCase {
    public void testCancelListsFileAndPreservesUnsavedChanges() throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("project/Scenario.txt", "original");
        FileDocumentManager manager = FileDocumentManager.getInstance();
        Document document = manager.getDocument(file);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText("edited"));
        TestDialog previous = TestDialogManager.setTestDialog(message -> {
            assertTrue(message.contains("Scenario.txt"));
            return 1;
        });
        try {
            assertFalse(GenerateFlowTestAction.saveProjectDocumentsBeforeGeneration(
                    getProject(), file.getParent().getPath(), "Generate tests"));
            assertTrue(manager.isDocumentUnsaved(document));
            assertEquals("edited", document.getText());
            assertEquals("original", new String(file.contentsToByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        } finally { TestDialogManager.setTestDialog(previous); }
    }

    public void testSaveContinuesWithoutSavingAnotherProject() throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("project/Scenario.txt", "original");
        VirtualFile other = myFixture.getTempDirFixture().createFile("other/Unrelated.txt", "original");
        FileDocumentManager manager = FileDocumentManager.getInstance();
        Document document = manager.getDocument(file);
        Document otherDocument = manager.getDocument(other);
        assertNotNull(document);
        assertNotNull(otherDocument);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            document.setText("edited");
            otherDocument.setText("unrelated edit");
        });
        TestDialog previous = TestDialogManager.setTestDialog(message -> {
            assertTrue(message.contains("Scenario.txt"));
            assertFalse(message.contains("Unrelated.txt"));
            return 0;
        });
        try {
            assertTrue(GenerateFlowTestAction.saveProjectDocumentsBeforeGeneration(
                    getProject(), file.getParent().getPath(), "Generate tests"));
            assertFalse(manager.isDocumentUnsaved(document));
            assertTrue(manager.isDocumentUnsaved(otherDocument));
            assertEquals("edited", new String(file.contentsToByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        } finally { TestDialogManager.setTestDialog(previous); }
    }
}
