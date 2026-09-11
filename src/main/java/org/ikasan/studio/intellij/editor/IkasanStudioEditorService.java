package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
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
    private boolean disposed;

    public IkasanStudioEditorService(Project project) {
        this.project = project;
        DebugCanvasSelection debugSelection = new DebugCanvasSelection(FileEditorManager.getInstance(project),
                IkasanStudioSettings::isKeepCanvasSelectedAtDebugBreakpoints, () -> disposed || project.isDisposed(),
                path -> project.getService(IkasanDebugSessionService.class).isPausedComponentSource(path),
                () -> project.getService(IkasanDebugSessionService.class).getPauseRevision(),
                task -> ApplicationManager.getApplication().invokeLater(task));
        project.getMessageBus().connect(this).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(FileEditorManagerEvent event) {
                        debugSelection.selectionChanged(event.getOldFile(), event.getNewFile(),
                                java.awt.EventQueue.getCurrentEvent() instanceof java.awt.event.InputEvent);
                    }

                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if (file.equals(studioFile)) {
                            recordEditorOpened();
                        }
                    }

                    @Override
                    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if (file.equals(studioFile)) {
                            recordEditorClosed();
                        }
                    }
                });
        ProjectManager.getInstance().addProjectManagerListener(
                project,
                new ProjectManagerListener() {
                    @Override
                    public void projectClosingBeforeSave(@NotNull Project closingProject) {
                        if (closingProject == project) {
                            recordProjectClosing();
                        }
                    }

                    @Override
                    public void projectClosing(@NotNull Project closingProject) {
                        if (closingProject == project) {
                            recordProjectClosing();
                            ModuleDiagramAutoSaver.saveOnProjectClose(project);
                        }
                    }
                });
    }

    void recordEditorOpened() {
        state.open = true;
    }

    void recordEditorClosed() {
        if (!projectClosing) state.open = false;
    }

    void recordProjectClosing() {
        projectClosing = true;
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
        disposed = true; // Cancels any queued debugger-driven editor selection as well as the bus connection.
    }
}
