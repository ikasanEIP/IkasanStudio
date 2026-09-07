package org.ikasan.studio.intellij.editor;

import com.intellij.testFramework.HeavyPlatformTestCase;

// Closing is permanent project state: each test needs its own project, not the shared light fixture.
public class IkasanStudioEditorServiceStateTest extends HeavyPlatformTestCase {

    public void testRestorationStateReflectsPersistedEditorState() {
        IkasanStudioEditorService service = getProject().getService(IkasanStudioEditorService.class);
        IkasanStudioEditorService.EditorState state = new IkasanStudioEditorService.EditorState();
        state.open = true;

        service.loadState(state);

        assertTrue(service.shouldRestore());
        assertSame(state, service.getState());
    }

    public void testDeliberateCloseDisablesRestoreAndReopenEnablesItAgain() {
        IkasanStudioEditorService service = getProject().getService(IkasanStudioEditorService.class);
        service.recordEditorOpened();
        assertTrue(service.shouldRestore());
        service.recordEditorClosed();
        assertFalse(service.shouldRestore());
        service.recordEditorOpened();
        assertTrue(service.shouldRestore());
    }

    public void testProjectShutdownDoesNotLookLikeADeliberateEditorClose() {
        IkasanStudioEditorService service = getProject().getService(IkasanStudioEditorService.class);
        service.recordEditorOpened();
        service.recordProjectClosing();
        service.recordEditorClosed();
        assertTrue(service.shouldRestore());
    }

    public void testVirtualFileUsesTheStudioSquidIcon() {
        IkasanStudioVirtualFile file = new IkasanStudioVirtualFile();

        assertSame(IkasanStudioFileType.INSTANCE, file.getFileType());
        assertNotNull(file.getFileType().getIcon());
    }
}
