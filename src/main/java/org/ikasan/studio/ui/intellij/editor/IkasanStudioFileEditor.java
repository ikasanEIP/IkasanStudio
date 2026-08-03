package org.ikasan.studio.ui.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
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
            designerUI.dispose();
        }
    }
}
