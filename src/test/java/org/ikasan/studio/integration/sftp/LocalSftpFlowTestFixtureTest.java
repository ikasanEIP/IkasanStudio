package org.ikasan.studio.integration.sftp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalSftpFlowTestFixtureTest {
    @TempDir Path temporary;

    public static class Endpoint {
        final Configuration configuration = new Configuration();
        public Configuration getConfiguration() { return configuration; }
    }
    // The generated LocalSftpTestServer invokes these JavaBean setters through reflection.
    @SuppressWarnings("unused")
    public static class Configuration {
        String host, username, password, hosts, directory, key;
        String passphrase = "existing-passphrase";
        String keyExchangeAlgorithm = "existing-algorithm";
        int port;
        long age = 120;
        public void setRemoteHost(String value) { host = value; }
        public void setRemotePort(Integer value) { port = value; }
        public void setUsername(String value) { username = value; }
        public void setPassword(String value) { password = value; }
        public void setKnownHostsFilename(String value) { hosts = value; }
        public void setPrivateKeyFilename(String value) { key = value; }
        public void setPrivateKeyPassphrase(String value) { passphrase = value; }
        public void setPreferredKeyExchangeAlgorithm(String value) { keyExchangeAlgorithm = value; }
        public void setSourceDirectory(String value) { directory = value; }
        public void setOutputDirectory(String value) { directory = value; }
        public void setMinAge(Long value) { age = value; }
    }
    private static String jarPath(Class<?> type) {
        var url = type.getResource("/" + type.getName().replace('.', '/') + ".class");
        String external = Objects.requireNonNull(url).toExternalForm();
        return Path.of(URI.create(external.substring("jar:".length(), external.indexOf("!/")))).toString();
    }

    @Test @Timeout(40) void realSftpUsesTrustedKeyAndSeparateEndpointDirectories() throws Exception {
        String relative = "templates/org/ikasan/studio/generator/localSftpTestServerTemplate_en.ftl";
        String template = Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/" + relative));
        assertEquals(template, Files.readString(Path.of("src/main/resources/studio/metapack/V4.1.6/" + relative)));
        Path source = temporary.resolve("LocalSftpTestServer.java");
        Files.writeString(source, template);
        String cp = String.join(File.pathSeparator, jarPath(org.apache.sshd.server.SshServer.class),
                jarPath(org.apache.sshd.common.Factory.class), jarPath(org.apache.sshd.sftp.server.SftpSubsystemFactory.class));
        var compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-cp", cp, "-d", temporary.toString(), source.toString()).redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes());
        assertEquals(0, compiler.waitFor(), diagnostics);
        int port;
        try (var loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("org.ikasan.studio.flowtests.support.utils.LocalSftpTestServer");
            try (var server = (AutoCloseable) type.getMethod("start", Map.class, Path.class)
                    .invoke(null, Map.of(), Files.createDirectory(temporary.resolve("server")))) {
                var configure = type.getMethod("configure", Object.class, String.class, String.class, boolean.class, Map.class);
                Endpoint consumer = new Endpoint(), producer = new Endpoint();
                configure.invoke(server, consumer, "flow", "input", true, Map.of());
                configure.invoke(server, producer, "flow", "output", false, Map.of());
                assertEquals(0, consumer.configuration.age);
                assertEquals("", consumer.configuration.key);
                for (Endpoint endpoint : List.of(consumer, producer)) {
                    assertEquals("", endpoint.configuration.passphrase);
                    assertNull(endpoint.configuration.keyExchangeAlgorithm);
                }
                Path input = (Path) type.getMethod("directory", String.class, String.class).invoke(server, "flow", "input");
                Path output = (Path) type.getMethod("directory", String.class, String.class).invoke(server, "flow", "output");
                assertNotEquals(input, output);
                var config = consumer.configuration;
                port = config.port;
                try (var client = new RemoteFilesClient()) {
                    client.connect(new RemoteFilesClient.Connection(config.host, config.port, config.username, config.password,
                            "", "", config.hosts), temporary);
                    for (String batch : List.of("first", "second")) {
                        Files.writeString(input.resolve(batch + ".txt"), batch);
                        var entries = client.list(config.directory).entries();
                        var entry = entries.stream().filter(e -> e.name().equals(batch + ".txt")).findFirst().orElseThrow();
                        assertEquals(batch, Files.readString(client.download(config.directory, entry, temporary)));
                    }
                    assertTrue(client.list(producer.configuration.directory).entries().isEmpty());
                }
                Files.writeString(Path.of(config.hosts), "");
                try (var untrusted = new RemoteFilesClient()) {
                    assertThrows(Exception.class, () -> untrusted.connect(new RemoteFilesClient.Connection(config.host,
                            config.port, config.username, config.password, "", "", config.hosts), temporary));
                }
            }
        }
        try (var socket = new Socket()) {
            assertThrows(java.io.IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 1000));
        }
    }
}
