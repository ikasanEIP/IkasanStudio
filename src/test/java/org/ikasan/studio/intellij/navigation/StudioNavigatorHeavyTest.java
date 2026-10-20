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

public class StudioNavigatorHeavyTest extends HeavyPlatformTestCase {
    public void testRetainedTargetNavigatesAfterJavaClassReplacement() {
        var file = new LightVirtualFile("CreateNewOrders.java", JavaFileType.INSTANCE,
                "class CreateNewOrders { }");
        var psi = (PsiJavaFile) PsiManager.getInstance(myProject).findFile(file);
        assertNotNull(psi);
        var original = psi.getClasses()[0];
        var target = NavigationTarget.forFile(psi);
        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            var factory = JavaPsiFacade.getElementFactory(myProject);
            var replacement = factory.createClass("CreateNewOrders");
            replacement.add(factory.createMethodFromText("public String suppliedLater() { return \"order\"; }", null));
            original.replace(replacement);
        });
        assertFalse("Regression requires the cached class to become invalid", original.isValid());
        ReadAction.run(() -> {
            var current = target.elementToNavigateTo();
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
        var file = new LightVirtualFile("CreateNewOrders.java", JavaFileType.INSTANCE,
                "// heading\nclass CreateNewOrders { }");
        var psi = PsiManager.getInstance(myProject).findFile(file);
        var target = NavigationTarget.forFile(psi).withOffset(3);
        var application = ApplicationManager.getApplication();
        // Run the same target resolution used by the navigator, away from the EDT.
        var resolved = new java.util.concurrent.atomic.AtomicReference<com.intellij.psi.PsiElement>();
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
        var editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        assertNotNull(editor);
        assertEquals(3, editor.getCaretModel().getOffset());
        FileEditorManager.getInstance(myProject).closeFile(file);
    }
}
