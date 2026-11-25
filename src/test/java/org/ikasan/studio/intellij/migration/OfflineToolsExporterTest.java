package org.ikasan.studio.intellij.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;
import java.util.zip.ZipOutputStream;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.ikasan.studio.core.metapack.loading.MetaPackResources;
import org.ikasan.studio.core.metapack.loading.OfflinePackBundle.Manifest;
import org.ikasan.studio.core.metapack.loading.OfflinePackBundle.Pack;
import org.ikasan.studio.core.metapack.loading.OfflinePackBundle;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OfflineToolsExporterTest {
    @TempDir Path parent;
    @Test void extractsBundledToolsAndPreservesExecutableLauncher() throws Exception {
        try(InputStream archive=getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
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
                assertTrue(cli.waitFor(30, TimeUnit.SECONDS), "Standalone CLI timed out");
                assertEquals(0,cli.exitValue(),Files.readString(output));
                assertTrue(Files.readString(output).contains("preview --project"));
            } finally { if(cli.isAlive())cli.destroyForcibly(); }
            try(Stream<Path> libs=Files.list(exported.resolve("lib"))) {
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
        try(InputStream archive=archive("studio-cli-test/bin/ok", "studio-cli-test/../../outside")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        try(Stream<Path> files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void cancellationCleansStagingAndDoesNotPublishDestination() throws Exception {
        try(InputStream archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IllegalStateException.class,()->OfflineToolsExporter.export(archive,parent,()->{throw new IllegalStateException("cancelled");}));
        }
        try(Stream<Path> files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void rejectsIncompleteArchive() throws Exception {
        try(InputStream archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        assertFalse(Files.exists(parent.resolve(OfflineToolsExporter.DIRECTORY)));
    }
    @Test void customPackExportRunsAfterRelocationAndMatchesStudioRendering() throws Exception {
        MetaPackArchive archive = MetaPackArchive.open(customPack());
        MetaPackResources.inspect(archive, () -> {
            String id = archive.id();
            Flow flow = TestFixtures.getUnbuiltFlow(id)
                    .consumer(TestFixtures.getEventGeneratingConsumer(id)).build();
            FlowElement producer = TestFixtures.getDevNullProducer(id);
            producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().getFlowElements().add(producer);
            flow.getConsumer().setContainingFlow(flow);
            Module module = TestFixtures.getMyFirstModuleIkasanModule(id, List.of(flow));
            String saved = ModelTemplate.create(module);
            String pom = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
            Map<String,String> expected = ProjectArtifactRenderer.render(
                    ComponentIO.validatePersistedModuleJson(saved, "IDE parity source", false), pom);
            Path project = Files.createDirectory(parent.resolve("source project"));
            Path model = project.resolve("generated/src/main/model/model.json");
            Files.createDirectories(model.getParent()); Files.writeString(model, saved);
            Files.writeString(project.resolve("pom.xml"), pom);
            Files.createDirectories(project.resolve("user")); Files.writeString(project.resolve("user/Keep.java"), "// developer-owned");
            Path exportParent = Files.createDirectory(parent.resolve("export parent"));
            Path exported;
            try (InputStream input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
                exported = OfflineToolsExporter.export(input, exportParent, saved, () -> { });
            }
            exported = Files.move(exported, parent.resolve("relocated tools"));
            Manifest inventory = OfflinePackBundle.verify(exported, () -> { });
            assertEquals(id, inventory.requiredPackId());
            Pack pack = inventory.packs().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
            assertEquals(ComponentLibrary.getMetaPackManifest(id).packVersion(), pack.packVersion());
            assertEquals("meta-packs/" + id + ".jar", pack.file());
            Path pendingProbe = exported.resolve("meta-packs/.pending/probe.json");
            Files.createDirectories(pendingProbe.getParent()); Files.writeString(pendingProbe, "{}");
            assertEquals(0, runCli(exported, "validate", "--project", project.toString()).code());
            assertEquals("{}", Files.readString(pendingProbe));
            assertFalse(Files.exists(exported.resolve("meta-packs/.management.lock")));
            Path output = parent.resolve("rendered preview");
            Result render = runCli(exported, "render", "--project", project.toString(), "--output", output.toString());
            assertEquals(0, render.code(), render.output());
            ObjectMapper json = StudioJson.newObjectMapper();
            for (Entry<String,String> artifact : expected.entrySet()) {
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
            Result tampered = runCli(exported, "render", "--project", project.toString(), "--output", parent.resolve("rejected-preview").toString());
            assertEquals(1, tampered.code()); assertTrue(tampered.output().contains("checksum mismatch"), tampered.output());
            assertFalse(Files.exists(parent.resolve("rejected-preview")));
            return null;
        });
    }

    @Test void bundledSelectionUsesTheExistingPackJarAndRecordsItsRevision() throws Exception {
        String saved = ModelTemplate.create(
                TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", List.of()));
        Path exported;
        try (InputStream input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            exported = OfflineToolsExporter.export(input, parent, saved, () -> { });
        }
        Manifest manifest = OfflinePackBundle.verify(exported, () -> { });
        assertEquals("V4.1.6", manifest.requiredPackId());
        Set<String> expectedPacks = Set.of("V3.3.9", "V4.1.6", "V5.1.0");
        assertEquals(expectedPacks,
                manifest.packs().stream().map(Pack::id).collect(Collectors.toSet()));
        Pack required = manifest.packs().stream().filter(pack -> pack.id().equals(manifest.requiredPackId())).findFirst().orElseThrow();
        assertTrue(required.file().startsWith("lib/"));
        try (Stream<Path> files = Files.list(exported.resolve("meta-packs"))) { assertEquals(0, files.count()); }
    }

    @Test void exportRejectsRevisionMismatchAndDoesNotPublishPartialTools() throws Exception {
        String source = ModelTemplate.create(
                TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", List.of()));
        ObjectMapper json = StudioJson.newObjectMapper();
        JsonNode model = json.readTree(source);
        ((ObjectNode) model.get("studio")).put("metaPackRevision", "99.0.0");
        try (InputStream input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            IOException failure = assertThrows(IOException.class, () -> OfflineToolsExporter.export(input, parent, model.toString(), () -> { }));
            assertTrue(failure.getMessage().contains("locked to revision"), failure.getMessage());
        }
        assertFalse(Files.exists(parent.resolve(OfflineToolsExporter.DIRECTORY)));
        try (Stream<Path> files = Files.list(parent)) { assertEquals(0, files.count()); }
    }

    @Test void portableManifestIsRequiredAndUnlistedPackJarsAreRejected() throws Exception {
        Path exported;
        try (InputStream input = getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            exported = OfflineToolsExporter.export(input, parent, () -> { });
        }
        String manifest = Files.readString(exported.resolve("meta-packs.json"));
        Files.delete(exported.resolve("meta-packs.json"));
        Result missing = runCli(exported, "validate", "--project", parent.toString());
        assertEquals(1, missing.code()); assertTrue(missing.output().contains("meta-packs.json"), missing.output());
        Files.writeString(exported.resolve("meta-packs.json"), manifest);
        Files.copy(customPack(), exported.resolve("meta-packs/unlisted.jar"));
        Result unlisted = runCli(exported, "validate", "--project", parent.toString());
        assertEquals(1, unlisted.code()); assertTrue(unlisted.output().contains("inventory differs"), unlisted.output());
    }

    private Path customPack() throws Exception {
        Path output = parent.resolve("AcmeOffline-source.jar");
        URL url = getClass().getClassLoader().getResource("studio/metapack/V4.1.6/metapack.json");
        assertNotNull(url);
        try (JarFile input = new JarFile(MetaPackArchive.sourceFile(url).toFile());
             JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            for (JarEntry entry : Collections.list(input.entries())) {
                if (entry.isDirectory() || !entry.getName().startsWith("studio/metapack/V4.1.6/")) continue;
                String name = entry.getName().replace("V4.1.6", "AcmeOffline");
                byte[] bytes;
                try (InputStream stream = input.getInputStream(entry)) { bytes = stream.readAllBytes(); }
                if (name.endsWith(".json")) bytes = new String(bytes, StandardCharsets.UTF_8)
                        .replace("V4.1.6", "AcmeOffline").getBytes(StandardCharsets.UTF_8);
                if (name.endsWith("applicationTemplate_en.ftl")) bytes = (new String(bytes, StandardCharsets.UTF_8)
                        + "\n// Offline custom pack marker\n").getBytes(StandardCharsets.UTF_8);
                jar.putNextEntry(new JarEntry(name)); jar.write(bytes); jar.closeEntry();
            }
        }
        return output;
    }
    private record Result(int code, String output) { }
    private Result runCli(Path tools, String... arguments) throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        ArrayList<String> command = new ArrayList<>(List.of(javaExecutable, "-cp", tools.resolve("lib/*").toString(), "org.ikasan.studio.cli.StudioMigrationCli"));
        command.addAll(List.of(arguments));
        Path log = Files.createTempFile(parent, "cli-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Exported CLI timed out");
            return new Result(process.exitValue(), Files.readString(log));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private InputStream archive(String... names) throws IOException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(buffer)) {
            for(String name:names){zip.putNextEntry(new ZipEntry(name));zip.write("test".getBytes());zip.closeEntry();}
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }
}
