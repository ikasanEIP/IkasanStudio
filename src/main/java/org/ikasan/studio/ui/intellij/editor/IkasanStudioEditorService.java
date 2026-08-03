package org.ikasan.studio.ui.intellij.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(
        name = "IkasanStudioEditorState",
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public final class IkasanStudioEditorService
        implements PersistentStateComponent<IkasanStudioEditorService.EditorState>, Disposable {
    public static final String EDITOR_NAME = "Ikasan Studio";

    public static final class EditorState {
        public boolean open;
    }

    private final Project project;
    private final IkasanStudioVirtualFile studioFile = new IkasanStudioVirtualFile();
    private EditorState state = new EditorState();
    private boolean projectClosing;

    public IkasanStudioEditorService(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if (file.equals(studioFile)) {
                            state.open = true;
                        }
                    }

                    @Override
                    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if (file.equals(studioFile) && !projectClosing) {
                            state.open = false;
                        }
                    }
                });
        ProjectManager.getInstance().addProjectManagerListener(
                project,
                new ProjectManagerListener() {
                    @Override
                    public void projectClosingBeforeSave(@NotNull Project closingProject) {
                        if (closingProject == project) {
                            projectClosing = true;
                        }
                    }

                    @Override
                    public void projectClosing(@NotNull Project closingProject) {
                        if (closingProject == project) {
                            projectClosing = true;
                        }
                    }
                });
    }

    public void open() {
        if (project.isDisposed()) {
            return;
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            openOnEdt();
        } else {
            ApplicationManager.getApplication().invokeLater(this::openOnEdt);
        }
    }

    public boolean isOpen() {
        return !project.isDisposed() && FileEditorManager.getInstance(project).isFileOpen(studioFile);
    }

    public boolean shouldRestore() {
        return state.open;
    }

    private void openOnEdt() {
        if (!project.isDisposed()) {
            FileEditorManager.getInstance(project).openFile(studioFile, true);
        }
    }

    @Override
    public EditorState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull EditorState state) {
        this.state = state;
    }

    @Override
    public void dispose() {
        // Disposed automatically by the platform when the project closes; nothing to release here
        // beyond the message-bus connection expiring via connect(this).
    }
}
