package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.ui.UiContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.intellij.openapi.application.Application;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

/** Project-owned embedded FTP server used to exercise local FTP consumers. */
@Service(Service.Level.PROJECT)
public final class TestFtpServerService implements Disposable {
    private final Project project;
    private FtpServer server;
    private volatile HarnessStopImpact.Endpoint listeningEndpoint;
    public HarnessStopImpact.Endpoint listeningEndpoint() { return disposed ? null : listeningEndpoint; }
    private TestFtpServerConfiguration configuration;
    private Path rootDirectory;
    private boolean localHarness;
    private volatile boolean disposed;

    public TestFtpServerService(Project project) { this.project = project; }

    public synchronized Path start(TestFtpServerConfiguration requested) throws Exception {
        return start(requested, false);
    }

    /** Bind port zero directly so the OS reserves a free port without a probe/close/rebind race. */
    public synchronized Path startLocal() throws Exception {
        if (disposed || project.isDisposed()) throw new IllegalStateException("Project is disposed");
        if (isRunning() && !localHarness)
            throw new IllegalStateException(org.ikasan.studio.ui.StudioBundle.message("harnessOverrides.stopConfiguredFtp"));
        if (isRunning()) return rootDirectory;
        return start(new TestFtpServerConfiguration("127.0.0.1", 0, "ikasan", "ikasan"), true);
    }

    /** Port zero means unallocated; it is never used for a module launch. Safe for canvas reads. */
    public synchronized TestFtpServerConfiguration localConfiguration() {
        return isRunning() && localHarness ? configuration
                : new TestFtpServerConfiguration("127.0.0.1", 0, "ikasan", "ikasan");
    }

    public synchronized java.util.Map<String, String> launchProperties() {
        if (!isRunning() || !localHarness)
            throw new IllegalStateException(org.ikasan.studio.ui.StudioBundle.message("harnessOverrides.startFtp"));
        return java.util.Map.of("studio.harness.ftp.host", configuration.host(),
                "studio.harness.ftp.port", Integer.toString(configuration.port()),
                "studio.harness.ftp.username", configuration.username(),
                "studio.harness.ftp.password", configuration.password(),
                "studio.harness.ftp.home", rootDirectory.toString());
    }

    private Path start(TestFtpServerConfiguration requested, boolean local) throws Exception {
        if (disposed || project.isDisposed()) throw new IllegalStateException("Project is disposed");
        if (!isLocalHost(requested.host())) throw new IllegalArgumentException("remoteHost must be local");
        if (isRunning()) {
            // Address match, not record equality - a Producer configured for "localhost" asking to start the
            // server a Consumer already started at "127.0.0.1" is asking for the server it already has.
            if (requested.sameAddressAs(configuration)) return rootDirectory;
            throw new IllegalStateException("A Studio test FTP server is already running at " + configuration.address());
        }
        if (!local) assertPortAvailable(requested);
        rootDirectory = testRoot(project);
        Files.createDirectories(rootDirectory);
        Path welcomeFile = rootDirectory.resolve("test-file.txt");
        if (Files.notExists(welcomeFile)) Files.writeString(welcomeFile, "Ikasan Studio FTP test file\n", StandardCharsets.UTF_8);

        FtpServerFactory factory = new FtpServerFactory();
        ListenerFactory listener = new ListenerFactory();
        listener.setServerAddress(InetAddress.getByName(requested.host()).getHostAddress());
        listener.setPort(requested.port());
        factory.addListener("default", listener.createListener());
        BaseUser user = new BaseUser();
        user.setName(requested.username());
        user.setPassword(requested.password());
        user.setHomeDirectory(rootDirectory.toString());
        user.setAuthorities(List.of(new WritePermission()));
        factory.getUserManager().save(user);
        FtpServer candidate = factory.createServer();
        try {
            candidate.start();
        } catch (Exception failure) {
            try { candidate.stop(); } catch (RuntimeException ignored) { }
            throw failure;
        }
        server = candidate;
        configuration = new TestFtpServerConfiguration(requested.host(), factory.getListener("default").getPort(),
                requested.username(), requested.password());
        localHarness = local;
        listeningEndpoint = new HarnessStopImpact.Endpoint("FTP", configuration.host(), configuration.port());
        repaintCanvas();
        return rootDirectory;
    }

    public synchronized boolean isRunning() { return server != null && !server.isStopped() && !server.isSuspended(); }
    /**
     * True if the running test server is the one this component addresses. Compares host/port only (loopback
     * spellings treated as equal - see {@link TestFtpServerConfiguration#sameAddressAs}), so an FTP Producer
     * written as "localhost" still recognises the server an FTP Consumer started as "127.0.0.1".
     */
    public synchronized boolean isRunningAt(TestFtpServerConfiguration requested) {
        return isRunning() && requested != null && requested.sameAddressAs(configuration);
    }
    public synchronized Path getRootDirectory() { return rootDirectory; }
    public synchronized Path getOrCreateTestFile() throws java.io.IOException {
        if (!isRunning() || rootDirectory == null) return null;
        Path testFile = rootDirectory.resolve("test-file.txt");
        if (Files.notExists(testFile))
            Files.writeString(testFile, "Ikasan Studio FTP test file\n", StandardCharsets.UTF_8);
        return testFile;
    }
    public synchronized void stop() {
        if (server != null) server.stop();
        server = null;
        configuration = null;
        listeningEndpoint = null;
        localHarness = false;
        repaintCanvas();
    }
    private void repaintCanvas() {
        Application application = ApplicationManager.getApplication();
        // Plain unit tests intentionally exercise the embedded server without booting an IDE application.
        if (application == null) return;
        application.invokeLater(() -> {
            if (disposed || project.isDisposed()) return;
            DesignerCanvas canvas = project.getService(UiContext.class).getDesignerCanvas();
            if (canvas == null || canvas.isDisposed()) return;
            canvas.setInitialiseAllDimensions(true);
            canvas.repaint();
        });
    }
    private static void assertPortAvailable(TestFtpServerConfiguration requested) throws Exception {
        try (ServerSocket probe = new ServerSocket()) {
            probe.setReuseAddress(false);
            probe.bind(new InetSocketAddress(InetAddress.getByName(requested.host()), requested.port()));
        }
    }

    static boolean isLocalHost(String host) throws Exception {
        for (InetAddress address : InetAddress.getAllByName(host)) if (address.isLoopbackAddress()) return true;
        return false;
    }
    static Path testRoot(Project project) {
        String basePath = project.getBasePath();
        if (basePath != null && !basePath.isBlank()) {
            return Path.of(basePath, "temporary-files", "test-data", "ftp");
        }
        String projectName = project.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        return Path.of(PathManager.getSystemPath(), "ikasan-studio", "ftp", projectName, "root");
    }
    @Override public void dispose() {
        disposed = true;
        FtpServer running;
        synchronized (this) {
            running = server;
            server = null;
            configuration = null;
            listeningEndpoint = null;
            localHarness = false;
        }
        if (running != null) {
            Application application = ApplicationManager.getApplication();
            if (application == null) {
                running.stop();
            } else {
                application.executeOnPooledThread(running::stop);
            }
        }
    }
}
