package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/** One deferred return per pause, cancelled by subsequent navigation, resume, closure or disposal. EDT only. */
final class DebugCanvasSelection {
    private final FileEditorManager editors;
    private final BooleanSupplier enabled;
    private final BooleanSupplier disposed;
    private final Predicate<String> pausedDebugSource;
    private final LongSupplier pauseRevision;
    private final Consumer<Runnable> later;
    private long selectionRevision;
    private long restoredPause = -1;

    DebugCanvasSelection(FileEditorManager editors, BooleanSupplier enabled, BooleanSupplier disposed,
                         Predicate<String> pausedDebugSource, LongSupplier pauseRevision, Consumer<Runnable> later) {
        this.editors = editors;
        this.enabled = enabled;
        this.disposed = disposed;
        this.pausedDebugSource = pausedDebugSource;
        this.pauseRevision = pauseRevision;
        this.later = later;
    }

    void selectionChanged(VirtualFile previous, VirtualFile selected, boolean userInput) {
        long selection = ++selectionRevision;
        if (userInput || !(previous instanceof IkasanStudioVirtualFile) || selected == null
                || !enabled.getAsBoolean() || disposed.getAsBoolean()) return;
        long pause = pauseRevision.getAsLong();
        if (pause == restoredPause || !pausedDebugSource.test(selected.getPath())) return;
        later.accept(() -> {
            if (disposed.getAsBoolean() || !enabled.getAsBoolean() || selection != selectionRevision
                    || pause != pauseRevision.getAsLong() || pause == restoredPause
                    || !editors.isFileOpen(previous) || !pausedDebugSource.test(selected.getPath())
                    || java.util.Arrays.stream(editors.getSelectedFiles()).noneMatch(selected::equals)) return;
            restoredPause = pause;
            // Preserve focus in the debugger tool window, and reuse the existing Studio editor.
            editors.openFile(previous, false);
        });
    }
}
