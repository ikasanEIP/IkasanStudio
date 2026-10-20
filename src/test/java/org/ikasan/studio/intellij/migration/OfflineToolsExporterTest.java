package org.ikasan.studio.intellij.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class OfflineToolsExporterTest {
    @TempDir Path parent;
    @Test void extractsBundledToolsAndPreservesExecutableLauncher() throws Exception {
        try(var archive=getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            assertNotNull(archive);
            Path exported=OfflineToolsExporter.export(archive,parent,()->{});
            assertTrue(Files.isRegularFile(exported.resolve("README.md")));
            assertTrue(Files.isRegularFile(exported.resolve("bin/studio_upgrade.py")));
            assertTrue(Files.isRegularFile(exported.resolve("bin/studio-cli.bat")));
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
                assertTrue(Files.isExecutable(exported.resolve("bin/studio-cli")));
            String javaExecutable=Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
            Path output=parent.resolve("cli-help.txt");
            Process cli=new ProcessBuilder(javaExecutable, "-cp", exported.resolve("lib/*").toString(),
                    "org.ikasan.studio.cli.StudioMigrationCli", "--help")
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try {
                assertTrue(cli.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "Standalone CLI timed out");
                assertEquals(0,cli.exitValue(),Files.readString(output));
                assertTrue(Files.readString(output).contains("preview --project"));
            } finally { if(cli.isAlive())cli.destroyForcibly(); }
            try(var libs=Files.list(exported.resolve("lib"))) {
                assertTrue(libs.anyMatch(p->p.getFileName().toString().startsWith("studio-generator-")));
            }
        }
    }
    @Test void refusesExistingDirectoryWithoutChangingItsFiles() throws Exception {
        Path existing=Files.createDirectory(parent.resolve(OfflineToolsExporter.DIRECTORY));
        Files.writeString(existing.resolve("keep.txt"),"keep");
        assertThrows(FileAlreadyExistsException.class,()->OfflineToolsExporter.export(new ByteArrayInputStream(new byte[0]),parent,()->{}));
        assertEquals("keep",Files.readString(existing.resolve("keep.txt")));
    }
    @Test void rejectsTraversalAndCleansPartialExtraction() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok", "studio-cli-test/../../outside")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        try(var files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void cancellationCleansStagingAndDoesNotPublishDestination() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IllegalStateException.class,()->OfflineToolsExporter.export(archive,parent,()->{throw new IllegalStateException("cancelled");}));
        }
        try(var files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void rejectsIncompleteArchive() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        assertFalse(Files.exists(parent.resolve(OfflineToolsExporter.DIRECTORY)));
    }
    @Test void customPackExportRunsAfterRelocationAndMatchesStudioRendering() throws Exception {
        var archive = org.ikasan.studio.core.metapack.loading.MetaPackArchive.open(customPack());
        org.ikasan.studio.core.metapack.loading.MetaPackResources.inspect(archive, () -> {
            String id = archive.id();
            var flow = org.ikasan.studio.core.TestFixtures.getUnbuiltFlow(id)
                    .consumer(org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumer(id)).build();
            var producer = org.ikasan.studio.core.TestFixtures.getDevNullProducer(id);
            producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().getFlowElements().add(producer);
            flow.getConsumer().setContainingFlow(flow);
            var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(id, java.util.List.of(flow));
            String saved = org.ikasan.studio.core.generator.ModelTemplate.create(module);
            String pom = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
            var expected = org.ikasan.studio.core.generator.ProjectArtifactRenderer.render(
                    org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(saved, "IDE parity source", false), pom);
            Path project = Files.createDirectory(parent.resolve("source project"));
            Path model = project.resolve("generated/src/main/model/model.json");
            Files.createDirectories(model.getParent()); Files.writeString(model, saved);
            Files.writeString(project.resolve("pom.xml"), pom);
            Files.createDirectories(project.resolve("user")); Files.writeString(project.resolve("user/Keep.java"), "// developer-owned");
            Path exportParent = Files.createDirectory(parent.resolve("export parent"));
            Path exported;
            try (var input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
                exported = OfflineToolsExporter.export(input, exportParent, saved, () -> { });
            }
            exported = Files.move(exported, parent.resolve("relocated tools"));
            var inventory = org.ikasan.studio.core.metapack.loading.OfflinePackBundle.verify(exported, () -> { });
            assertEquals(id, inventory.requiredPackId());
            var pack = inventory.packs().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
            assertEquals("0.1.0-SNAPSHOT", pack.packVersion());
            assertEquals("meta-packs/" + id + ".jar", pack.file());
            Path pendingProbe = exported.resolve("meta-packs/.pending/probe.json");
            Files.createDirectories(pendingProbe.getParent()); Files.writeString(pendingProbe, "{}");
            assertEquals(0, runCli(exported, "validate", "--project", project.toString()).code());
            assertEquals("{}", Files.readString(pendingProbe));
            assertFalse(Files.exists(exported.resolve("meta-packs/.management.lock")));
            Path output = parent.resolve("rendered preview");
            var render = runCli(exported, "render", "--project", project.toString(), "--output", output.toString());
            assertEquals(0, render.code(), render.output());
            var json = org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper();
            for (var artifact : expected.entrySet()) {
                String actual = Files.readString(output.resolve(artifact.getKey()));
                if (artifact.getKey().endsWith(".json")) assertEquals(json.readTree(artifact.getValue()), json.readTree(actual), artifact.getKey());
                else assertEquals(artifact.getValue(), actual, artifact.getKey());
            }
            assertTrue(Files.readString(output.resolve("generated/src/main/java/org/ikasan/studio/boot/Application.java")).contains("Offline custom pack marker"));
            assertEquals(saved, Files.readString(model)); assertEquals(pom, Files.readString(project.resolve("pom.xml")));
            assertEquals("// developer-owned", Files.readString(project.resolve("user/Keep.java")));
            assertEquals(1, runCli(exported, "render", "--project", project.toString(), "--output", output.toString()).code());
            assertEquals(1, runCli(exported, "render", "--project", project.toString(), "--output", project.resolve("preview").toString()).code());
            // Mutating the dedicated pack must fail before rendering publishes any output.
            Files.writeString(exported.resolve(pack.file()), "changed", StandardOpenOption.APPEND);
            var tampered = runCli(exported, "render", "--project", project.toString(), "--output", parent.resolve("rejected-preview").toString());
            assertEquals(1, tampered.code()); assertTrue(tampered.output().contains("checksum mismatch"), tampered.output());
            assertFalse(Files.exists(parent.resolve("rejected-preview")));
            return null;
        });
    }

    @Test void bundledSelectionUsesTheExistingPackJarAndRecordsItsRevision() throws Exception {
        String saved = org.ikasan.studio.core.generator.ModelTemplate.create(
                org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", java.util.List.of()));
        Path exported;
        try (var input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            exported = OfflineToolsExporter.export(input, parent, saved, () -> { });
        }
        var manifest = org.ikasan.studio.core.metapack.loading.OfflinePackBundle.verify(exported, () -> { });
        assertEquals("V4.1.6", manifest.requiredPackId());
        var expectedPacks = Boolean.getBoolean("studio.includeSnapshotPacks")
                ? java.util.Set.of("V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT") : java.util.Set.of("V3.3.9", "V4.1.6");
        assertEquals(expectedPacks,
                manifest.packs().stream().map(org.ikasan.studio.core.metapack.loading.OfflinePackBundle.Pack::id).collect(java.util.stream.Collectors.toSet()));
        var required = manifest.packs().stream().filter(pack -> pack.id().equals(manifest.requiredPackId())).findFirst().orElseThrow();
        assertTrue(required.file().startsWith("lib/"));
        try (var files = Files.list(exported.resolve("meta-packs"))) { assertEquals(0, files.count()); }
    }

    @Test void exportRejectsRevisionMismatchAndDoesNotPublishPartialTools() throws Exception {
        String source = org.ikasan.studio.core.generator.ModelTemplate.create(
                org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", java.util.List.of()));
        var json = org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper();
        var model = json.readTree(source);
        ((com.fasterxml.jackson.databind.node.ObjectNode) model.get("studio")).put("metaPackRevision", "99.0.0");
        try (var input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            var failure = assertThrows(IOException.class, () -> OfflineToolsExporter.export(input, parent, model.toString(), () -> { }));
            assertTrue(failure.getMessage().contains("locked to revision"), failure.getMessage());
        }
        assertFalse(Files.exists(parent.resolve(OfflineToolsExporter.DIRECTORY)));
        try (var files = Files.list(parent)) { assertEquals(0, files.count()); }
    }

    @Test void portableManifestIsRequiredAndUnlistedPackJarsAreRejected() throws Exception {
        Path exported;
        try (var input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            exported = OfflineToolsExporter.export(input, parent, () -> { });
        }
        String manifest = Files.readString(exported.resolve("meta-packs.json"));
        Files.delete(exported.resolve("meta-packs.json"));
        var missing = runCli(exported, "validate", "--project", parent.toString());
        assertEquals(1, missing.code()); assertTrue(missing.output().contains("meta-packs.json"), missing.output());
        Files.writeString(exported.resolve("meta-packs.json"), manifest);
        Files.copy(customPack(), exported.resolve("meta-packs/unlisted.jar"));
        var unlisted = runCli(exported, "validate", "--project", parent.toString());
        assertEquals(1, unlisted.code()); assertTrue(unlisted.output().contains("inventory differs"), unlisted.output());
    }

    private Path customPack() throws Exception {
        Path output = parent.resolve("AcmeOffline-source.jar");
        var url = getClass().getClassLoader().getResource("studio/metapack/V4.1.6/metapack.json");
        assertNotNull(url);
        try (var input = new java.util.jar.JarFile(org.ikasan.studio.core.metapack.loading.MetaPackArchive.sourceFile(url).toFile());
             var jar = new java.util.jar.JarOutputStream(Files.newOutputStream(output))) {
            for (var entry : java.util.Collections.list(input.entries())) {
                if (entry.isDirectory() || !entry.getName().startsWith("studio/metapack/V4.1.6/")) continue;
                String name = entry.getName().replace("V4.1.6", "AcmeOffline");
                byte[] bytes;
                try (var stream = input.getInputStream(entry)) { bytes = stream.readAllBytes(); }
                if (name.endsWith(".json")) bytes = new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("V4.1.6", "AcmeOffline").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if (name.endsWith("applicationTemplate_en.ftl")) bytes = (new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                        + "\n// Offline custom pack marker\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                jar.putNextEntry(new java.util.jar.JarEntry(name)); jar.write(bytes); jar.closeEntry();
            }
        }
        return output;
    }
    private record Result(int code, String output) { }
    private Result runCli(Path tools, String... arguments) throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var command = new java.util.ArrayList<>(java.util.List.of(javaExecutable, "-cp", tools.resolve("lib/*").toString(), "org.ikasan.studio.cli.StudioMigrationCli"));
        command.addAll(java.util.List.of(arguments));
        Path log = Files.createTempFile(parent, "cli-", ".log");
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS), "Exported CLI timed out");
            return new Result(process.exitValue(), Files.readString(log));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private InputStream archive(String... names) throws IOException {
        var buffer=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(buffer)) {
            for(String name:names){zip.putNextEntry(new ZipEntry(name));zip.write("test".getBytes());zip.closeEntry();}
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }
}
