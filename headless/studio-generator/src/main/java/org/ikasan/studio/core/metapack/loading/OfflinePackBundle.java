package org.ikasan.studio.core.metapack.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

/** Portable, read-only resource packs. Checksums identify artifacts; they are not publisher signatures. */
public final class OfflinePackBundle {
    public static final String MANIFEST = "meta-packs.json";
    public static final String DIRECTORY = "meta-packs";
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public record Pack(String id, String packVersion, String file, String sha256) { }
    public record Manifest(int formatVersion, String requiredPackId, List<Pack> packs) {
        public Manifest { packs = packs == null ? List.of() : List.copyOf(packs); }
    }
    private OfflinePackBundle() { }

    /** Called inside the exporter's private staging directory; never writes into the source project. */
    public static Manifest write(Path tools, String savedModel, Runnable checkCancelled) throws Exception {
        String required = null;
        if (savedModel != null) {
            Module model = ComponentIO.validatePersistedModuleJson(savedModel, "saved project model for offline export", true);
            if (!Module.DUMB_MODULE_VERSION.equals(model.getVersion())) required = model.getVersion();
        }
        Files.createDirectories(tools.resolve(DIRECTORY));
        var archives = packFiles(tools);
        if (required != null) {
            checkCancelled.run();
            var expected = ComponentLibrary.getMetaPackManifest(required);
            var url = MetaPackResources.resource("studio/metapack/" + required + "/metapack.json");
            if (url == null || !url.getProtocol().equals("jar"))
                throw new IOException("Offline export requires a packaged JAR for meta-pack " + required + "; build the pack first");
            Path source = MetaPackArchive.sourceFile(url);
            String sourceHash = MetaPackStore.sha256(source);
            Path destination = null;
            for (Path file : archives) {
                if (MetaPackStore.manifest(file).id().equals(required)) {
                    if (destination != null) throw new IOException("Duplicate offline pack ID " + required);
                    destination = file;
                }
            }
            if (destination == null) {
                destination = tools.resolve(DIRECTORY).resolve(required + ".jar");
                Files.copy(source, destination); // Export staging is private; never replace another file.
                archives.add(destination);
            }
            if (!sourceHash.equals(MetaPackStore.sha256(source)) || !sourceHash.equals(MetaPackStore.sha256(destination)))
                throw new IOException("The required pack changed during export, or the bundled tools contain another artifact; export again from a matching Studio build");
            if (!expected.equals(MetaPackInstaller.validate(MetaPackArchive.open(destination))))
                throw new IOException("The exported pack does not match the project's selected metadata");
        }
        var packs = new ArrayList<Pack>();
        for (Path file : archives) {
            checkCancelled.run();
            var manifest = MetaPackInstaller.validate(MetaPackArchive.open(file));
            packs.add(new Pack(manifest.id(), manifest.packVersion(), tools.relativize(file).toString().replace('\\', '/'), MetaPackStore.sha256(file)));
        }
        packs.sort(Comparator.comparing(Pack::id));
        var manifest = new Manifest(1, required, packs);
        Files.writeString(tools.resolve(MANIFEST), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n", StandardOpenOption.CREATE_NEW);
        verify(tools, checkCancelled);
        return manifest;
    }

    /** Old generic distributions without an external-pack directory retain their bundled-only behaviour. */
    public static void verifyIfPresent(Path tools) throws Exception {
        if (tools != null && (Files.exists(tools.resolve(MANIFEST)) || Files.exists(tools.resolve(DIRECTORY)))) verify(tools, () -> { });
    }

    public static Manifest verify(Path tools, Runnable checkCancelled) throws Exception {
        Path root = tools.toRealPath();
        if (Files.isSymbolicLink(root.resolve(MANIFEST))) throw new IOException("Offline pack manifest must not be a symlink");
        var manifest = JSON.readValue(Files.readString(root.resolve(MANIFEST)), Manifest.class);
        if (manifest.formatVersion() != 1 || manifest.packs().isEmpty()) throw new IOException("Unsupported or empty offline pack manifest");
        var ids = new HashSet<String>();
        var expected = new HashSet<Path>();
        for (Pack pack : manifest.packs()) {
            checkCancelled.run();
            if (pack.id() == null || !pack.id().matches("[A-Za-z0-9][A-Za-z0-9._-]*") || !ids.add(pack.id())
                    || pack.packVersion() == null || pack.file() == null || !pack.file().matches("(lib|meta-packs)/[A-Za-z0-9._-]+\\.jar")
                    || pack.sha256() == null || !pack.sha256().matches("[a-f0-9]{64}")) throw new IOException("Invalid or duplicate offline pack record");
            Path file = root.resolve(pack.file());
            if (!expected.add(file) || Files.isSymbolicLink(file) || Files.isSymbolicLink(file.getParent())
                    || !file.toRealPath().startsWith(root)) throw new IOException("Unsafe or duplicate offline pack path: " + pack.file());
            if (!MetaPackStore.sha256(file).equals(pack.sha256())) throw new IOException("Offline pack checksum mismatch: " + pack.file());
            var actual = MetaPackStore.manifest(file);
            if (!actual.id().equals(pack.id()) || !Objects.equals(actual.packVersion(), pack.packVersion()))
                throw new IOException("Offline pack ID/revision mismatch: " + pack.file());
            MetaPackInstaller.validate(MetaPackArchive.open(file));
        }
        if (manifest.requiredPackId() != null && !ids.contains(manifest.requiredPackId())) throw new IOException("Required project pack is missing from the offline manifest");
        if (!expected.equals(new HashSet<>(packFiles(root)))) throw new IOException("Offline pack inventory differs from its manifest; use a fresh export");
        return manifest;
    }

    private static ArrayList<Path> packFiles(Path tools) throws IOException {
        var result = new ArrayList<Path>();
        if (!Files.isDirectory(tools.resolve("lib"))) throw new IOException("Offline tools lib directory is missing");
        for (String name : List.of("lib", DIRECTORY)) {
            Path directory = tools.resolve(name);
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                    boolean pack = name.equals(DIRECTORY);
                    if (!pack) try (var jar = new JarFile(file.toFile(), false)) {
                        pack = jar.stream().anyMatch(entry -> entry.getName().matches("studio/metapack/[^/]+/metapack\\.json"));
                    }
                    if (pack) { MetaPackArchive.open(file); result.add(file); }
                }
            }
        }
        return result;
    }
}
