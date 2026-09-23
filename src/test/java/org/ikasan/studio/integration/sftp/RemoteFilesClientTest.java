package org.ikasan.studio.integration.sftp;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RemoteFilesClientTest {
    @TempDir Path temporary;
    private SshServer server;
    private Path root, hosts, downloads;
    @BeforeEach void start() throws Exception {
        root = Files.createDirectory(temporary.resolve("remote"));
        downloads = Files.createDirectory(temporary.resolve("downloads"));
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1"); server.setPort(0);
        var keys = new SimpleGeneratorHostKeyProvider(temporary.resolve("host.key"));
        server.setKeyPairProvider(keys);
        server.setPasswordAuthenticator((user, password, session) -> user.equals("demo") && password.equals("test-password"));
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(root));
        server.start();
        hosts = temporary.resolve("known_hosts");
        Files.writeString(hosts, "[127.0.0.1]:" + server.getPort() + " " + PublicKeyEntry.toString(keys.loadKeys(null).iterator().next().getPublic()) + "\n");
    }
    @AfterEach void stop() throws Exception { if (server != null) server.stop(true); }
    private RemoteFilesClient.Connection config() {
        return new RemoteFilesClient.Connection("127.0.0.1", server.getPort(), "demo", "test-password", "", "", hosts.toString());
    }
    private RemoteFilesClient connected() throws Exception {
        var client = new RemoteFilesClient();
        try { client.connect(config(), temporary); return client; }
        catch (Exception e) { client.close(); throw e; }
    }
    @Test void listsNavigatesDownloadsAndDeletesLiteralFilenames() throws Exception {
        Files.createDirectory(root.resolve("out"));
        Files.writeString(root.resolve("out/order [1]*.txt"), "order payload");
        Files.writeString(root.resolve("out/keep.txt"), "keep");
        try (var client = connected()) {
            var top = client.list("/");
            assertTrue(top.entries().get(0).directory());
            var listing = client.list(RemoteFilesClient.child(top.directory(), "out"));
            assertEquals("/out", listing.directory());
            var entry = listing.entries().stream().filter(e -> e.name().contains("[1]")).findFirst().orElseThrow();
            assertTrue(entry.regular());
            Path downloaded = client.download(listing.directory(), entry, downloads);
            assertEquals("order payload", Files.readString(downloaded));
            assertThrows(FileAlreadyExistsException.class, () -> client.download(listing.directory(), entry, downloads));
            assertEquals("order payload", Files.readString(downloaded));
            client.delete(listing.directory(), entry);
            assertFalse(Files.exists(root.resolve("out/order [1]*.txt")));
            assertTrue(Files.exists(root.resolve("out/keep.txt")));
        }
    }
    @Test void refusesDirectoriesLinksAndChangedFiles() throws Exception {
        Files.createDirectory(root.resolve("sub"));
        Files.writeString(root.resolve("order.txt"), "one");
        Files.createSymbolicLink(root.resolve("link.txt"), Path.of("order.txt"));
        try (var client = connected()) {
            var listing = client.list("/");
            for (var entry : listing.entries()) {
                if (!entry.regular()) {
                    assertThrows(java.io.IOException.class, () -> client.delete("/", entry));
                    assertThrows(java.io.IOException.class, () -> client.download("/", entry, downloads));
                }
            }
            var old = listing.entries().stream().filter(e -> e.name().equals("order.txt")).findFirst().orElseThrow();
            Files.writeString(root.resolve("order.txt"), "changed contents");
            assertThrows(java.io.IOException.class, () -> client.delete("/", old));
            assertThrows(java.io.IOException.class, () -> client.download("/", old, downloads));
            assertTrue(Files.exists(root.resolve("order.txt")));
            assertTrue(Files.isSymbolicLink(root.resolve("link.txt")));
            assertTrue(Files.isDirectory(root.resolve("sub")));
        }
    }
    @Test void rejectsUntrustedAndChangedHostKeys() throws Exception {
        Files.writeString(hosts, "");
        try (var client = new RemoteFilesClient()) {
            assertThrows(Exception.class, () -> client.connect(config(), temporary));
        }
        var other = new SimpleGeneratorHostKeyProvider(temporary.resolve("other.key"));
        Files.writeString(hosts, "[127.0.0.1]:" + server.getPort() + " " + PublicKeyEntry.toString(other.loadKeys(null).iterator().next().getPublic()) + "\n");
        try (var client = new RemoteFilesClient()) {
            assertThrows(Exception.class, () -> client.connect(config(), temporary));
        }
    }
    @Test void supportsPrivateKeyAuthentication() throws Exception {
        var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var pair = generator.generateKeyPair();
        server.setPublickeyAuthenticator((user, key, session) -> user.equals("demo") && key.equals(pair.getPublic()));
        Path key = temporary.resolve("id_rsa");
        Files.writeString(key, "-----BEGIN PRIVATE KEY-----\n" + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n");
        try (var client = new RemoteFilesClient()) {
            client.connect(new RemoteFilesClient.Connection("127.0.0.1", server.getPort(), "demo", "", key.toString(), "", hosts.toString()), temporary);
            assertEquals("/", client.list("/").directory());
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"RSA", "Ed25519"})
    void supportsEncryptedOpenSshKeys(String algorithm) throws Exception {
        var generator = org.apache.sshd.common.util.security.SecurityUtils.getKeyPairGenerator(algorithm.equals("Ed25519") ? "EdDSA" : algorithm);
        if (algorithm.equals("RSA")) generator.initialize(2048);
        var pair = generator.generateKeyPair();
        server.setPublickeyAuthenticator((user, key, session) -> user.equals("demo") && key.equals(pair.getPublic()));
        Path key = temporary.resolve("encrypted_key");
        var encryption = new org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext();
        encryption.setPassword("test-passphrase"); encryption.setCipherType("256");
        try (var output = Files.newOutputStream(key)) {
            org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter.INSTANCE
                    .writePrivateKey(pair, "test", encryption, output);
        }
        try (var client = new RemoteFilesClient()) {
            client.connect(new RemoteFilesClient.Connection("127.0.0.1", server.getPort(), "demo", "", key.toString(), "test-passphrase", hosts.toString()), temporary);
            assertEquals("/", client.list("/").directory());
        }
    }
    @Test void rechecksLinksAndRejectsActionsAfterClose() throws Exception {
        Files.writeString(root.resolve("order.txt"), "one");
        try (var client = connected()) {
            var entry = client.list("/").entries().get(0);
            Files.delete(root.resolve("order.txt"));
            Files.createSymbolicLink(root.resolve("order.txt"), Path.of("elsewhere.txt"));
            assertThrows(java.io.IOException.class, () -> client.delete("/", entry));
            assertThrows(java.io.IOException.class, () -> client.download("/", entry, downloads));
            client.close();
            assertThrows(java.io.InterruptedIOException.class, () -> client.list("/"));
        }
    }
    @Test void rejectsUnsafeChildNamesAndUnresolvedPaths() {
        for (String name : List.of("..", "../file", "a/b", "a\\b", "a\nb"))
            assertThrows(java.io.IOException.class, () -> RemoteFilesClient.child("/out", name));
        assertThrows(java.io.IOException.class, () -> RemoteFilesClient.localPath("${KEY}", temporary));
    }
}
