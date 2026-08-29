package org.ikasan.studio.ui.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.ui.DesignerUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;

final class IkasanStudioFileEditor extends UserDataHolderBase implements FileEditor {
    private final VirtualFile file;
    private final DesignerUI designerUI;
    private boolean disposed;

    IkasanStudioFileEditor(Project project, VirtualFile file) {
        this.file = file;
        designerUI = new DesignerUI(project);
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return designerUI.getContent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return designerUI.getContent();
    }

    @Override
    public @NotNull String getName() {
        return IkasanStudioEditorService.EDITOR_NAME;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return !disposed;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            // Not a plain designerUI.dispose() call: DesignerUI registers itself as a message-bus connection's
            // parent Disposable in its own constructor, which silently adopts it as a ROOT_DISPOSABLE child the
            // first time anything is registered under it - a raw method call runs its dispose() logic but never
            // tells Disposer's own tree that node (and its registered children, e.g. CanvasPanel) was disposed,
            // which IntelliJ's leak detector then reports as a permanent leak at IDE shutdown. Disposer.dispose()
            // both invokes dispose() and correctly retires the tree node, wherever in the tree it ended up.
            Disposer.dispose(designerUI);
        }
    }
}
