package org.ikasan.studio.core.metapack.loading;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

/** Indexed resource-only JAR. No classes or service providers from the JAR are loaded. */
public final class MetaPackArchive {
    private static final String BASE = "studio/metapack/";
    private final Path file;
    private final String id;
    private final Set<String> entries;

    private MetaPackArchive(Path file, String id, Set<String> entries) {
        this.file = file;
        this.id = id;
        this.entries = Set.copyOf(entries);
    }

    public static MetaPackArchive open(Path file) throws IOException {
        Set<String> names = new HashSet<>();
        Set<String> ids = new HashSet<>();
        long expandedSize = 0;
        try (var jar = new JarFile(file.toFile(), false)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\")
                        || Arrays.asList(name.split("/")).contains("..")
                        || Arrays.asList(name.split("/")).contains(".") || !names.add(name)) {
                    throw new IOException("Unsafe or duplicate archive entry: " + name);
                }
                if (names.size() > 20000 || entry.getSize() > 64L * 1024 * 1024
                        || (expandedSize += Math.max(0, entry.getSize())) > 256L * 1024 * 1024) {
                    throw new IOException("Meta-pack archive exceeds resource limits");
                }
                if (entry.isDirectory()) continue;
                if (!name.startsWith(BASE) && !name.startsWith("META-INF/")) {
                    throw new IOException("Pack JAR must contain resources only: " + name);
                }
                if (name.endsWith(".class") || name.startsWith("META-INF/services/")) {
                    throw new IOException("Pack JAR must not supply executable classes or services: " + name);
                }
                if (name.startsWith(BASE)) {
                    String relative = name.substring(BASE.length());
                    int slash = relative.indexOf('/');
                    if (slash <= 0) throw new IOException("Resource outside a pack directory: " + name);
                    String id = relative.substring(0, slash);
                    if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) throw new IOException("Invalid pack ID: " + id);
                    ids.add(id);
                }
            }
        }
        if (ids.size() != 1) throw new IOException("A pack JAR must contain exactly one complete meta-pack");
        String id = ids.iterator().next();
        if (!names.contains(BASE + id + "/metapack.json")) throw new IOException("Missing metapack.json for " + id);
        return new MetaPackArchive(file.toAbsolutePath().normalize(), id, names);
    }

    /** IntelliJ supplies its own jar URLConnection, so identify the archive without casting connections. */
    public static Path sourceFile(URL resource) throws IOException {
        String location = resource.toExternalForm();
        int separator = location.lastIndexOf("!/");
        if (!"jar".equals(resource.getProtocol()) || separator < 4) throw new IOException("Pack source must be a local JAR");
        try {
            URI archive = URI.create(location.substring(4, separator));
            if (!"file".equals(archive.getScheme())) throw new IOException("Pack source must be a local JAR");
            return Path.of(archive);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid pack archive URL", invalid); }
    }

    public java.util.List<String> templateNames(String prefix) {
        return entries.stream().filter(name -> name.startsWith(prefix) && name.endsWith(".ftl"))
                .map(name -> name.substring(prefix.length())).sorted().toList();
    }

    public String id() { return id; }
    public Path file() { return file; }

    public URL resource(String name) {
        if (!entries.contains(name)) return null;
        try {
            String encoded = new URI(null, null, "/" + name, null).getRawPath().substring(1);
            return URI.create("jar:" + file.toUri().toASCIIString() + "!/" + encoded).toURL();
        } catch (Exception e) { throw new IllegalArgumentException("Invalid resource path: " + name, e); }
    }

    public String[] directories(String directory) {
        String prefix = directory + "/";
        var result = new TreeSet<String>();
        for (String name : entries) {
            if (!name.startsWith(prefix)) continue;
            int slash = name.indexOf('/', prefix.length());
            if (slash > 0) result.add(name.substring(0, slash));
        }
        return result.toArray(String[]::new);
    }
}
