package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.integration.sftp.LocalSftpHarness;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.nio.file.Path;
import java.util.Map;

/** Project-owned SFTP mode and server. Only the mode is persisted in the local IDE workspace. */
@Service(Service.Level.PROJECT)
@State(name = "StudioSftpHarness", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class TestSftpServerService implements PersistentStateComponent<TestSftpServerService.Settings>, Disposable {
    public static final class Settings { public String mode = "UNSELECTED"; }
    private Settings settings = new Settings();
    private final Project project;
    private LocalSftpHarness server;
    private boolean disposed;
    public TestSftpServerService(Project project) { this.project = project; }
    @Override public Settings getState() { return settings; }
    @Override public void loadState(Settings state) { settings = state; }
    public synchronized boolean isLocal() { return "LOCAL".equals(settings.mode); }
    public synchronized boolean isSelected() { return !"UNSELECTED".equals(settings.mode); }
    public synchronized void setLocal(boolean local) { settings.mode = local ? "LOCAL" : "EXTERNAL"; }
    public static boolean hasSftp(Module module) {
        return module != null && module.getFlows() != null && module.getFlows().stream()
                .flatMap(flow -> flow.getFlowElementsNoExternalEndPoints().stream())
                .anyMatch(element -> "SFTP Endpoint".equals(element.getComponentMeta().getEndpointKey()));
    }
    /** Starts once per project. No file or network work belongs on the EDT. */
    public synchronized LocalSftpHarness start() throws Exception {
        if (disposed || project.isDisposed()) throw new IllegalStateException("Project is closed");
        if (!isLocal()) throw new IllegalStateException("Select local SFTP mode first");
        if (server == null) server = LocalSftpHarness.start(Path.of(project.getBasePath(), "temporary-files", "test-data", "sftp"));
        return server;
    }
    public synchronized Map<String, String> launchProperties() {
        if (!isLocal()) return Map.of();
        if (server == null) throw new IllegalStateException("Start the local SFTP harness before running the module.");
        return server.launchProperties();
    }
    public synchronized void stop() throws Exception {
        if (server != null) { server.close(); server = null; }
    }
    @Override public void dispose() {
        LocalSftpHarness owned;
        synchronized (this) { disposed = true; owned = server; server = null; }
        if (owned == null) return;
        Runnable cleanup = () -> {
            try { owned.close(); } catch (Exception failure) {
                com.intellij.openapi.diagnostic.Logger.getInstance(TestSftpServerService.class).warn("Could not stop local SFTP harness", failure);
            }
        };
        var application = ApplicationManager.getApplication();
        if (application == null) cleanup.run(); else application.executeOnPooledThread(cleanup);
    }
}
