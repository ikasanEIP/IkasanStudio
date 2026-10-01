package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Compiles the shipped helper and exercises real FTP, without launching an IntelliJ project. */
class LocalFtpFlowTestFixtureTest {
    public static class Consumer {
        private final Configuration config = new Configuration();
        public Configuration getConfiguration() { return config; }
    }
    public static class Configuration {
        public Long minAge = 120L;
        // Invoked reflectively by the compiled LocalFtpTestServer template's configureConsumer method.
        @SuppressWarnings("unused")
        public void setMinAge(Long value) { minAge = value; }
    }

    @TempDir Path temporary;

    private static String jarPath(Class<?> type) {
        URL resource = type.getResource("/" + type.getName().replace('.', '/') + ".class");
        assertNotNull(resource, "Class resource not found for " + type.getName());
        assertEquals("jar", resource.getProtocol(), "Expected a JAR resource for " + type.getName());
        String url = resource.toExternalForm();
        return Path.of(URI.create(url.substring("jar:".length(), url.indexOf("!/")))).toString();
    }

    @Test @org.junit.jupiter.api.Timeout(30) void realFtpDeliveryIsolationAndCleanup() throws Throwable {
        Path source = temporary.resolve("LocalFtpTestServer.java");
        String template = Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/templates/org/ikasan/studio/generator/localFtpTestServerTemplate_en.ftl"));
        assertEquals(template, Files.readString(Path.of("src/main/resources/studio/metapack/V4.1.6/templates/org/ikasan/studio/generator/localFtpTestServerTemplate_en.ftl")));
        Files.writeString(source, template);
        String cp = String.join(File.pathSeparator, List.of(
                jarPath(org.apache.ftpserver.FtpServer.class),
                jarPath(org.apache.ftpserver.ftplet.User.class)));
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-classpath", cp, "-d", temporary.toString(), source.toString())
                .redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, compiler.waitFor(), diagnostics);
        try (var loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("org.ikasan.studio.flowtests.support.utils.LocalFtpTestServer");
            for (boolean failScenario : List.of(false, true)) {
                var folder = org.junit.rules.TemporaryFolder.builder().assureDeletion().build();
                Path[] testRoot = new Path[1];
                var statement = new org.junit.runners.model.Statement() {
                    @Override public void evaluate() throws Throwable {
                        testRoot[0] = folder.getRoot().toPath();
                        Path home;
                        int port;
            try (AutoCloseable server = (AutoCloseable) type.getMethod("start", Map.class, Path.class).invoke(null, Map.of(), folder.newFolder().toPath());
                 AutoCloseable second = (AutoCloseable) type.getMethod("start", Map.class, Path.class).invoke(null, Map.of(), folder.newFolder().toPath())) {
                Consumer consumer = new Consumer();
                var configureConsumer = type.getMethod("configureConsumer", Object.class, Map.class);
                configureConsumer.invoke(server, consumer, Map.of());
                assertEquals(0L, consumer.config.minAge);
                configureConsumer.invoke(server, consumer, Map.of("test.ftp.consumer.min-age-seconds", "5"));
                assertEquals(5L, consumer.config.minAge);
                assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> configureConsumer.invoke(server, consumer, Map.of("test.ftp.consumer.min-age-seconds", "-1")));
                assertEquals(5L, consumer.config.minAge);
                home = (Path) type.getMethod("root").invoke(server);
                port = (Integer) type.getMethod("port").invoke(server);
                assertNotEquals(port, type.getMethod("port").invoke(second));
                assertNotEquals(home, type.getMethod("root").invoke(second));
                Map<String, String> properties = new HashMap<>();
                type.getMethod("configure", Map.class, String.class, boolean.class, String.class, String.class, String.class, String.class, String.class)
                        .invoke(server, properties, "test producer", false, "host", "port", "user", "password", "directory");
                assertEquals("127.0.0.1", properties.get("host"));
                assertEquals(Integer.toString(port), properties.get("port"));
                URL url = new URI("ftp://ikasan:ikasan@127.0.0.1:" + port + "/sample.txt;type=i").toURL();
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) { out.write("first batch".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                assertEquals("first batch", Files.readString(home.resolve("sample.txt")));
                URLConnection download = url.openConnection();
                download.setConnectTimeout(5000); download.setReadTimeout(5000);
                try (InputStream in = download.getInputStream()) {
                    assertEquals("first batch", new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            // Stopping the server does not delete JUnit-owned files prematurely.
            assertTrue(Files.exists(home.resolve("sample.txt")));
            try (Socket socket = new Socket()) {
                assertThrows(IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 1000));
            }
                        if (failScenario) throw new AssertionError("deliberate scenario failure");
                    }
                };
                var execution = folder.apply(statement, org.junit.runner.Description.EMPTY);
                if (failScenario) {
                    var failure = assertThrows(AssertionError.class, execution::evaluate);
                    assertEquals("deliberate scenario failure", failure.getMessage());
                } else execution.evaluate();
                assertFalse(Files.exists(testRoot[0]), "JUnit must remove both FTP homes after success or failure");
            }
        }
    }
}
