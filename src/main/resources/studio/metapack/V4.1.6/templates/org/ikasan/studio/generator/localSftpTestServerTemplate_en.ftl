package org.ikasan.studio.flowtests.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

/** Real SFTP transport with test-only credentials, trusted host key and separate endpoint homes. */
public final class LocalSftpTestServer implements AutoCloseable {
    private final Path home;
    private final Path knownHosts;
    private final String username;
    private final String password;
    private final Map<List<String>, Path> directories = new LinkedHashMap<>();
    private SshServer server;

    private LocalSftpTestServer(Map<String, String> properties, Path temporary) throws IOException {
        if (!Files.isDirectory(temporary) || Files.isSymbolicLink(temporary))
            throw new IOException("SFTP fixture requires an existing JUnit temporary directory");
        username = properties.getOrDefault("test.sftp.username", "ikasan");
        password = properties.getOrDefault("test.sftp.password", username);
        if (username.isBlank() || password.isBlank()) throw new IllegalArgumentException("Test SFTP credentials must not be blank");
        home = Files.createDirectory(temporary.resolve("home"));
        knownHosts = temporary.resolve("known_hosts");
    }

    /**
     * Starts an embedded loopback server on an allocated port, with a fresh RSA host key.
     * @param properties optional test.sftp.username/password; password defaults to username
     * @param temporary caller-owned empty JUnit directory; close the server before JUnit removes it
     * @return running server; partial startup failures close any opened listener
     * @throws Exception if credentials, filesystem or SSH startup fail
     */
    public static LocalSftpTestServer start(Map<String, String> properties, Path temporary) throws Exception {
        LocalSftpTestServer fixture = new LocalSftpTestServer(properties, temporary);
        try {
            fixture.server = SshServer.setUpDefaultServer();
            fixture.server.setHost("127.0.0.1");
            fixture.server.setPort(0);
            var keys = new SimpleGeneratorHostKeyProvider(temporary.resolve("host-key.ser"));
            keys.setAlgorithm("RSA");
            keys.setKeySize(2048);
            fixture.server.setKeyPairProvider(keys);
            fixture.server.setSignatureFactories(List.of(BuiltinSignatures.rsaSHA512, BuiltinSignatures.rsaSHA256));
            fixture.server.setPasswordAuthenticator((user, secret, session) -> fixture.username.equals(user) && fixture.password.equals(secret));
            fixture.server.setPublickeyAuthenticator((user, key, session) -> false);
            fixture.server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
            fixture.server.setFileSystemFactory(new VirtualFileSystemFactory(fixture.home));
            fixture.server.start();
            Files.writeString(fixture.knownHosts, "[127.0.0.1]:" + fixture.port() + " "
                    + PublicKeyEntry.toString(keys.loadKeys(null).iterator().next().getPublic()) + "\n");
            return fixture;
        } catch (Exception | Error failure) {
            try { fixture.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /**
     * Overrides the real endpoint configuration before starting its flow. Ikasan builder proxies expose
     * the SFTP configuration through getConfiguration(). Uses the API contract supplied by this meta-pack.
     * Host checking stays enabled; personal SSH keys and configured remote directories are not used.
     * @param endpoint consumer or producer from the flow's component map
     * @param flow exact model flow name
     * @param component exact component name
     * @param consumer true for a scheduled SFTP consumer
     * @param properties test settings; test.sftp.consumer.min-age-seconds defaults to zero
     * @throws Exception if endpoint API, filesystem or minimum-age settings are invalid
     */
    public void configure(Object endpoint, String flow, String component, boolean consumer, Map<String, String> properties) throws Exception {
        Object config = endpoint.getClass().getMethod("getConfiguration").invoke(endpoint);
        Path directory = directories.get(List.of(flow, component));
        if (directory == null) {
            directory = Files.createTempDirectory(home, "endpoint-");
            directories.put(List.of(flow, component), directory);
        }
        set(config, "RemoteHost", String.class, "127.0.0.1");
        set(config, "RemotePort", Integer.class, port());
        set(config, "Username", String.class, username);
        set(config, "Password", String.class, password);
        set(config, "PrivateKeyFilename", String.class, "");
        set(config, "PrivateKeyPassphrase", String.class, "");
        set(config, "KnownHostsFilename", String.class, knownHosts.toString());
        set(config, "PreferredKeyExchangeAlgorithm", String.class, null);
        set(config, consumer ? "SourceDirectory" : "OutputDirectory", String.class, "/" + directory.getFileName());
        if (consumer) {
            long age;
            try { age = Long.parseLong(properties.getOrDefault("test.sftp.consumer.min-age-seconds", "0")); }
            catch (NumberFormatException failure) { throw new IllegalArgumentException("test.sftp.consumer.min-age-seconds must be a non-negative integer", failure); }
            if (age < 0) throw new IllegalArgumentException("test.sftp.consumer.min-age-seconds must be non-negative");
            set(config, "MinAge", Long.class, age);
        }
    }

    private static void set(Object config, String property, Class<?> type, Object value) throws Exception {
        config.getClass().getMethod("set" + property, type).invoke(config, value);
    }

    /** Returns a configured endpoint's local directory for copying inputs or asserting delivery. */
    public Path directory(String flow, String component) {
        Path directory = directories.get(List.of(flow, component));
        if (directory == null) throw new IllegalArgumentException("No local test SFTP directory configured for " + flow + " / " + component);
        return directory;
    }

    /** @return the allocated port while this server is running */
    public int port() { return server.getPort(); }

    /** Stops SSH sessions and listener; JUnit owns deletion of all files, including keys. Safe to repeat. */
    @Override public void close() throws IOException {
        if (server != null) { server.stop(true); server = null; }
    }
}
