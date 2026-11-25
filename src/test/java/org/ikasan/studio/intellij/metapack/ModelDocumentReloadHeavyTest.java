package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelDocumentReloadHeavyTest extends BasePlatformTestCase {
    public void testReviewedDiskUpdateIsVisibleToPsiBeforeModelLoading() throws Exception {
        String original = "{\"revision\":\"old\"}";
        String updated = "{\"revision\":\"new\"}";
        Path directory = Files.createTempDirectory("studio-model-reload-");
        Path path = directory.resolve("model.json");
        Files.writeString(path, original);
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertNotNull(file);
        try {
            PsiFile psi = PsiManager.getInstance(getProject()).findFile(file);
            Document document = FileDocumentManager.getInstance().getDocument(file);
            assertNotNull(psi);
            assertNotNull(document);
            assertEquals(original, psi.getText());
            Files.writeString(file.toNioPath(), updated);
            file.refresh(false, false);
            ModelDocumentReload.reload(getProject(), file);
            assertEquals(updated, document.getText());
            assertTrue(PsiDocumentManager.getInstance(getProject()).isCommitted(document));
            assertEquals(updated, psi.getText());
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }
}
