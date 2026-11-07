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
    public static final String USER_CONFIGURATION = "user/src/main/resources/studio-local.properties";
    public static final String USER_IMPORT = "optional:classpath:studio-local.properties";
    public static final String APPLICATION_CONFIGURATION = "generated/src/main/resources/application.properties";
    private EnvironmentProperties() { }

    public record Selection(String text, String environment, boolean localOnly, boolean userResource) {
        public Selection(String text, String environment, boolean localOnly) {
            this(text, environment, localOnly, false);
        }
        public static Selection developerOwned(String text) { return new Selection(text, "", false, true); }
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
            if (userResource) return USER_CONFIGURATION;
            return "config/" + (localOnly ? "local/" : "") + "application"
                    + (environment.isEmpty() ? "" : "-" + environment) + ".properties";
        }
    }

    public record Change(String key, String kind) { }
    public record Preview(Selection selection, String previous, List<Change> changes,
                          Set<String> generatedOverrides, Set<String> sharedOverrides, String applicationPrevious) {
        public Preview(Selection selection, String previous, List<Change> changes,
                       Set<String> generatedOverrides, Set<String> sharedOverrides) {
            this(selection, previous, changes, generatedOverrides, sharedOverrides, null);
        }
    }

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

    /** Captures raw destination bytes so the comparison can also repair invalid existing properties. */
    public static Preview previewDeveloperDestination(Path root) throws IOException {
        return new Preview(Selection.developerOwned(""), read(safePath(root, USER_CONFIGURATION)),
                List.of(), Set.of(), Set.of(), read(safePath(root, APPLICATION_CONFIGURATION)));
    }

    public static Preview preview(Path root, Selection selection) throws IOException {
        Path target = safePath(root, selection.relativePath());
        String previous = read(target);
        Properties before = parse(previous == null ? "" : previous);
        Properties after = parse(selection.text());
        validateImport(selection);
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
                Collections.unmodifiableSet(shared), selection.userResource() ? generated : null);
    }

    public static void validateImport(Selection selection) throws IOException {
        Properties after = parse(selection.text());
        for (String key : after.stringPropertyNames()) {
            if (key.equals("spring.profiles") || key.startsWith("spring.profiles.") || key.startsWith("spring.config."))
                throw new IOException("Remove launch-control property before import: " + key);
        }
    }

    /** Adds only the optional user resource import; all unrelated generated text is retained. */
    public static String withDeveloperImport(String text) throws IOException {
        String value = parse(text).getProperty("spring.config.import");
        if (value != null && Arrays.stream(value.split(",")).map(String::trim).anyMatch(entry -> USER_IMPORT.equals(entry) || "classpath:studio-local.properties".equals(entry))) return text;
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        if (value == null) return text + (text.isEmpty() || text.endsWith("\n") || text.endsWith("\r") ? "" : newline)
                + "spring.config.import=" + USER_IMPORT + newline;
        var matcher = java.util.regex.Pattern.compile("(?m)^(spring\\.config\\.import[ \t]*[=:][ \t]*)([^\r\n]*)").matcher(text);
        if (!matcher.find() || matcher.group(2).stripTrailing().endsWith("\\"))
            throw new IOException("Regenerate application.properties before importing: its spring.config.import uses an unsupported layout.");
        String updated = matcher.group(1) + matcher.group(2).stripTrailing()
                + (value.isBlank() ? "" : ",") + USER_IMPORT;
        return text.substring(0, matcher.start()) + updated + text.substring(matcher.end());
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
            validateImport(preview.selection());
            if (preview.selection().userResource()) {
                Path application = safePath(root, APPLICATION_CONFIGURATION);
                if (!Objects.equals(read(application), preview.applicationPrevious()))
                    throw new IOException("Application configuration changed since comparison; compare again.");
                if (preview.applicationPrevious() != null) {
                    String updated = withDeveloperImport(preview.applicationPrevious());
                    if (!updated.equals(preview.applicationPrevious())) {
                        original.put(application, preview.applicationPrevious());
                        writes.put(application, updated);
                    }
                }
            }
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
