package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;

/** Publish a reviewed disk update to both the editor document and PSI before model loading. */
final class ModelDocumentReload {
    private ModelDocumentReload() { }

    static void reload(Project project, VirtualFile file) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document == null) return;
        ApplicationManager.getApplication().runWriteAction(() -> {
            FileDocumentManager.getInstance().reloadFromDisk(document);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }
}
