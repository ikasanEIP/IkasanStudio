package org.ikasan.studio.testkit;

import org.ikasan.studio.core.BuildContext;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.loading.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class ExternalMetaPackTest {
    @TempDir Path temporary;

    @Test
    void validatesInstallsAndDiscoversPackOnlyInNextSession() throws Exception {
        Path source = customPack("AcmePack", false);
        Path installed = temporary.resolve("installed");
        assertNull(MetaPackResources.resource("studio/metapack/AcmePack/metapack.json"));
        var manifest = MetaPackInstaller.install(source, installed, () -> { });
        assertEquals("AcmePack", manifest.id());
        assertTrue(Files.exists(installed.resolve("AcmePack.jar")));
        assertFalse(ComponentLibrary.getMetapackList().contains("AcmePack"));
        assertThrows(IOException.class, () -> MetaPackInstaller.install(source, installed, () -> { }));
        inFreshSession(installed, "AcmePack", false);
    }

    @Test
    void rejectsInvalidTemplatesAndConflictingBundledIdsWithoutPublishingFiles() throws Exception {
        Path installed = temporary.resolve("installed");
        assertThrows(Exception.class, () -> MetaPackInstaller.install(customPack("BrokenPack", true), installed, () -> { }));
        assertThrows(IOException.class, () -> MetaPackInstaller.install(customPack("V4.1.6", false), installed, () -> { }));
        try (var files = Files.list(installed)) { assertEquals(0, files.count()); }
    }

    @Test
    void cancellationLeavesNoPendingInstallation() throws Exception {
        Path source = customPack("CancelledPack", false);
        Path installed = temporary.resolve("installed");
        assertThrows(IllegalStateException.class, () -> MetaPackInstaller.install(source, installed,
                () -> { throw new IllegalStateException("cancelled"); }));
        try (var files = Files.list(installed)) { assertEquals(0, files.count()); }
    }

    @Test
    void duplicateInstalledIdsAreRejectedInsteadOfDependingOnFileOrder() throws Exception {
        Path installed = Files.createDirectory(temporary.resolve("duplicates"));
        Path source = customPack("DuplicatePack", false);
        Files.copy(source, installed.resolve("first.jar"));
        Files.copy(source, installed.resolve("second.jar"));
        inFreshSession(installed, "DuplicatePack", true);
    }

    @Test
    void inspectionCannotLeakResourcesToOtherThreadsOrAfterFailure() throws Exception {
        var archive = MetaPackArchive.open(customPack("ScopedPack", false));
        assertThrows(IOException.class, () -> MetaPackResources.inspect(archive, () -> {
            assertNotNull(MetaPackResources.resource("studio/metapack/ScopedPack/metapack.json"));
            assertNull(java.util.concurrent.CompletableFuture.supplyAsync(() ->
                    MetaPackResources.resource("studio/metapack/ScopedPack/metapack.json")).get());
            throw new IOException("inspection failure");
        }));
        assertNull(MetaPackResources.resource("studio/metapack/ScopedPack/metapack.json"));
    }

    private Path customPack(String id, boolean broken) throws Exception {
        Path output = temporary.resolve(id + "-source.jar");
        URL resource = getClass().getClassLoader().getResource("studio/metapack/V4.1.6/metapack.json");
        var connection = (JarURLConnection) Objects.requireNonNull(resource).openConnection();
        connection.setUseCaches(false);
        try (var input = connection.getJarFile(); var jar = new JarOutputStream(Files.newOutputStream(output))) {
            for (var entry : Collections.list(input.entries())) {
                if (entry.isDirectory() || !entry.getName().startsWith("studio/metapack/V4.1.6/")) continue;
                String name = entry.getName().replace("V4.1.6", id);
                jar.putNextEntry(new JarEntry(name)); // Deliberately omit all directory entries.
                byte[] bytes;
                try (var stream = input.getInputStream(entry)) { bytes = stream.readAllBytes(); }
                if (name.endsWith(".json")) bytes = new String(bytes, StandardCharsets.UTF_8)
                        .replace("V4.1.6", id).getBytes(StandardCharsets.UTF_8);
                if (broken && name.endsWith("applicationTemplate_en.ftl")) bytes = "<#if".getBytes(StandardCharsets.UTF_8);
                jar.write(bytes);
                jar.closeEntry();
            }
        }
        return output;
    }

    private void inFreshSession(Path installed, String id, boolean rejected) throws Exception {
        Path configuration = Files.createTempDirectory(temporary, "session");
        Path services = Files.createDirectories(configuration.resolve("META-INF/services"));
        Files.writeString(services.resolve(MetaPackDirectoryProvider.class.getName()), DirectoryProvider.class.getName());
        Files.writeString(configuration.resolve("installed-packs-location.txt"), installed.toString());
        var urls = new ArrayList<URL>();
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            urls.add(Path.of(entry).toUri().toURL());
        }
        urls.add(configuration.toUri().toURL());
        try (var session = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            try {
                Class.forName(RestartProbe.class.getName(), true, session).getMethod("verify", String.class, boolean.class)
                        .invoke(null, id, rejected);
            } catch (java.lang.reflect.InvocationTargetException e) { throw new AssertionError(e.getCause()); }
        }
    }

    public static class DirectoryProvider implements MetaPackDirectoryProvider {
        @Override public Path directory() {
            try (var input = getClass().getResourceAsStream("/installed-packs-location.txt")) {
                return Path.of(new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }

    public static class RestartProbe {
        @SuppressWarnings("unused") // Invoked reflectively in an isolated session classloader above.
        public static void verify(String id, boolean rejected) throws Exception {
            if (rejected) {
                assertFalse(ComponentLibrary.getMetapackList().contains(id));
                assertFalse(MetaPackResources.installationProblems().isEmpty());
                return;
            }
            assertTrue(ComponentLibrary.getMetapackList().contains(id));
            assertEquals("4.1.6", ComponentLibrary.getMetaPackManifest(id).ikasanVersion());
            assertFalse(ComponentLibrary.getIkasanComponents(id).isEmpty());
            var module = Module.moduleBuilder().version(id).name("Example")
                    .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                    .h2WebPortNumber("8093").flows(List.of()).build();
            assertTrue(MetaPackTestKit.render(module).get("Application.java").contains("class Application"));
            var icon = ComponentLibrary.getPaletteComponentList(id).iterator().next().getIconResourceDirectory() + "/small.png";
            assertNotNull(MetaPackResources.resource(icon));
            try (var stream = MetaPackResources.open(icon)) {
                assertNotNull(stream);
                assertTrue(stream.read() >= 0);
            }
            assertThrows(ClassNotFoundException.class, () -> Class.forName("com.intellij.openapi.application.ApplicationManager"));
            assertNotNull(BuildContext.getFreemarkerConfig(id).getTemplate("flowTemplate_en.ftl"));
        }
    }
}
