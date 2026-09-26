package org.ikasan.studio.core.migration;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative, opt-in import edits only; never searches/replaces arbitrary source text. */
public final class UserImportMigration {
    private UserImportMigration() { }
    private static final Pattern IMPORT = Pattern.compile("(?m)^[\\t ]*import[\\t ]+(?:static[\\t ]+)?([\\w.$]+(?:\\.\\*)?)[\\t ]*;");

    public static boolean permittedPath(String path) {
        return path.startsWith("user/src/main/java/") && path.endsWith(".java");
    }

    public static List<MigrationWorkspace.Change> prepare(Path root, ModelMigration.Plan plan) throws Exception {
        var prefixes = MigrationRules.load(plan.sourceVersion(), plan.targetVersion()).userImportPrefixes();
        List<MigrationWorkspace.Change> changes = new ArrayList<>();
        Path source = root.resolve("user/src/main/java");
        for (Path p = source; p != null && p.startsWith(root); p = p.getParent())
            if (Files.isSymbolicLink(p)) throw new IllegalArgumentException("Import migration does not follow symlinks: " + p);
        if (!Files.exists(source)) return changes;
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted().toList()) {
                if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("Import migration does not follow symlinks: " + path);
                if (!Files.isRegularFile(path) || !path.toString().endsWith(".java")) continue;
                String before = Files.readString(path);
                String after = rewrite(before, prefixes);
                if (!before.equals(after)) changes.add(new MigrationWorkspace.Change(
                        root.relativize(path).toString().replace('\\', '/'), encode(before), encode(after)));
            }
        }
        return List.copyOf(changes);
    }

    private static String encode(String text) { return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }

    /** Mask comments and literals, retaining offsets and line endings. Escaped Unicode needs manual review. */
    public static String rewrite(String source, Map<String, String> prefixes) {
        if (source.contains("\\u")) return source;
        char[] mask = source.toCharArray();
        int i = 0;
        while (i < source.length()) {
            int start = i;
            if (source.startsWith("//", i)) {
                i += 2; while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') i++;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2); i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("\"\"\"", i)) {
                i += 3;
                while (i < source.length()) {
                    if (source.charAt(i) == '\\') { i = Math.min(source.length(), i + 2); continue; }
                    if (source.startsWith("\"\"\"", i)) { i += 3; break; }
                    i++;
                }
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                char quote = source.charAt(i++);
                while (i < source.length()) {
                    char c = source.charAt(i++);
                    if (c == '\\') i = Math.min(source.length(), i + 1);
                    else if (c == quote) break;
                }
            } else { i++; continue; }
            for (int j = start; j < i; j++) if (mask[j] != '\n' && mask[j] != '\r') mask[j] = ' ';
        }
        var matcher = IMPORT.matcher(new String(mask));
        StringBuilder result = new StringBuilder(source);
        List<int[]> locations = new ArrayList<>(); List<String> replacements = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            for (String prefix : new TreeSet<>(prefixes.keySet())) {
                if (name.startsWith(prefix)) {
                    locations.add(new int[]{matcher.start(1), matcher.end(1)});
                    replacements.add(prefixes.get(prefix) + name.substring(prefix.length())); break;
                }
            }
        }
        for (int n = locations.size() - 1; n >= 0; n--) result.replace(locations.get(n)[0], locations.get(n)[1], replacements.get(n));
        return result.toString();
    }

    public static String report(String report, List<MigrationWorkspace.Change> changes) {
        StringBuilder text = new StringBuilder(report.replace("Existing user/ files are preserved.",
                "Only reviewed Java import edits in user/src/main/java are permitted; other developer code and tests are preserved."));
        text.append("\nUser import migration: ").append(changes.size()).append(" file(s).\n");
        changes.forEach(c -> text.append("IMPORT UPDATE: ").append(c.path()).append('\n'));
        return text.append("Review remaining fully qualified types, configuration strings and dependencies manually. Files with Unicode escapes are left unchanged.\n").toString();
    }
}
