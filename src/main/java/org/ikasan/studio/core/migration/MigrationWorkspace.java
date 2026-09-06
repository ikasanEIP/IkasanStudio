package org.ikasan.studio.core.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.persistence.json.StudioJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Exact-byte recovery snapshots and optimistic, rollback-capable writes. Run outside the EDT. */
public final class MigrationWorkspace {
    private static final ObjectMapper JSON = StudioJson.newObjectMapper();
    public static final String HISTORY = ".ikasan-studio/migrations";
    private MigrationWorkspace() { }

    public record Change(String path, String before, String after) {
        public String beforeText() { return decodeText(before); }
        public String afterText() { return decodeText(after); }
        private static String decodeText(String encoded) { return encoded == null ? "" : new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
    }
    public record Snapshot(int formatVersion, String id, boolean committed, String report, List<Change> changes) {
        public Snapshot { changes = List.copyOf(changes); }
    }

    public static List<Change> prepare(Path root, Map<String, String> artifacts) throws IOException {
        List<Change> changes = new ArrayList<>();
        for (var artifact : artifacts.entrySet()) {
            String before = read(safe(root, artifact.getKey()));
            String after = artifact.getValue() == null ? null : encode(artifact.getValue().getBytes(StandardCharsets.UTF_8));
            changes.add(new Change(artifact.getKey(), before, after));
        }
        return List.copyOf(changes);
    }

    public static List<Change> prepareRestore(Path root, Snapshot snapshot) throws IOException {
        if (snapshot.formatVersion() != 1 || !snapshot.committed()) throw new IOException("This snapshot is not a completed migration.");
        List<Change> changes = new ArrayList<>();
        for (Change change : snapshot.changes()) {
            String current = read(safe(root, change.path()));
            // Instructions added by a team after migration remain developer-owned.
            if (change.path().equals("AGENTS.md") && !Objects.equals(current, change.after())) continue;
            changes.add(new Change(change.path(), current, change.before()));
        }
        return List.copyOf(changes);
    }

    public static Snapshot latest(Path root) throws IOException {
        Path history = safeHistory(root);
        if (!Files.isDirectory(history)) throw new IOException("This project has no migration snapshots.");
        try (var paths = Files.list(history)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("A migration snapshot must not be a symbolic link.");
                Snapshot snapshot = JSON.readValue(Files.readString(path), Snapshot.class);
                if (snapshot.committed()) return snapshot;
            }
        }
        throw new IOException("This project has no completed migration snapshots.");
    }

    public static Path commit(Path root, List<Change> changes, String report) throws IOException {
        Set<String> paths = new HashSet<>();
        for (Change change : changes) {
            if (!paths.add(change.path())) throw new IOException("Duplicate migration path: " + change.path());
            if (!Objects.equals(change.before(), read(safe(root, change.path())))) {
                throw new IOException("The project changed after preview: " + change.path() + ". Preview again.");
            }
        }
        Path history = safeHistory(root);
        Files.createDirectories(history);
        String id = Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID();
        Path snapshotPath = history.resolve(id + ".json");
        Snapshot pending = new Snapshot(1, id, false, report, changes);
        write(snapshotPath, encode(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(pending)));
        List<Change> applied = new ArrayList<>();
        try {
            for (Change change : changes) {
                Path path = safe(root, change.path());
                if (!Objects.equals(change.before(), read(path))) throw new IOException("Concurrent change to " + change.path());
                if (Objects.equals(change.before(), change.after())) continue;
                write(path, change.after());
                applied.add(change);
            }
            write(snapshotPath, encode(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(new Snapshot(1, id, true, report, changes))));
            return snapshotPath;
        } catch (IOException | RuntimeException failure) {
            IOException result = new IOException("Migration failed; restoring original files. Recovery snapshot: " + snapshotPath, failure);
            Collections.reverse(applied);
            for (Change change : applied) {
                try {
                    Path path = safe(root, change.path());
                    if (!Objects.equals(read(path), change.after())) throw new IOException("File changed during recovery: " + change.path());
                    write(path, change.before());
                }
                catch (IOException | RuntimeException rollback) { result.addSuppressed(rollback); }
            }
            if (result.getSuppressed().length > 0) throw new IOException("Automatic restoration was incomplete. Use recovery snapshot " + snapshotPath, result);
            throw result;
        }
    }

    private static String read(Path path) throws IOException { return Files.exists(path) ? encode(Files.readAllBytes(path)) : null; }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static void write(Path path, String value) throws IOException {
        if (value == null) { Files.deleteIfExists(path); return; }
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), ".studio-migration-", ".tmp");
        try {
            Files.write(temp, Base64.getDecoder().decode(value));
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private static Path safe(Path root, String relative) throws IOException {
        if (!(relative.equals("pom.xml") || relative.equals("AGENTS.md") || relative.startsWith("generated/"))) {
            throw new IOException("Migration cannot write developer-owned path: " + relative);
        }
        return resolve(root, relative);
    }
    private static Path safeHistory(Path root) throws IOException { return resolve(root, HISTORY); }
    private static Path resolve(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path candidate = base.resolve(relative).normalize();
        if (!candidate.startsWith(base) || Path.of(relative).isAbsolute() || !Path.of(relative).normalize().toString().equals(relative)) {
            throw new IOException("Invalid migration path: " + relative);
        }
        for (Path part = candidate; part != null && part.startsWith(base); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("Migration does not follow symbolic links: " + part);
        }
        return candidate;
    }
}
