package org.ikasan.studio.cli;

import org.ikasan.studio.core.migration.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.io.PrintStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Standalone entry point sharing the IDE's migration renderer and recovery transaction. */
public final class StudioMigrationCli {
    public record Preview(int formatVersion, String project, String targetVersion, String report,
                          List<MigrationWorkspace.Change> changes, Map<String,String> userHashes) {}
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 0 || args[0].equals("--help")) {
                out.println("""
                        Studio migration CLI (Java 17+)
                        preview --project PATH --to V4.1.6 --plan NEW_FILE.json
                        apply --plan FILE.json
                        Close the project in IntelliJ and stop the module before apply.
                        Preview is read-only for the project. Apply uses the saved preview,
                        rejects stale files, preserves user/, and writes a recovery snapshot.
                        It does not change IDE SDK settings or run application tests.
                        """);
                return 0;
            }
            Map<String,String> options = options(args);
            var json = strictJson();
            if (args[0].equals("preview")) {
                requireKeys(options, Set.of("--project", "--to", "--plan"));
                Path root = Path.of(options.get("--project")).toRealPath();
                String source = Files.readString(root.resolve(MigrationArtifacts.MODEL));
                String pom = Files.readString(root.resolve("pom.xml"));
                var plan = ModelMigration.analyse(source, options.get("--to"));
                out.println(plan.report());
                if (!plan.canApply()) return 2;
                var changes = MigrationWorkspace.prepare(root, MigrationArtifacts.render(plan, pom));
                // Detect source/POM edits during rendering as well as after the preview.
                requireOriginal(changes, MigrationArtifacts.MODEL, source);
                requireOriginal(changes, "pom.xml", pom);
                var preview = new Preview(1, root.toString(), plan.targetVersion(), plan.report(), changes, userHashes(root));
                Path file = Path.of(options.get("--plan")).toAbsolutePath().normalize();
                if (file.startsWith(root.resolve("generated")) || file.startsWith(root.resolve("user")))
                    throw new IllegalArgumentException("Store the preview outside generated/ and user/.");
                Path diff = file.resolveSibling(file.getFileName() + ".diff");
                if (Files.exists(file) || Files.exists(diff)) throw new IllegalArgumentException("Choose a fresh preview filename.");
                Files.writeString(diff, readableDiff(changes), StandardOpenOption.CREATE_NEW);
                Files.writeString(file, json.writerWithDefaultPrettyPrinter().writeValueAsString(preview), StandardOpenOption.CREATE_NEW);
                out.println("Preview saved: " + file + "\nReview file changes: " + diff);
                for (var change : changes) if (!Objects.equals(change.before(), change.after())) out.println("CHANGE " + change.path());
                return 0;
            }
            if (args[0].equals("apply")) {
                requireKeys(options, Set.of("--plan"));
                var preview = json.readValue(Files.readString(Path.of(options.get("--plan"))), Preview.class);
                if (preview.formatVersion() != 1) throw new IllegalArgumentException("Unsupported preview format.");
                Path root = Path.of(preview.project()).toRealPath();
                if (!userHashes(root).equals(preview.userHashes())) throw new IllegalStateException("Developer files changed after preview; preview again.");
                var plan = ModelMigration.analyse(Files.readString(root.resolve(MigrationArtifacts.MODEL)), preview.targetVersion());
                if (!plan.canApply()) throw new IllegalStateException(plan.report());
                var current = MigrationWorkspace.prepare(root, MigrationArtifacts.render(plan, Files.readString(root.resolve("pom.xml"))));
                if (!sameChanges(current, preview.changes())) throw new IllegalStateException("Project or renderer changed after preview; preview again.");
                // Commit enforces permitted paths, rejects symlinks/concurrent edits and rolls back write failures.
                Path recovery = MigrationWorkspace.commit(root, preview.changes(), plan.report());
                out.println("Migration applied. Recovery snapshot: " + recovery);
                out.println("Select the target JDK for Maven/IntelliJ, reimport, then run after-upgrade verification.");
                return 0;
            }
            throw new IllegalArgumentException("Unknown command: " + args[0]);
        } catch (Exception failure) {
            err.println("Migration failed: " + failure.getMessage());
            return 1;
        }
    }
    private static com.fasterxml.jackson.databind.ObjectMapper strictJson() {
        return StudioJson.newObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    private static boolean sameChanges(List<MigrationWorkspace.Change> current, List<MigrationWorkspace.Change> preview) throws Exception {
        if (current.size()!=preview.size()) return false;
        var json = strictJson();
        for (int i=0;i<current.size();i++) {
            var a=current.get(i); var b=preview.get(i);
            if (!a.path().equals(b.path()) || !Objects.equals(a.before(),b.before())) return false;
            if (Objects.equals(a.after(),b.after())) continue;
            // Map.of iteration order varies between JVMs. Compare rendered JSON values,
            // while preserving byte-exact stale-file guards and applying the reviewed bytes.
            if (!a.path().endsWith(".json") || a.after()==null || b.after()==null ||
                    !json.readTree(a.afterText()).equals(json.readTree(b.afterText()))) return false;
        }
        return true;
    }
    private static String readableDiff(List<MigrationWorkspace.Change> changes) throws Exception {
        var result = new StringBuilder();
        for (var change : changes) {
            if (Objects.equals(change.before(), change.after())) continue;
            String before = change.beforeText(), after = change.afterText();
            if (change.path().equals(MigrationArtifacts.MODEL)) {
                var json = strictJson();
                before = json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(before));
                after = json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(after));
            }
            var a = before.lines().toList(); var b = after.lines().toList();
            int prefix=0, suffix=0;
            while (prefix<Math.min(a.size(),b.size()) && a.get(prefix).equals(b.get(prefix))) prefix++;
            while (suffix<Math.min(a.size(),b.size())-prefix && a.get(a.size()-1-suffix).equals(b.get(b.size()-1-suffix))) suffix++;
            int start=Math.max(0,prefix-3), endA=Math.min(a.size(),a.size()-suffix+3), endB=Math.min(b.size(),b.size()-suffix+3);
            result.append("--- a/").append(change.path()).append("\n+++ b/").append(change.path())
                    .append("\n@@ -").append(start+1).append(',').append(endA-start)
                    .append(" +").append(start+1).append(',').append(endB-start).append(" @@\n");
            for (int i=start;i<prefix;i++) result.append(' ').append(a.get(i)).append('\n');
            for (int i=prefix;i<a.size()-suffix;i++) result.append('-').append(a.get(i)).append('\n');
            for (int i=prefix;i<b.size()-suffix;i++) result.append('+').append(b.get(i)).append('\n');
            for (int i=a.size()-suffix;i<endA;i++) result.append(' ').append(a.get(i)).append('\n');
        }
        return result.toString();
    }
    private static Map<String,String> options(String[] args) {
        var result = new LinkedHashMap<String,String>();
        for (int i=1; i<args.length; i+=2) {
            if (i+1>=args.length || !args[i].startsWith("--") || result.putIfAbsent(args[i], args[i+1])!=null)
                throw new IllegalArgumentException("Expected unique --option VALUE pairs.");
        }
        return result;
    }
    private static void requireKeys(Map<String,String> options, Set<String> keys) {
        if (!options.keySet().equals(keys)) throw new IllegalArgumentException("Required options: " + keys);
    }
    private static void requireOriginal(List<MigrationWorkspace.Change> changes, String path, String expected) {
        if (!changes.stream().filter(c -> c.path().equals(path)).findFirst().orElseThrow().beforeText().equals(expected))
            throw new IllegalStateException("Project changed during preview: " + path);
    }
    private static Map<String,String> userHashes(Path root) throws Exception {
        var hashes = new TreeMap<String,String>();
        Path user = root.resolve("user");
        if (!Files.isDirectory(user)) throw new IllegalArgumentException("Expected a Studio project with user/.");
        try (var paths = Files.walk(user)) {
            for (Path p : paths.toList()) {
                if (user.relativize(p).startsWith("target")) continue;
                if (Files.isSymbolicLink(p)) throw new IllegalArgumentException("Developer files must not be symlinks: " + p);
                if (Files.isRegularFile(p)) hashes.put(root.relativize(p).toString(),
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));
            }
        }
        return hashes;
    }
}
