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
        try (var files = Files.list(installed)) { assertEquals(List.of(".management.lock"), files.map(p -> p.getFileName().toString()).toList()); }
    }

    @Test
    void cancellationLeavesNoPendingInstallation() throws Exception {
        Path source = customPack("CancelledPack", false);
        Path installed = temporary.resolve("installed");
        assertThrows(IllegalStateException.class, () -> MetaPackInstaller.install(source, installed,
                () -> { throw new IllegalStateException("cancelled"); }));
        try (var files = Files.list(installed)) { assertEquals(List.of(".management.lock"), files.map(p -> p.getFileName().toString()).toList()); }
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

    @Test
    void updatesActivateOnRestartRetainPreviousJarAndCanRollBack() throws Exception {
        Path directory = temporary.resolve("managed");
        Path original = customPack("ManagedPack", false);
        MetaPackInstaller.install(original, directory, () -> { });
        Path current = directory.resolve("ManagedPack.jar");
        String lockedModel = (String) freshCall(directory, "model", new Class<?>[]{String.class}, "ManagedPack");
        byte[] before = Files.readAllBytes(current);
        Path update = customPack("ManagedPack", false, "0.2.0");
        var reviewed = MetaPackStore.review(update);
        var entry = MetaPackStore.list(directory).get(0);
        MetaPackStore.stageUpdate(update, current, directory, () -> { }, reviewed, entry.sha256());
        assertArrayEquals(before, Files.readAllBytes(current), "Scheduling must preserve the current session's archive");
        assertEquals("update", MetaPackStore.list(directory).get(0).pending());
        assertThrows(IOException.class, () -> MetaPackStore.stageUpdate(update, current, directory, () -> { }));
        assertEquals("0.2.0", freshCall(directory, "revision", new Class<?>[]{String.class}, "ManagedPack"));
        assertEquals("0.2.0", MetaPackStore.manifest(current).packVersion());
        assertTrue((Boolean) freshCall(directory, "rejectsLockedModel", new Class<?>[]{String.class}, lockedModel));
        var history = MetaPackStore.history(directory, "ManagedPack");
        assertEquals(1, history.size());
        assertArrayEquals(before, Files.readAllBytes(history.get(0)));
        MetaPackStore.stageUpdate(history.get(0), current, directory, () -> { });
        assertEquals("0.1.0-SNAPSHOT", freshCall(directory, "revision", new Class<?>[]{String.class}, "ManagedPack"));
        assertFalse((Boolean) freshCall(directory, "rejectsLockedModel", new Class<?>[]{String.class}, lockedModel));
    }

    @Test
    void removalIsCancellableAndRetainedBackupCanBeInstalledAgain() throws Exception {
        Path directory = temporary.resolve("removal");
        MetaPackInstaller.install(customPack("RemovablePack", false), directory, () -> { });
        Path current = directory.resolve("RemovablePack.jar");
        MetaPackStore.stageRemoval(current, directory);
        assertTrue(Files.exists(current));
        MetaPackStore.cancel(directory, "RemovablePack");
        assertNull(MetaPackStore.list(directory).get(0).pending());
        MetaPackStore.stageRemoval(current, directory);
        freshCall(directory, "activate", new Class<?>[0]);
        assertFalse(Files.exists(current));
        var entry = MetaPackStore.list(directory).get(0);
        assertEquals("removed", entry.pending());
        var backup = MetaPackStore.history(directory, "RemovablePack").get(0);
        MetaPackInstaller.install(backup, directory, () -> { });
        inFreshSession(directory, "RemovablePack", false);
    }

    @Test
    void changedPendingArtifactsAndStaleReviewsLeaveInstalledPackUntouched() throws Exception {
        Path directory = temporary.resolve("tampering");
        MetaPackInstaller.install(customPack("IntegrityPack", false), directory, () -> { });
        Path current = directory.resolve("IntegrityPack.jar");
        Path update = customPack("IntegrityPack", false, "0.2.0");
        var review = MetaPackStore.review(update);
        Files.writeString(update, "changed", StandardOpenOption.APPEND);
        assertThrows(IOException.class, () -> MetaPackStore.stageUpdate(update, current, directory, () -> { }, review, null));
        Path replacement = customPack("IntegrityPack", false, "0.2.0");
        MetaPackStore.stageUpdate(replacement, current, directory, () -> { });
        byte[] original = Files.readAllBytes(current);
        Files.writeString(directory.resolve(".pending/IntegrityPack.jar"), "changed", StandardOpenOption.APPEND);
        var problems = (Map<?, ?>) freshCall(directory, "activate", new Class<?>[0]);
        assertFalse(problems.isEmpty());
        assertArrayEquals(original, Files.readAllBytes(current));
        assertTrue(Files.exists(directory.resolve(".pending/IntegrityPack.json")));
        MetaPackStore.cancel(directory, "IntegrityPack");
        assertThrows(IOException.class, () -> MetaPackStore.stageRemoval(current, directory, "stale hash"));
    }

    @Test
    void updatesRequireNewRevisionAndSameRuntimeAndCancellationCleansStaging() throws Exception {
        Path directory = temporary.resolve("contracts");
        Path initial = customPack("ContractPack", false);
        MetaPackInstaller.install(initial, directory, () -> { });
        Path current = directory.resolve("ContractPack.jar");
        assertThrows(IOException.class, () -> MetaPackStore.stageUpdate(initial, current, directory, () -> { }));
        Path update = customPack("ContractPack", false, "0.2.0");
        assertThrows(IllegalStateException.class, () -> MetaPackStore.stageUpdate(update, current, directory,
                () -> { throw new IllegalStateException("cancelled"); }));
        assertNull(MetaPackStore.list(directory).get(0).pending());
        try (var files = Files.list(directory)) {
            assertEquals(2, files.count(), "Only the current JAR and management lock remain");
        }
    }

    @Test
    void previouslyPublishedRevisionCannotBeReplacedWithDifferentBytes() throws Exception {
        Path directory = temporary.resolve("immutable");
        Path original = customPack("ImmutablePack", false);
        MetaPackInstaller.install(original, directory, () -> { });
        Path current = directory.resolve("ImmutablePack.jar");
        MetaPackStore.stageUpdate(customPack("ImmutablePack", false, "0.2.0"), current, directory, () -> { });
        freshCall(directory, "activate", new Class<?>[0]);
        Files.writeString(original, "changed", StandardOpenOption.APPEND);
        assertThrows(IOException.class, () -> MetaPackStore.stageUpdate(original, current, directory, () -> { }));
        MetaPackStore.stageRemoval(current, directory);
        freshCall(directory, "activate", new Class<?>[0]);
        assertThrows(IOException.class, () -> MetaPackInstaller.install(original, directory, () -> { }));
    }

    @Test
    void malformedPendingOperationCanBeCancelledWithoutTouchingTheCurrentJar() throws Exception {
        Path directory = temporary.resolve("invalid-plan");
        MetaPackInstaller.install(customPack("RecoverablePack", false), directory, () -> { });
        Path current = directory.resolve("RecoverablePack.jar");
        byte[] original = Files.readAllBytes(current);
        MetaPackStore.stageUpdate(customPack("RecoverablePack", false, "0.2.0"), current, directory, () -> { });
        Files.writeString(directory.resolve(".pending/RecoverablePack.json"), "{}");
        assertFalse(((Map<?, ?>) freshCall(directory, "activate", new Class<?>[0])).isEmpty());
        var entry = MetaPackStore.list(directory).get(0);
        assertEquals("RecoverablePack", entry.id());
        assertEquals("invalid", entry.pending());
        MetaPackStore.cancel(directory, entry.id());
        assertNull(MetaPackStore.list(directory).get(0).pending());
        assertArrayEquals(original, Files.readAllBytes(current));
    }

    @Test
    void interruptedActivationCleanupIsIdempotentAndOtherManagersCannotMutateTheStore() throws Exception {
        Path directory = temporary.resolve("interrupted");
        Path original = customPack("InterruptedPack", false);
        MetaPackInstaller.install(original, directory, () -> { });
        Path current = directory.resolve("InterruptedPack.jar");
        try (var channel = java.nio.channels.FileChannel.open(directory.resolve(".management.lock"), StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertThrows(IOException.class, () -> MetaPackStore.stageRemoval(current, directory));
        }
        MetaPackStore.stageUpdate(customPack("InterruptedPack", false, "0.2.0"), current, directory, () -> { });
        Path pending = directory.resolve(".pending/InterruptedPack.json");
        var plan = new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(pending));
        Path history = directory.resolve(".history/InterruptedPack");
        Files.createDirectories(history);
        Files.copy(current, history.resolve(plan.path("expectedSha256").asText() + ".jar"));
        Files.move(directory.resolve(".pending/InterruptedPack.jar"), current, StandardCopyOption.REPLACE_EXISTING);
        // Model a crash after replacement but before deleting the operation record.
        assertTrue(((Map<?, ?>) freshCall(directory, "activate", new Class<?>[0])).isEmpty());
        assertFalse(Files.exists(pending));
        assertEquals("0.2.0", MetaPackStore.manifest(current).packVersion());
        assertTrue(((Map<?, ?>) freshCall(directory, "activate", new Class<?>[0])).isEmpty());
        assertEquals(1, MetaPackStore.history(directory, "InterruptedPack").size());
    }

    private Path customPack(String id, boolean broken) throws Exception {
        return customPack(id, broken, "0.1.0-SNAPSHOT");
    }

    private Path customPack(String id, boolean broken, String revision) throws Exception {
        Path output = temporary.resolve(id + "-" + revision + "-source.jar");
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
                        .replace("V4.1.6", id).replace("0.1.0-SNAPSHOT", revision).getBytes(StandardCharsets.UTF_8);
                if (broken && name.endsWith("applicationTemplate_en.ftl")) bytes = "<#if".getBytes(StandardCharsets.UTF_8);
                jar.write(bytes);
                jar.closeEntry();
            }
        }
        return output;
    }

    private void inFreshSession(Path installed, String id, boolean rejected) throws Exception {
        freshCall(installed, "verify", new Class<?>[]{String.class, boolean.class}, id, rejected);
    }

    private Object freshCall(Path installed, String method, Class<?>[] parameters, Object... args) throws Exception {
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
                return Class.forName(RestartProbe.class.getName(), true, session).getMethod(method, parameters).invoke(null, args);
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
        @SuppressWarnings("unused")
        public static Map<String, String> activate() { return MetaPackResources.installationProblems(); }
        @SuppressWarnings("unused")
        public static String revision(String id) throws Exception { return ComponentLibrary.getMetaPackManifest(id).packVersion(); }
        @SuppressWarnings("unused")
        public static String model(String id) throws Exception {
            var module = Module.moduleBuilder().version(id).name("Example").applicationPackageName("org.example")
                    .port("8080").h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of()).build();
            return MetaPackTestKit.render(module).get("model.json");
        }
        @SuppressWarnings("unused")
        public static boolean rejectsLockedModel(String source) {
            try { MetaPackTestKit.readModel(source, "revision lock test"); return false; }
            catch (org.ikasan.studio.core.StudioBuildException failure) {
                assertTrue(failure.getMessage().contains("locked to revision")); return true;
            }
        }
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
