package org.ikasan.studio.integration.sftp;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalSftpHarnessTest {
    @TempDir Path temporary;
    public static class Endpoint extends LocalSftpFlowTestFixtureTest.Endpoint {
        String configurationId = "external-settings";
        public void setConfiguredResourceId(String value) { configurationId = value; }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"}) @Timeout(60)
    void generatedOverridesDeliverAllPayloadsAndPreserveExternalMode(String pack) throws Exception {
        String helper = Files.readString(Path.of("src/main/resources/studio/metapack", pack,
                "templates/org/ikasan/studio/generator/localSftpHarness_en.ftl"));
        Path source = temporary.resolve("HarnessSettings.java");
        Files.writeString(source, "public class HarnessSettings { public static Object configure(Object endpoint, boolean consumer) {"
                + "return studioLocalSftp(endpoint, \"My flow\", consumer ? \"input\" : \"output\", consumer); }" + helper + "}");
        var compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", pack.equals("V3.3.9") ? "11" : "17", "-d", temporary.toString(), source.toString())
                .redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes());
        assertEquals(0, compiler.waitFor(), diagnostics);
        // These are file-transfer examples, not a fixed batch count in the flow-test runner.
        // Both transfer directions check every filename/payload pair in this collection.
        var expectedFiles = List.of(
                Map.entry("first.txt", "First payload"),
                Map.entry("second.txt", "Second payload"),
                Map.entry("later.txt", "Later payload on the same connection"));
        int port;
        Map<String, String> previous = new HashMap<>();
        try (var server = LocalSftpHarness.start(temporary.resolve("server"));
             var loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, getClass().getClassLoader())) {
            port = server.port();
            var method = loader.loadClass("HarnessSettings").getMethod("configure", Object.class, boolean.class);
            var external = new Endpoint();
            assertSame(external, method.invoke(null, external, true));
            assertEquals("external-settings", external.configurationId);
            assertEquals("existing-algorithm", external.configuration.keyExchangeAlgorithm);
            server.launchProperties().forEach((key, value) -> { previous.put(key, System.getProperty(key)); System.setProperty(key, value); });
            var consumer = new Endpoint();
            var producer = new Endpoint();
            method.invoke(null, consumer, true);
            method.invoke(null, producer, false);
            assertTrue(consumer.configurationId.startsWith("studio-test-sftp-"));
            assertNotEquals(consumer.configurationId, producer.configurationId);
            assertEquals(port, consumer.configuration.port);
            assertEquals(0, consumer.configuration.age);
            assertEquals("", consumer.configuration.key);
            assertNull(consumer.configuration.keyExchangeAlgorithm);
            assertNotEquals(consumer.configuration.directory, producer.configuration.directory);
            assertEquals("/" + LocalSftpHarness.directory("My flow", "input"), consumer.configuration.directory);
            try (var client = new RemoteFilesClient()) {
                client.connect(server.connection(), temporary);
                for (var expectedFile : expectedFiles) {
                    Path file = server.home().resolve(consumer.configuration.directory.substring(1)).resolve(expectedFile.getKey());
                    Files.writeString(file, expectedFile.getValue());
                    var entry = client.list(consumer.configuration.directory).entries().stream()
                            .filter(e -> e.name().equals(expectedFile.getKey())).findFirst().orElseThrow();
                    assertEquals(expectedFile.getValue(), Files.readString(client.download(consumer.configuration.directory, entry, temporary)));
                }
                assertTrue(client.list(producer.configuration.directory).entries().isEmpty());
                try (var ssh = org.apache.sshd.client.SshClient.setUpDefaultClient()) {
                    ssh.setServerKeyVerifier(new org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier(
                            org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier.INSTANCE,
                            Path.of(producer.configuration.hosts)));
                    ssh.start();
                    try (var session = ssh.connect("ikasan", "127.0.0.1", port).verify(5000).getSession()) {
                        session.addPasswordIdentity(producer.configuration.password);
                        session.auth().verify(5000);
                        try (var sftp = org.apache.sshd.sftp.client.SftpClientFactory.instance().createSftpClient(session)) {
                            for (var expectedFile : expectedFiles) {
                                try (var stream = sftp.write(producer.configuration.directory + "/" + expectedFile.getKey())) {
                                    stream.write(expectedFile.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }
                                assertEquals(expectedFile.getValue(), Files.readString(server.home().resolve(
                                        producer.configuration.directory.substring(1)).resolve(expectedFile.getKey())));
                            }
                        }
                    }
                }
            }
            // A separate project owns a different listener, key and file tree.
            try (var other = LocalSftpHarness.start(temporary.resolve("other"))) {
                assertNotEquals(port, other.port());
                assertNotEquals(server.home(), other.home());
            }
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
        try (var socket = new Socket()) {
            assertThrows(java.io.IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 1000));
        }
        assertTrue(Files.exists(temporary.resolve("server/home")), "Manual harness files remain available for inspection");
    }
}
