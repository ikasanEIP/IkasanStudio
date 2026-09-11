package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class DebugCanvasSelectionTest {
    private final FileEditorManager editors = mock(FileEditorManager.class);
    private final VirtualFile canvas = new IkasanStudioVirtualFile();
    private final VirtualFile source = mock(VirtualFile.class);
    private final List<Runnable> queued = new ArrayList<>();
    private boolean enabled = true, disposed, paused = true;
    private long pause = 1;
    private final DebugCanvasSelection selection = new DebugCanvasSelection(editors, () -> enabled, () -> disposed,
            path -> paused && "Debug.java".equals(path), () -> pause, queued::add);

    DebugCanvasSelectionTest() {
        when(source.getPath()).thenReturn("Debug.java");
        when(editors.isFileOpen(canvas)).thenReturn(true);
        when(editors.getSelectedFiles()).thenReturn(new VirtualFile[]{source});
    }

    @Test void returnsOncePerPauseWithoutTakingKeyboardFocus() {
        selection.selectionChanged(canvas, source, false);
        verify(editors, never()).openFile(any(), anyBoolean());
        queued.remove(0).run();
        verify(editors).openFile(canvas, false);
        selection.selectionChanged(canvas, source, false);
        assertThat(queued).isEmpty();
        pause++;
        selection.selectionChanged(canvas, source, false);
        queued.remove(0).run();
        verify(editors, times(2)).openFile(canvas, false);
    }

    @Test void ignoresManualNavigationDisabledSettingAndNonDebugBreakpoints() {
        selection.selectionChanged(canvas, source, true);
        enabled = false;
        selection.selectionChanged(canvas, source, false);
        enabled = true;
        paused = false;
        selection.selectionChanged(canvas, source, false);
        paused = true;
        selection.selectionChanged(source, source, false);
        assertThat(queued).isEmpty();
    }

    @Test void subsequentNavigationCancelsPendingReturn() {
        selection.selectionChanged(canvas, source, false);
        selection.selectionChanged(source, mock(VirtualFile.class), true);
        queued.remove(0).run();
        verify(editors, never()).openFile(any(), anyBoolean());
    }

    @Test void resumeClosureDisposalSettingChangeAndNewPauseCancelPendingReturn() {
        for (int reason = 0; reason < 5; reason++) {
            enabled = true; disposed = false; paused = true;
            when(editors.isFileOpen(canvas)).thenReturn(true);
            selection.selectionChanged(canvas, source, false);
            switch (reason) {
                case 0 -> paused = false;
                case 1 -> when(editors.isFileOpen(canvas)).thenReturn(false);
                case 2 -> disposed = true;
                case 3 -> enabled = false;
                case 4 -> pause++;
            }
            queued.remove(0).run();
        }
        verify(editors, never()).openFile(any(), anyBoolean());
    }
}
