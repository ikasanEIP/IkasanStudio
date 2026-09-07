package org.ikasan.studio.intellij.project;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.psi.PsiManager;
import java.util.concurrent.atomic.AtomicReference;

public class NavigationTextReaderHeavyTest extends HeavyPlatformTestCase {
    public void testUncachedJavaDocumentLoadsOffEdtAndPublishesOnEdt() {
        var file = new LightVirtualFile("NavigationSample.java", JavaFileType.INSTANCE, "class NavigationSample { }");
        var psi = PsiManager.getInstance(myProject).findFile(file);
        assertNotNull(psi);
        assertNull(FileDocumentManager.getInstance().getCachedDocument(file));
        var result = new AtomicReference<NavigationTextReader.Snapshot>();
        NavigationTextReader.read(myProject, psi, snapshot -> {
            assertTrue(ApplicationManager.getApplication().isDispatchThread());
            result.set(snapshot);
        });
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        assertNotNull(result.get());
        assertEquals("class NavigationSample { }", result.get().text());
        assertSame(psi, result.get().target().psiFile());
        assertNotNull(FileDocumentManager.getInstance().getCachedDocument(file));
    }

    public void testUnsavedDocumentContentIsUsedForOffsets() {
        var file = new LightVirtualFile("NavigationSample.java", JavaFileType.INSTANCE, "class NavigationSample { }");
        var psi = PsiManager.getInstance(myProject).findFile(file);
        var document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(myProject, () -> document.setText("// unsaved\nclass NavigationSample { }"));
        var result = new AtomicReference<NavigationTextReader.Snapshot>();
        NavigationTextReader.read(myProject, psi, result::set);
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        assertNotNull(result.get());
        assertEquals(document.getText(), result.get().text());
    }
}
