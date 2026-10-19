package org.ikasan.studio.core.metapack.loading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/** Session-fixed pack sources shared by metadata, templates and icons; never an active pack selection. */
public final class MetaPackResources {
    private static final Logger LOG = LoggerFactory.getLogger(MetaPackResources.class);
    private record SourceIndex(Map<String, MetaPackArchive> archives, Map<String, String> problems) { }
    private static final SourceIndex SOURCES = discover();
    private static final ThreadLocal<MetaPackArchive> CANDIDATE = new ThreadLocal<>();
    private MetaPackResources() { }

    private static ClassLoader bundled() { return MetaPackResources.class.getClassLoader(); }

    private static SourceIndex discover() {
        var problems = new LinkedHashMap<String, String>();
        var result = new LinkedHashMap<String, MetaPackArchive>();
        var conflicts = new HashSet<String>();
        for (var provider : ServiceLoader.load(MetaPackDirectoryProvider.class, bundled())) {
            var directory = provider.directory();
            if (!Files.exists(directory)) continue;
            problems.putAll(MetaPackStore.activatePending(directory));
            try (var files = Files.list(directory)) {
                for (var file : files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                    try {
                        var archive = MetaPackArchive.open(file);
                        if (bundled().getResource("studio/metapack/" + archive.id() + "/metapack.json") != null
                                || result.containsKey(archive.id()) || conflicts.contains(archive.id())) {
                            conflicts.add(archive.id());
                            result.remove(archive.id());
                            throw new IOException("Conflicting pack ID " + archive.id() + "; no external revision will be loaded");
                        }
                        result.put(archive.id(), archive);
                    } catch (IOException e) {
                        problems.put(file.toString(), e.getMessage());
                        LOG.warn("Cannot load meta-pack {}: {}", file, e.getMessage());
                    }
                }
            } catch (IOException e) {
                problems.put(directory.toString(), e.getMessage());
                LOG.warn("Cannot read meta-pack directory {}", directory, e);
            }
        }
        return new SourceIndex(Map.copyOf(result), Map.copyOf(problems));
    }

    public static void initialize() { /* Class initialization fixes the sources for this session. */ }

    public static Map<String, String> installationProblems() { return SOURCES.problems(); }

    private static MetaPackArchive archive(String path) {
        String prefix = "studio/metapack/";
        if (!path.startsWith(prefix)) return null;
        String id = path.substring(prefix.length()).split("/", 2)[0];
        var candidate = CANDIDATE.get();
        return candidate != null && candidate.id().equals(id) ? candidate : SOURCES.archives().get(id);
    }

    public static URL resource(String path) {
        var archive = archive(path);
        return archive == null ? bundled().getResource(path) : archive.resource(path);
    }

    public static InputStream open(String path) throws IOException {
        var url = resource(path);
        if (url == null) return null;
        var connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    public static String[] directories(String path) throws IOException, java.net.URISyntaxException {
        var archive = archive(path);
        if (archive != null) return archive.directories(path);
        var result = new TreeSet<>(Arrays.asList(ClasspathDirectoryScanner.getDirectories(path)));
        if (path.equals("studio/metapack")) {
            SOURCES.archives().keySet().forEach(id -> result.add(path + "/" + id));
        }
        return result.toArray(String[]::new);
    }

    /** Resource lookup only: never adds the pack JAR to the Java classpath. */
    public static ClassLoader templateLoader(String id) {
        // Capture the source so a FreeMarker configuration remains bound to the candidate after validation scope ends.
        var source = archive("studio/metapack/" + id + "/metapack.json");
        if (source == null) return bundled();
        return new ClassLoader(bundled()) {
            @Override public URL getResource(String name) {
                return name.startsWith("studio/metapack/" + id + "/") ? source.resource(name) : null;
            }
        };
    }

    @FunctionalInterface
    public interface Inspection<T> { T run() throws Exception; }

    /** Isolated authoring/installation inspection, invisible to other threads and existing projects. */
    public static <T> T inspect(MetaPackArchive archive, Inspection<T> inspection) throws Exception {
        var previous = CANDIDATE.get();
        CANDIDATE.set(archive);
        try { return inspection.run(); }
        finally {
            if (previous == null) CANDIDATE.remove(); else CANDIDATE.set(previous);
        }
    }
}
