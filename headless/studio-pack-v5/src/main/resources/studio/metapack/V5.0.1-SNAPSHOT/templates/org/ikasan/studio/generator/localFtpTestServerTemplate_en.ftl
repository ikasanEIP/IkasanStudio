package org.ikasan.studio.flowtests.support.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

/** Real FTP transport for tests, with a fresh loopback listener and disposable home per context. */
public final class LocalFtpTestServer implements AutoCloseable {
    private final Path root;
    private final String username;
    private final String password;
    private FtpServer server;
    private int port;

    private LocalFtpTestServer(Map<String, String> properties, Path directory) throws IOException {
        username = properties.getOrDefault("test.ftp.username", "ikasan");
        password = properties.getOrDefault("test.ftp.password", username);
        if (username.isBlank() || password.isBlank()) throw new IllegalArgumentException("Test FTP credentials must not be blank");
        if (!Files.isDirectory(directory) || Files.isSymbolicLink(directory)) {
            throw new IOException("Test FTP home must be an existing temporary directory: " + directory);
        }
        root = directory;
    }

    /** Binds directly to port zero; no free-port probe/race. Cleans partial startup failures. */
    /**
     * Starts a loopback-only FTP server on an allocated port using a caller-owned home directory.
     * @param properties settings containing optional {@code test.ftp.username/password}; defaults
     *                   are {@code ikasan}, with password defaulting to the username
     * @param directory existing JUnit temporary directory; this fixture never deletes it
     * @return the running fixture; close it before the owning JUnit rule deletes the directory
     * @throws Exception if settings, directory validation or server startup fail
     */
    public static LocalFtpTestServer start(Map<String, String> properties, Path directory) throws Exception {
        LocalFtpTestServer fixture = new LocalFtpTestServer(properties, directory);
        try {
            FtpServerFactory factory = new FtpServerFactory();
            ListenerFactory listener = new ListenerFactory();
            listener.setServerAddress("127.0.0.1");
            listener.setPort(0);
            factory.addListener("default", listener.createListener());
            BaseUser user = new BaseUser();
            user.setName(fixture.username);
            user.setPassword(fixture.password);
            user.setHomeDirectory(fixture.root.toString());
            user.setAuthorities(List.of(new WritePermission()));
            factory.getUserManager().save(user);
            fixture.server = factory.createServer();
            fixture.server.start();
            fixture.port = factory.getListener("default").getPort();
            if (fixture.port <= 0) throw new IllegalStateException("FTP listener did not publish its allocated port");
            return fixture;
        } catch (Exception | Error failure) {
            try { fixture.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /**
     * Overrides one endpoint's Spring properties with this fixture's address and credentials.
     * Invoke before creating the application context. Keys come from the selected meta-pack.
     * Remote directory becomes {@code /}, mapped to {@link #root()} on this server.
     * @param properties mutable test-only Spring property overrides
     * @param name flow/component description used in diagnostics
     * @param secure true for FTPS, which this plain FTP fixture deliberately rejects
     * @param hostKey externalised host property key
     * @param portKey externalised port property key
     * @param userKey externalised username property key
     * @param passwordKey externalised password property key
     * @param directoryKey consumer source or producer output directory property key
     * @throws IllegalStateException if FTPS is requested or any mapping is missing
     */
    public void configure(Map<String, String> properties, String name, boolean secure,
                          String hostKey, String portKey, String userKey, String passwordKey, String directoryKey) {
        if (secure || List.of(hostKey, portKey, userKey, passwordKey, directoryKey).contains("")) {
            throw new IllegalStateException("Local plain FTP fixture cannot override " + name
                    + ": configure plain FTP and externalised host, port, username, password and directory in Studio, then regenerate shared setup.");
        }
        properties.put(hostKey, "127.0.0.1");
        properties.put(portKey, Integer.toString(port));
        properties.put(userKey, username);
        properties.put(passwordKey, password);
        properties.put(directoryKey, "/");
    }

    /**
     * Configures a local test FTP consumer to discover newly published, complete fixture files.
     * Call before flow startup. The default zero bypasses the endpoint's production minimum age;
     * duplicate detection and filename filtering remain unchanged. To test age filtering itself,
     * set {@code test.ftp.consumer.min-age-seconds} explicitly and prepare suitably aged fixtures.
     * @param consumer pack-compatible consumer exposing {@code getConfiguration().setMinAge(Long)}
     * @param properties test settings; the minimum age must be a non-negative whole number of seconds
     * @throws Exception if the setting is invalid or the consumer lacks the expected configuration API
     */
    public void configureConsumer(Object consumer, Map<String, String> properties) throws Exception {
        String value = properties.getOrDefault("test.ftp.consumer.min-age-seconds", "0");
        long seconds;
        try { seconds = Long.parseLong(value); }
        catch (NumberFormatException failure) {
            throw new IllegalArgumentException("test.ftp.consumer.min-age-seconds must be a non-negative whole number", failure);
        }
        if (seconds < 0) throw new IllegalArgumentException("test.ftp.consumer.min-age-seconds must be non-negative");
        Object configuration = consumer.getClass().getMethod("getConfiguration").invoke(consumer);
        configuration.getClass().getMethod("setMinAge", Long.class).invoke(configuration, seconds);
    }

    /**
     * @return the server's local home for seeding consumer input or checking producer delivery;
     *         retained across batches and removed by the owning JUnit temporary-folder rule
     */
    public Path root() { return root; }
    /** @return the allocated FTP control port, valid while the fixture is running */
    public int port() { return port; }

    /** Stops this test server; the enclosing JUnit TemporaryFolder owns file cleanup. Safe to call twice. */
    @Override public void close() {
        if (server != null) { server.stop(); server = null; }
    }
}
