package org.ikasan.studio.integration.sftp;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;

/** Real loopback SFTP transport for manual development runs; files survive server restarts for inspection. */
public final class LocalSftpHarness implements AutoCloseable {
    private final SshServer server;
    private final Path home;
    private final Path knownHosts;
    private final String sessionId = UUID.randomUUID().toString();
    private final String password = UUID.randomUUID().toString();

    private LocalSftpHarness(Path root) throws IOException {
        Files.createDirectories(root);
        home = root.resolve("home");
        Files.createDirectories(home);
        knownHosts = root.resolve("known_hosts");
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1");
        server.setPort(0);
        var keys = new SimpleGeneratorHostKeyProvider(root.resolve("host-key.ser"));
        keys.setAlgorithm("RSA");
        keys.setKeySize(2048);
        server.setKeyPairProvider(keys);
        server.setSignatureFactories(List.of(BuiltinSignatures.rsaSHA512, BuiltinSignatures.rsaSHA256));
        server.setPasswordAuthenticator((user, secret, session) -> "ikasan".equals(user) && password.equals(secret));
        server.setPublickeyAuthenticator((user, key, session) -> false);
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(home));
    }

    /** Starts on an OS-allocated port. Partial startup failures release the listener. Call off the EDT. */
    public static LocalSftpHarness start(Path root) throws Exception {
        LocalSftpHarness harness = new LocalSftpHarness(root);
        try {
            harness.server.start();
            var key = harness.server.getKeyPairProvider().loadKeys(null).iterator().next().getPublic();
            Files.writeString(harness.knownHosts, "[127.0.0.1]:" + harness.port() + " " + PublicKeyEntry.toString(key) + "\n");
            return harness;
        } catch (Exception | Error failure) {
            try { harness.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    /** Stable, traversal-safe directory names shared with the generated development override. */
    public static String directory(String flow, String component) {
        return "flow-" + java.net.URLEncoder.encode(flow, java.nio.charset.StandardCharsets.UTF_8)
                + "/component-" + java.net.URLEncoder.encode(component, java.nio.charset.StandardCharsets.UTF_8);
    }
    /** Non-blocking listener state for the canvas; performs no network probe. */
    public boolean isRunning() { return server.isStarted() && !server.isClosing(); }
    public int port() { return server.getPort(); }
    public Path home() { return home; }
    /** Ephemeral launch overrides; never save these credentials in the model or log them. */
    public Map<String, String> launchProperties() {
        return Map.of("studio.test.sftp.session", sessionId, "studio.test.sftp.enabled", "true", "studio.test.sftp.port", Integer.toString(port()),
                "studio.test.sftp.password", password, "studio.test.sftp.home", home.toString(),
                "studio.test.sftp.knownHosts", knownHosts.toString());
    }
    public RemoteFilesClient.Connection connection() {
        return new RemoteFilesClient.Connection("127.0.0.1", port(), "ikasan", password, "", "", knownHosts.toString());
    }
    @Override public void close() throws IOException { server.stop(true); }
}
