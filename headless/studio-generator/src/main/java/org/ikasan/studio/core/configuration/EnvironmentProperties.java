package org.ikasan.studio.core.configuration;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/** Developer-owned Spring configuration. Imported text retains comments and placeholders. */
public final class EnvironmentProperties {
    private EnvironmentProperties() { }

    public record Selection(String text, String environment, boolean localOnly) {
        public Selection {
            Objects.requireNonNull(text);
            if (text.length() > 2 * 1024 * 1024) throw new IllegalArgumentException("Properties file exceeds 2 MiB.");
            // Spring Boot reads .properties as ISO-8859-1. Escape pasted Unicode without changing file bytes
            // read with that encoding, including pre-existing Unicode escapes, comments or line endings.
            StringBuilder encoded = new StringBuilder();
            for (char character : text.toCharArray()) {
                if (character > 255) encoded.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                else encoded.append(character);
            }
            text = encoded.toString();
            environment = environment.trim();
            if (!environment.isEmpty() && (!environment.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                    || environment.equals("studio-debug")))
                throw new IllegalArgumentException("Use a profile name starting with a letter, followed by letters, digits, _ or -. studio-debug is reserved.");
        }
        public String relativePath() {
            return "config/" + (localOnly ? "local/" : "") + "application"
                    + (environment.isEmpty() ? "" : "-" + environment) + ".properties";
        }
    }

    public record Change(String key, String kind) { }
    public record Preview(Selection selection, String previous, List<Change> changes,
                          Set<String> generatedOverrides, Set<String> sharedOverrides) { }

    public static Properties parse(String text) throws IOException {
        if (text.lines().anyMatch(line -> line.matches("[#!]---")))
            throw new IOException("Split multi-document properties into separate environment files before importing.");
        Properties result = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate property key: " + key);
                return super.put(key, value);
            }
        };
        try { result.load(new StringReader(text)); }
        catch (IllegalArgumentException failure) { throw new IOException("Invalid properties: " + failure.getMessage(), failure); }
        return result;
    }

    public static Preview preview(Path root, Selection selection) throws IOException {
        Path target = safePath(root, selection.relativePath());
        String previous = read(target);
        Properties before = parse(previous == null ? "" : previous);
        Properties after = parse(selection.text());
        for (String key : after.stringPropertyNames()) {
            if (key.equals("spring.profiles") || key.startsWith("spring.profiles.") || key.startsWith("spring.config."))
                throw new IOException("Remove launch-control property before import: " + key);
        }
        Set<String> keys = new TreeSet<>(before.stringPropertyNames());
        keys.addAll(after.stringPropertyNames());
        List<Change> changes = new ArrayList<>();
        for (String key : keys) {
            if (!before.containsKey(key)) changes.add(new Change(key, "added"));
            else if (!after.containsKey(key)) changes.add(new Change(key, "removed"));
            else if (!Objects.equals(before.getProperty(key), after.getProperty(key))) changes.add(new Change(key, "changed"));
        }
        String generated = read(safePath(root, "generated/src/main/resources/application.properties"));
        Properties defaults = parse(generated == null ? "" : generated);
        Set<String> overrides = new TreeSet<>(after.stringPropertyNames());
        overrides.retainAll(defaults.stringPropertyNames());
        Set<String> shared = new TreeSet<>();
        List<String> lowerFiles = new ArrayList<>();
        if (!selection.environment().isEmpty()) lowerFiles.add("config/application.properties");
        if (selection.localOnly()) {
            if (!selection.environment().isEmpty()) lowerFiles.add("config/local/application.properties");
            lowerFiles.add("config/application" + (selection.environment().isEmpty() ? "" : "-" + selection.environment()) + ".properties");
        }
        for (String lower : lowerFiles) {
            String lowerText = read(safePath(root, lower));
            if (lowerText != null) shared.addAll(parse(lowerText).stringPropertyNames());
        }
        shared.retainAll(after.stringPropertyNames());
        return new Preview(selection, previous, List.copyOf(changes), Collections.unmodifiableSet(overrides),
                Collections.unmodifiableSet(shared));
    }

    public static String comparisonBaseline(Path root, Preview preview) throws IOException {
        if (preview.previous() != null) return preview.previous();
        String defaults = read(safePath(root, "generated/src/main/resources/application.properties"));
        return defaults == null ? "" : defaults;
    }

    /** Refuses stale previews. Replacement requires explicit approval by the caller. */
    public static void apply(Path root, List<Preview> previews, boolean replaceExisting) throws IOException {
        Map<Path, String> original = new LinkedHashMap<>();
        Map<Path, String> writes = new LinkedHashMap<>();
        for (Preview preview : previews) {
            Path target = safePath(root, preview.selection().relativePath());
            if (writes.containsKey(target)) throw new IOException("Multiple imports target " + preview.selection().relativePath());
            if (!Objects.equals(read(target), preview.previous())) throw new IOException("Configuration changed since preview; review again: " + preview.selection().relativePath());
            if (preview.previous() != null && !replaceExisting) throw new IOException("Approve replacement of existing configuration files first.");
            original.put(target, preview.previous());
            writes.put(target, preview.selection().text());
        }
        if (previews.stream().anyMatch(p -> p.selection().localOnly())) {
            Path ignore = safePath(root, ".gitignore");
            String prior = read(ignore);
            String contents = prior == null ? "" : prior;
            if (!contents.stripTrailing().endsWith("\n/config/local/") && !contents.strip().equals("/config/local/")) {
                original.put(ignore, prior);
                writes.put(ignore, contents + (contents.endsWith("\n") || contents.isEmpty() ? "" : "\n")
                        + "\n# Ikasan Studio local environment configuration\n/config/local/\n");
            }
        }
        List<Path> written = new ArrayList<>();
        try {
            for (var entry : writes.entrySet()) {
                Path target = entry.getKey();
                safePath(root, root.toAbsolutePath().normalize().relativize(target).toString());
                if (!Objects.equals(read(target), original.get(target))) throw new IOException("Configuration changed during import; review again.");
                Files.createDirectories(target.getParent());
                try (var writer = Files.newBufferedWriter(target, encoding(target),
                        original.get(target) == null ? StandardOpenOption.CREATE_NEW : StandardOpenOption.TRUNCATE_EXISTING,
                        LinkOption.NOFOLLOW_LINKS)) {
                    written.add(target);
                    writer.write(entry.getValue());
                }
            }
        } catch (IOException failure) {
            Collections.reverse(written);
            for (Path target : written) {
                try {
                    if (original.get(target) == null) Files.deleteIfExists(target);
                    else Files.writeString(target, original.get(target), encoding(target), LinkOption.NOFOLLOW_LINKS,
                            StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        }
    }

    private static String read(Path file) throws IOException {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.size(file) > 2 * 1024 * 1024) throw new IOException("Properties file exceeds 2 MiB: " + file.getFileName());
        return Files.readString(file, encoding(file));
    }

    private static java.nio.charset.Charset encoding(Path file) {
        return file.getFileName().toString().endsWith(".properties") ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
    }

    private static Path safePath(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base)) throw new IOException("Configuration path escapes the project.");
        for (Path part = target; !part.equals(base); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("Configuration paths must not use symbolic links: " + part);
        }
        return target;
    }
}
