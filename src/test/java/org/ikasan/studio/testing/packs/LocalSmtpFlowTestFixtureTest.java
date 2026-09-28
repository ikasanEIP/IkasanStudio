package org.ikasan.studio.testing.packs;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.time.Duration;
import java.util.List;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class LocalSmtpFlowTestFixtureTest {
    @TempDir Path root;

    @Test @Timeout(30) void deliversTwoBatchesIsolatesInboxesAndClosesPort() throws Exception {
        Path source = root.resolve("LocalSmtpTestServer.java");
        Files.writeString(source, Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/templates/org/ikasan/studio/generator/localSmtpTestServerTemplate_en.ftl")));
        String classpath = String.join(File.pathSeparator, List.of(
                location(com.icegreen.greenmail.util.GreenMail.class), location(javax.mail.Part.class), location(org.junit.Assert.class)));
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-cp", classpath, "-d", root.toString(), source.toString()).redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes());
        assertEquals(0, compiler.waitFor(), diagnostics);
        int port;
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("org.ikasan.studio.flowtests.support.LocalSmtpTestServer");
            Object server = type.getMethod("start").invoke(null);
            Object second = type.getMethod("start").invoke(null);
            port = (int) type.getMethod("port").invoke(server);
            try {
                var setup = new ServerSetup(port, "127.0.0.1", "smtp");
                GreenMailUtil.sendTextEmail("to@example.test", "from@example.test", "first", "first body", setup);
                type.getMethod("assertBody", int.class, String.class, Duration.class)
                        .invoke(server, 1, "first body", Duration.ofSeconds(2));
                GreenMailUtil.sendTextEmail("to@example.test", "from@example.test", "second", "later body", setup);
                type.getMethod("assertBody", int.class, String.class, Duration.class)
                        .invoke(server, 2, "later body", Duration.ofSeconds(2));
                assertEquals(0, ((Object[]) type.getMethod("receivedMessages").invoke(second)).length);
                InvocationTargetException failure = assertThrows(InvocationTargetException.class, () ->
                        type.getMethod("assertBody", int.class, String.class, Duration.class)
                                .invoke(server, 3, "missing", Duration.ofMillis(50)));
                assertInstanceOf(AssertionError.class, failure.getCause());
                assertTrue(failure.getCause().getMessage().contains("No SMTP delivery 3"));
            } finally {
                ((AutoCloseable) server).close();
                ((AutoCloseable) second).close();
                ((AutoCloseable) server).close();
            }
        }
        try (var socket = new Socket()) {
            assertThrows(java.io.IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 200));
        }
    }
    private static String location(Class<?> type) {
        URL resource = type.getResource("/" + type.getName().replace('.', '/') + ".class");
        assertNotNull(resource, "Class resource not found for " + type.getName());
        assertEquals("jar", resource.getProtocol(), "Expected a JAR resource for " + type.getName());
        String url = resource.toExternalForm();
        return Path.of(URI.create(url.substring("jar:".length(), url.indexOf("!/")))).toString();
    }
}
