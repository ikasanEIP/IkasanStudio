package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class FtpInputFixtureTemplateTest {
    @TempDir Path root;

    @Test void createsMatchingCompleteBatchFilesAndRejectsUnsafeOrStaleInputs() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/templates/org/ikasan/studio/generator/ftpInputFixtureTemplate_en.ftl"));
        assertEquals(template, Files.readString(Path.of("src/main/resources/studio/metapack/V4.1.6/templates/org/ikasan/studio/generator/ftpInputFixtureTemplate_en.ftl")));
        Path shared = root.resolve("FileInputFixture.java");
        String sharedTemplate = Files.readString(Path.of("src/main/resources/studio/metapack/V3.3.9/templates/org/ikasan/studio/generator/fileInputFixtureTemplate_en.ftl"));
        assertEquals(sharedTemplate, Files.readString(Path.of("src/main/resources/studio/metapack/V4.1.6/templates/org/ikasan/studio/generator/fileInputFixtureTemplate_en.ftl")));
        Files.writeString(shared, sharedTemplate);
        Path source = root.resolve("FtpInputFixture.java");
        Files.writeString(source, template);
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), source.toString(), shared.toString()).redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, compiler.waitFor(), diagnostics);
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, getClass().getClassLoader())) {
            var write = loader.loadClass("org.ikasan.studio.flowtests.support.utils.FtpInputFixture")
                    .getMethod("write", Path.class, String.class, String.class, int.class, String.class);
            Path home = Files.createDirectory(root.resolve("ftp"));
            Path first = (Path) write.invoke(null, home, ".*\\.txt", null, 1, "first café");
            Path second = (Path) write.invoke(null, home, ".*\\.txt", null, 2, "second");
            assertNotEquals(first, second);
            assertEquals("first café", Files.readString(first));
            assertEquals("second", Files.readString(second));
            assertThrows(InvocationTargetException.class, () -> write.invoke(null, home, ".*\\.txt", null, 1, "overwrite"));
            assertEquals("first café", Files.readString(first));
            var unsupported = assertThrows(InvocationTargetException.class,
                    () -> write.invoke(null, home, "orders-[0-9]+\\.csv", null, 1, "input"));
            assertTrue(unsupported.getCause().getMessage().contains("test.ftp.input.filename.batch1"));
            Path custom = (Path) write.invoke(null, home, "orders-[0-9]+\\.csv", "orders-1.csv", 1, "custom");
            assertEquals("custom", Files.readString(custom));
            assertThrows(InvocationTargetException.class, () -> write.invoke(null, home, ".*", "../escape", 1, "bad"));
            assertThrows(InvocationTargetException.class, () -> write.invoke(null, home, ".*\\.txt", "bad.csv", 1, "bad"));
            var copy = loader.loadClass("org.ikasan.studio.flowtests.support.utils.FtpInputFixture")
                    .getMethod("copyResource", Path.class, String.class, String.class);
            Path resources = Files.createDirectories(root.resolve("input"));
            byte[] original = new byte[]{0, 1, (byte) 255, 13, 10, 42};
            Files.write(resources.resolve("orders-2.csv"), original);
            Path copied = (Path) copy.invoke(null, home, "orders-[0-9]+\\.csv", "/input/orders-2.csv");
            assertEquals(home.resolve("orders-2.csv"), copied);
            assertArrayEquals(original, Files.readAllBytes(copied));
            assertThrows(InvocationTargetException.class,
                    () -> copy.invoke(null, home, ".*", "input/orders-2.csv"));
            var missing = assertThrows(InvocationTargetException.class,
                    () -> copy.invoke(null, home, ".*", "input/missing.csv"));
            assertTrue(missing.getCause().getMessage().contains("Missing test resource"));
            var mismatch = assertThrows(InvocationTargetException.class,
                    () -> copy.invoke(null, home, "only.txt", "input/orders-2.csv"));
            assertTrue(mismatch.getCause().getMessage().contains("consumer pattern"));
            try (var paths = Files.list(root)) {
                assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith("file-input-")));
            }
            try (var files = Files.list(home)) { assertEquals(4, files.count()); }
            Class<?> sharedType = loader.loadClass("org.ikasan.studio.flowtests.support.utils.FileInputFixture");
            var pattern = sharedType.getMethod("localFilenames", Path.class, String[].class);
            var prepare = sharedType.getMethod("prepareLocalBatch", Path.class, int.class, String[].class);
            Path localHome = Files.createDirectory(root.resolve("local"));
            Files.writeString(resources.resolve("first[1].xml"), "first XML");
            Files.writeString(resources.resolve("second.xml"), "second XML");
            String[] paths = {"/input/first[1].xml", "/input/second.xml"};
            String expression = (String) pattern.invoke(null, localHome, paths);
            String filenameRegex = expression.substring(expression.lastIndexOf('/') + 1);
            assertTrue(java.util.regex.Pattern.matches(filenameRegex, "first[1].xml"));
            assertFalse(java.util.regex.Pattern.matches(filenameRegex, "first1.xml"));
            Files.writeString(localHome.resolve("keep.txt"), "unrelated");
            prepare.invoke(null, localHome, 1, paths);
            assertEquals("first XML", Files.readString(localHome.resolve("first[1].xml")));
            prepare.invoke(null, localHome, 2, paths);
            assertFalse(Files.exists(localHome.resolve("first[1].xml")));
            assertEquals("second XML", Files.readString(localHome.resolve("second.xml")));
            assertTrue(Files.exists(localHome.resolve("keep.txt")));
            assertThrows(InvocationTargetException.class, () -> prepare.invoke(null, localHome, 3, paths));
            assertThrows(InvocationTargetException.class, () -> pattern.invoke(null, localHome,
                    new String[]{"/one/same.txt", "/two/same.txt"}));
        }
    }
}
