package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileDeliveryAssertionsTemplateTest {
    @TempDir Path root;

    @Test @Timeout(30) void generatedHelperChecksActualFilesWithoutChangingThem() throws Exception {
        Path source = root.resolve("FileDeliveryAssertions.java");
        String template = Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/templates/org/ikasan/studio/generator/fileDeliveryAssertionsTemplate_en.ftl"));
        assertEquals(template, Files.readString(Path.of("src/main/resources/studio/metapack/V4.1.6/templates/org/ikasan/studio/generator/fileDeliveryAssertionsTemplate_en.ftl")));
        Files.writeString(source, template);
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), source.toString()).redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, compiler.waitFor(), diagnostics);
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("org.ikasan.studio.flowtests.FileDeliveryAssertions");
            Method file = type.getMethod("assertFileContents", Path.class, String.class, Duration.class);
            Method files = type.getMethod("assertDeliveredFileContents", Path.class, String.class, List.class, Duration.class);
            Path output = Files.createDirectory(root.resolve("output"));
            Path first = output.resolve("first.dat");
            var writer = java.util.concurrent.Executors.newSingleThreadExecutor();
            try {
                var written = writer.submit(() -> {
                    Thread.sleep(100);
                    return Files.writeString(first, "first\n");
                });
                file.invoke(null, first, "first\n", Duration.ofSeconds(2));
                written.get();
            } finally { writer.shutdownNow(); }
            files.invoke(null, output, "*.dat", List.of("first\n"), Duration.ofSeconds(1));
            Files.writeString(output.resolve("second.dat"), "second\n");
            files.invoke(null, output, "*.dat", List.of("second\n", "first\n"), Duration.ofSeconds(1));
            Files.writeString(output.resolve("pending.tmp"), "not final");
            files.invoke(null, output, "*.dat", List.of("first\n", "second\n"), Duration.ofSeconds(1));
            fails(files, output, "*", List.of("first\n", "second\n"));
            fails(files, output, "*.dat", List.of("first\n", "wrong"));
            fails(files, output, "*.dat", List.of("first\n", "first\n"));
            fails(file, first, "wrong");
            fails(file, output.resolve("missing.dat"), "missing");
            fails(files, output.resolve("missing"), "*.dat", List.of());
            assertEquals("first\n", Files.readString(first));
            assertTrue(Files.exists(output.resolve("pending.tmp")));
        }
    }

    private static void fails(Method method, Object... arguments) {
        Object[] timed = java.util.Arrays.copyOf(arguments, arguments.length + 1);
        timed[arguments.length] = Duration.ofMillis(75);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class, () -> method.invoke(null, timed));
        assertInstanceOf(AssertionError.class, failure.getCause());
    }
}
