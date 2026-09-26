package org.ikasan.studio.testing.packs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class OutputTextSupportTemplateTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"}) @Timeout(30)
    void convertsContentAndRejectsUnsafeFallbacks(String version) throws Exception {
        Path source = root.resolve("OutputTextSupport.java");
        Files.writeString(source, Files.readString(Path.of("src/main/resources/studio/metapack", version,
                "templates/org/ikasan/studio/generator/outputTextSupportTemplate_en.ftl")));
        String namespace = version.equals("V3.3.9") ? "javax.jms" : "jakarta.jms";
        // Minimal public contracts exercise proxy/interface recognition without requiring optional endpoint jars.
        Path payload = root.resolve("Payload.java");
        Files.writeString(payload, "package org.ikasan.filetransfer; public interface Payload { byte[] getContent(); }");
        Path message = root.resolve("TextMessage.java");
        Files.writeString(message, "package " + namespace + "; public interface TextMessage { String getText(); void acknowledge(); }");
        Path diagnostics = root.resolve("javac.log");
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-encoding", "UTF-8", "-d", root.toString(),
                source.toString(), payload.toString(), message.toString())
                .redirectErrorStream(true).redirectOutput(diagnostics.toFile()).start();
        try {
            assertTrue(compiler.waitFor(20, TimeUnit.SECONDS), "javac did not finish within 20 seconds");
            assertEquals(0, compiler.exitValue(), Files.readString(diagnostics));
        } finally {
            if (compiler.isAlive()) compiler.destroyForcibly().waitFor();
        }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Method stringify = loader.loadClass("org.ikasan.studio.flowtests.OutputTextSupport").getMethod("stringify", Object.class);
            assertEquals("hello", stringify.invoke(null, "hello"));
            assertEquals("null", stringify.invoke(null, new Object[]{null}));
            byte[] bytes = "注文\n".getBytes(StandardCharsets.UTF_8);
            assertEquals("注文\n", stringify.invoke(null, bytes));
            Path file = Files.write(root.resolve("payload.txt"), bytes);
            assertEquals("注文\n", stringify.invoke(null, file));
            assertEquals("注文\n", stringify.invoke(null, file.toFile()));
            Path secondFile = Files.writeString(root.resolve("second.txt"), "second\n");
            assertEquals("second\n注文\n", stringify.invoke(null, List.of(secondFile.toFile(), file.toFile())));
            Class<?> contract = loader.loadClass("org.ikasan.filetransfer.Payload");
            Object wrapped = Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (p, m, a) -> bytes);
            assertEquals("注文\n", stringify.invoke(null, wrapped));
            Class<?> text = loader.loadClass(namespace + ".TextMessage");
            List<String> calls = new ArrayList<>();
            Object jms = Proxy.newProxyInstance(loader, new Class<?>[]{text}, (p, m, a) -> {
                calls.add(m.getName());
                if (!m.getName().equals("getText")) throw new AssertionError("Must not mutate a JMS message");
                return "message body";
            });
            assertEquals("message body", stringify.invoke(null, jms));
            assertEquals(List.of("getText"), calls);
            fails(stringify, new Object());
            fails(stringify, new byte[]{(byte) 0xc3, (byte) 0x28});
            fails(stringify, root.resolve("missing"));
            fails(stringify, List.of("not a file"));
            assertArrayEquals(bytes, Files.readAllBytes(file));
            assertEquals("second\n", Files.readString(secondFile));
        }
    }

    private static void fails(Method stringify, Object value) {
        InvocationTargetException failure = assertThrows(InvocationTargetException.class, () -> stringify.invoke(null, value));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }
}
