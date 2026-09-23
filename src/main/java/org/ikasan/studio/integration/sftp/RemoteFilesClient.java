package org.ikasan.studio.integration.sftp;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** One background operation per connection. Never logs credentials; rejects links when checking file operations. */
public final class RemoteFilesClient implements AutoCloseable {
    public static final class ChangedFileException extends IOException {
        public ChangedFileException() { super("Remote file changed; refresh before continuing."); }
    }
    public static final int MAX_ENTRIES = 10000;
    public static final class Connection {
        public final String host, username, password, keyFile, passphrase, knownHosts;
        public final int port;
        public Connection(String host, int port, String username, String password, String keyFile,
                          String passphrase, String knownHosts) {
            if (host.isBlank() || username.isBlank() || port < 1 || port > 65535)
                throw new IllegalArgumentException("Host, username and port (1–65535) are required.");
            this.host = host; this.port = port; this.username = username; this.password = password;
            this.keyFile = keyFile; this.passphrase = passphrase; this.knownHosts = knownHosts;
        }
    }
    public record Entry(String name, boolean directory, boolean regular, long size, long modified) { }
    public record Listing(String directory, List<Entry> entries, boolean truncated) { }
    private final SshClient client;
    private volatile ClientSession session;
    private SftpClient sftp;
    private volatile boolean closed;

    public RemoteFilesClient() {
        client = SshClient.setUpDefaultClient();
        // Do not silently load unrelated ~/.ssh/config entries or default identities.
        client.setHostConfigEntryResolver(org.apache.sshd.client.config.hosts.HostConfigEntryResolver.EMPTY);
        client.setKeyIdentityProvider(org.apache.sshd.common.keyprovider.KeyIdentityProvider.EMPTY_KEYS_PROVIDER);
        org.apache.sshd.core.CoreModuleProperties.IDLE_TIMEOUT.set(client, Duration.ofSeconds(30));
    }
    public void connect(Connection config, Path projectRoot) throws Exception {
        checkOpen();
        Path hosts = localPath(config.knownHosts, projectRoot);
        if (!Files.isRegularFile(hosts)) throw new IOException("known_hosts file is missing: " + hosts);
        client.setServerKeyVerifier(new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, hosts));
        client.setUserAuthFactories(config.password.isEmpty()
                ? List.of(org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyFactory.INSTANCE)
                : List.of(org.apache.sshd.client.auth.password.UserAuthPasswordFactory.INSTANCE));
        client.start();
        session = client.connect(config.username, config.host, config.port).verify(Duration.ofSeconds(15)).getSession();
        checkOpen();
        if (!config.password.isEmpty()) session.addPasswordIdentity(config.password);
        else {
            var keys = new FileKeyPairProvider(localPath(config.keyFile, projectRoot));
            keys.setPasswordFinder((s, resource, retry) -> retry == 0 ? config.passphrase : null);
            for (var key : keys.loadKeys(session)) session.addPublicKeyIdentity(key);
        }
        session.auth().verify(Duration.ofSeconds(15));
        checkOpen();
        sftp = SftpClientFactory.instance().createSftpClient(session);
        checkOpen();
    }
    public Listing list(String directory) throws IOException {
        checkOpen();
        String canonical = sftp.canonicalPath(directory.isBlank() ? "." : directory);
        List<Entry> entries = new ArrayList<>();
        boolean truncated = false;
        try (var handle = sftp.openDir(canonical)) {
            java.util.List<SftpClient.DirEntry> batch;
            outer: while ((batch = sftp.readDir(handle)) != null) {
                for (var item : batch) {
                    checkOpen();
                    String name = item.getFilename();
                    if (name.equals(".") || name.equals("..")) continue;
                    validName(name);
                    if (entries.size() == MAX_ENTRIES) { truncated = true; break outer; }
                    var attr = item.getAttributes();
                    entries.add(new Entry(name, attr.isDirectory() && !attr.isSymbolicLink(),
                            attr.isRegularFile() && !attr.isSymbolicLink(), attr.getSize(), attr.getModifyTime().toMillis()));
                }
            }
        }
        entries.sort(Comparator.comparing(Entry::directory).reversed().thenComparing(Entry::name));
        return new Listing(canonical, List.copyOf(entries), truncated);
    }
    public void delete(String directory, Entry entry) throws IOException {
        String path = checkedFile(directory, entry);
        sftp.remove(path);
    }
    public Path download(String directory, Entry entry, Path folder) throws IOException {
        String remote = checkedFile(directory, entry);
        Path destination = folder.resolve(entry.name());
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(destination.toString());
        Path temp = Files.createTempFile(folder, ".studio-sftp-", ".part");
        try {
            try (var input = sftp.read(remote); var output = Files.newOutputStream(temp)) {
                byte[] bytes = new byte[65536];
                int count;
                while ((count = input.read(bytes)) != -1) { checkOpen(); output.write(bytes, 0, count); }
            }
            checkedFile(directory, entry);
            if (Files.size(temp) != entry.size()) throw new ChangedFileException();
            // No REPLACE_EXISTING: protect local files even if one appeared during the download.
            return Files.move(temp, destination);
        } finally { Files.deleteIfExists(temp); }
    }
    private String checkedFile(String directory, Entry entry) throws IOException {
        checkOpen();
        if (!entry.regular()) throw new IOException("Only regular files can be downloaded or deleted.");
        String path = child(directory, entry.name());
        var current = sftp.lstat(path);
        if (!current.isRegularFile() || current.isSymbolicLink() || current.getSize() != entry.size()
                || current.getModifyTime().toMillis() != entry.modified())
            throw new ChangedFileException();
        return path;
    }
    public static String child(String directory, String name) throws IOException {
        validName(name);
        return (directory.endsWith("/") ? directory : directory + "/") + name;
    }
    private static void validName(String name) throws IOException {
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl)) throw new IOException("Unsupported remote filename.");
    }
    public static Path localPath(String value, Path root) throws IOException {
        if (value.isBlank() || value.contains("${")) throw new IOException("Provide a resolved local file path.");
        if (value.equals("~")) value = System.getProperty("user.home");
        else if (value.startsWith("~/")) value = System.getProperty("user.home") + value.substring(1);
        return root.resolve(value).normalize();
    }
    private void checkOpen() throws IOException {
        if (closed || Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Operation cancelled.");
    }
    @Override public void close() {
        closed = true;
        if (session != null) session.close(true);
        client.close(true);
    }
}
