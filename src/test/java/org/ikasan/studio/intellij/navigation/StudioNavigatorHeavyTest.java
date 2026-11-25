package org.ikasan.studio.intellij.navigation;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import java.util.concurrent.atomic.AtomicReference;

public class StudioNavigatorHeavyTest extends HeavyPlatformTestCase {
    public void testRetainedTargetNavigatesAfterJavaClassReplacement() {
        LightVirtualFile file = new LightVirtualFile("CreateNewOrders.java", JavaFileType.INSTANCE,
                "class CreateNewOrders { }");
        PsiJavaFile psi = (PsiJavaFile) PsiManager.getInstance(myProject).findFile(file);
        assertNotNull(psi);
        PsiClass original = psi.getClasses()[0];
        NavigationTarget target = NavigationTarget.forFile(psi);
        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(myProject);
            PsiClass replacement = factory.createClass("CreateNewOrders");
            replacement.add(factory.createMethodFromText("public String suppliedLater() { return \"order\"; }", null));
            original.replace(replacement);
        });
        assertFalse("Regression requires the cached class to become invalid", original.isValid());
        ReadAction.run(() -> {
            PsiElement current = target.elementToNavigateTo();
            assertNotNull(current);
            assertTrue(current.isValid());
            assertNotSame(original, current);
            assertTrue(current.getText().contains("suppliedLater"));
        });
        StudioNavigator.navigateToSource(myProject, target, false);
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        assertTrue(FileEditorManager.getInstance(myProject).isFileOpen(file));
        FileEditorManager.getInstance(myProject).closeFile(file);
    }

    public void testTargetIsResolvedOnBackgroundReadAndExplicitOffsetSurvives() {
        LightVirtualFile file = new LightVirtualFile("CreateNewOrders.java", JavaFileType.INSTANCE,
                "// heading\nclass CreateNewOrders { }");
        PsiFile psi = PsiManager.getInstance(myProject).findFile(file);
        NavigationTarget target = NavigationTarget.forFile(psi).withOffset(3);
        Application application = ApplicationManager.getApplication();
        // Run the same target resolution used by the navigator, away from the EDT.
        AtomicReference<PsiElement> resolved = new java.util.concurrent.atomic.AtomicReference<>();
        com.intellij.openapi.application.ReadAction.nonBlocking(() -> {
            assertFalse(application.isDispatchThread());
            application.assertReadAccessAllowed();
            return target.elementToNavigateTo();
        }).finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), resolved::set)
                .submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        assertNotNull(resolved.get());
        StudioNavigator.navigateToSource(myProject, target, true);
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        assertNotNull(editor);
        assertEquals(3, editor.getCaretModel().getOffset());
        FileEditorManager.getInstance(myProject).closeFile(file);
    }
}
