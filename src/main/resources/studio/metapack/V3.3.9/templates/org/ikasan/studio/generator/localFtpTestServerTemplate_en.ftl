package org.ikasan.studio.flowtests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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

    private LocalFtpTestServer(Map<String, String> properties) throws IOException {
        username = properties.getOrDefault("test.ftp.username", "ikasan");
        password = properties.getOrDefault("test.ftp.password", username);
        if (username.isBlank() || password.isBlank()) throw new IllegalArgumentException("Test FTP credentials must not be blank");
        root = Files.createTempDirectory("studio-flow-test-ftp-");
    }

    /** Binds directly to port zero; no free-port probe/race. Cleans partial startup failures. */
    public static LocalFtpTestServer start(Map<String, String> properties) throws Exception {
        LocalFtpTestServer fixture = new LocalFtpTestServer(properties);
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

    /** Applies pack-derived property keys; refuses incomplete mappings instead of contacting a real endpoint. */
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

    /** Seed consumer input here or inspect producer delivery before the context closes. */
    public Path root() { return root; }
    public int port() { return port; }

    /** Stops only this owned server and removes only its unique temporary home. Safe to call twice. */
    @Override public void close() throws IOException {
        if (server != null) { server.stop(); server = null; }
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList())) Files.deleteIfExists(path);
            }
        }
    }
}
