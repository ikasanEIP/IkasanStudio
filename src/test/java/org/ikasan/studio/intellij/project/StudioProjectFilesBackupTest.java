package org.ikasan.studio.intellij.project;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.ikasan.studio.StudioRuntimeException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StudioProjectFilesBackupTest extends BasePlatformTestCase {
    public void testBackupsCaptureUnsavedInvalidEditorTextWithoutSavingOrReplacingIt() throws Exception {
        String saved = "class Batch { }\n";
        VirtualFile file = myFixture.getTempDirFixture().createFile("Batch.java", saved);
        var manager = FileDocumentManager.getInstance();
        var document = manager.getDocument(file);
        assertNotNull(document);
        String firstEdit = "class Batch { // 日本語\n void broken( {\n";
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText(firstEdit));
        assertTrue(manager.isDocumentUnsaved(document));
        assertEquals(saved, VfsUtilCore.loadText(file));

        StudioProjectFiles.backupFile(getProject(), file);
        String secondEdit = firstEdit + " // another unsaved edit\n";
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText(secondEdit));
        StudioProjectFiles.backupFile(getProject(), file);

        var backups = Arrays.stream(file.getParent().getChildren())
                .filter(child -> child.getName().startsWith("Batch.java.backup")).toList();
        assertEquals(2, backups.size());
        Set<String> texts = backups.stream().map(backup -> {
            try { return VfsUtilCore.loadText(backup); }
            catch (IOException failure) { throw new RuntimeException(failure); }
        }).collect(Collectors.toSet());
        assertEquals(Set.of(firstEdit, secondEdit), texts);
        assertEquals(secondEdit, document.getText());
        assertEquals(saved, VfsUtilCore.loadText(file));
        assertTrue(manager.isDocumentUnsaved(document));
    }

    public void testUnopenedFileIsCopiedByteForByte() throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("Closed.java", "class Closed {}\r\n");
        byte[] original = file.contentsToByteArray();
        StudioProjectFiles.backupFile(getProject(), file);
        VirtualFile backup = Arrays.stream(file.getParent().getChildren())
                .filter(child -> child.getName().startsWith("Closed.java.backup")).findFirst().orElseThrow();
        assertArrayEquals(original, backup.contentsToByteArray());
    }

    public void testCopyFailurePropagatesInsteadOfAllowingOverwrite() throws Exception {
        VirtualFile file = mock(VirtualFile.class);
        VirtualFile parent = mock(VirtualFile.class);
        when(file.isValid()).thenReturn(true);
        when(file.getName()).thenReturn("Batch.java");
        when(file.getPath()).thenReturn("/user/Batch.java");
        when(file.getParent()).thenReturn(parent);
        when(file.copy(any(), same(parent), anyString())).thenThrow(new IOException("disk full"));
        try (var managers = mockStatic(FileDocumentManager.class)) {
            managers.when(FileDocumentManager::getInstance).thenReturn(mock(FileDocumentManager.class));
            try {
                StudioProjectFiles.backupFile(getProject(), file);
                fail("A failed backup must stop its caller");
            } catch (StudioRuntimeException expected) {
                assertTrue(expected.getMessage().contains("Batch.java"));
            }
        }
    }
}
