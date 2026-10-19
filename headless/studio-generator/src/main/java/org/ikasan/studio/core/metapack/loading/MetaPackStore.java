package org.ikasan.studio.core.metapack.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Restart-bound external-pack changes. Active JARs are never changed by a management operation. */
public final class MetaPackStore {
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private MetaPackStore() { }
    public record Entry(String id, String revision, Path file, String pending, String problem, String sha256) {
        // Keep the headless core independent of IDE annotation dependencies.
        @SuppressWarnings("NullableProblems")
        @Override public String toString() {
            return id + " (" + revision + ")" + (pending == null ? "" : " — " + pending)
                    + (problem == null ? "" : " — " + problem);
        }
    }
    private record Change(int format, String operation, String id, String expectedSha256, String candidateSha256) { }
    @FunctionalInterface interface Operation<T> { T run() throws Exception; }

    static <T> T locked(Path directory, Operation<T> operation) throws Exception {
        Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve(".management.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            try (var lock = channel.tryLock()) {
                if (lock == null) throw new IOException("Another IDE is managing this meta-pack directory; try again later");
                return operation.run();
            } catch (java.nio.channels.OverlappingFileLockException e) {
                throw new IOException("Another task is managing this meta-pack directory; try again later", e);
            }
        }
    }

    public static MetaPackManifest manifest(Path file) throws IOException {
        var archive = MetaPackArchive.open(file);
        var resource = archive.resource("studio/metapack/" + archive.id() + "/metapack.json");
        if (resource == null) throw new IOException("Missing metapack.json for " + archive.id());
        var connection = resource.openConnection();
        connection.setUseCaches(false);
        try (var input = connection.getInputStream()) {
            var manifest = JSON.readValue(input, MetaPackManifest.class);
            if (!archive.id().equals(manifest.id())) throw new IOException("Manifest ID does not match the archive directory");
            return manifest;
        }
    }

    public static List<Entry> list(Path directory) throws IOException {
        MetaPackResources.initialize();
        if (!Files.exists(directory)) return List.of();
        var result = new ArrayList<Entry>();
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                try {
                    String hash = sha256(file);
                    var manifest = manifest(file);
                    if (!hash.equals(sha256(file))) throw new IOException("Pack changed while listing; refresh and review again");
                    Path plan = plan(directory, manifest.id());
                    String pending = null;
                    String problem = null;
                    if (Files.exists(plan)) {
                        try { pending = readChange(plan).operation(); }
                        catch (Exception invalid) { pending = "invalid"; problem = invalid.toString(); }
                    }
                    result.add(new Entry(manifest.id(), manifest.packVersion(), file, pending, problem, hash));
                } catch (Exception e) {
                    result.add(new Entry(file.getFileName().toString(), "?", file, null, e.toString(), null));
                }
            }
        }
        Path historyRoot = directory.resolve(".history");
        if (Files.exists(historyRoot)) try (var directories = Files.list(historyRoot)) {
            for (Path archived : directories.filter(Files::isDirectory).sorted().toList()) {
                String id = archived.getFileName().toString();
                if (Files.exists(directory.resolve(id + ".jar"))) continue;
                var revisions = history(directory, id);
                if (!revisions.isEmpty()) {
                    var manifest = manifest(revisions.get(0));
                    result.add(new Entry(id, manifest.packVersion(), directory.resolve(id + ".jar"), "removed", null, null));
                }
            }
        }
        return List.copyOf(result);
    }

    public record Candidate(MetaPackManifest manifest, String sha256) { }

    public static Candidate review(Path source) throws Exception {
        String hash = sha256(source);
        var manifest = MetaPackInstaller.validate(MetaPackArchive.open(source));
        if (!hash.equals(sha256(source))) throw new IOException("The candidate changed during review; review again");
        return new Candidate(manifest, hash);
    }

    @SuppressWarnings("UnusedReturnValue") // Preserve the manifest result for headless API consumers.
    public static MetaPackManifest stageUpdate(Path source, Path current, Path directory, Runnable checkCancelled) throws Exception {
        return stageUpdate(source, current, directory, checkCancelled, null, null);
    }

    public static MetaPackManifest stageUpdate(Path source, Path current, Path directory, Runnable checkCancelled,
                                              Candidate reviewed, String expectedCurrentHash) throws Exception {
        MetaPackResources.initialize();
        return locked(directory, () -> {
            requireCurrent(current, directory);
            String originalHash = sha256(current);
            if (expectedCurrentHash != null && !originalHash.equals(expectedCurrentHash))
                throw new IOException("The installed pack changed after review; refresh and review again");
            var before = manifest(current);
            checkId(before.id());
            if (Files.exists(plan(directory, before.id()))) throw new IOException("Cancel the pending change before scheduling another one");
            Path staged = Files.createTempFile(directory, ".update-", ".tmp");
            Path candidate = null;
            boolean published = false;
            boolean candidateMoved = false;
            try {
                checkCancelled.run();
                Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
                var archive = MetaPackArchive.open(staged);
                var after = MetaPackInstaller.validate(archive);
                if (!before.id().equals(after.id())) throw new IOException("An update must retain the pack ID " + before.id());
                if (!before.ikasanVersion().equals(after.ikasanVersion())) throw new IOException("Changing the Ikasan runtime requires a separate pack ID and an explicit migration");
                if (Objects.equals(before.packVersion(), after.packVersion())) throw new IOException("An update must use a different immutable packVersion");
                String hash = sha256(staged);
                if (reviewed != null && (!hash.equals(reviewed.sha256()) || !after.equals(reviewed.manifest())))
                    throw new IOException("The candidate changed after review; review again");
                requireImmutableRevision(directory, after, hash);
                checkCancelled.run();
                if (!sha256(current).equals(originalHash)) throw new IOException("The installed pack changed during review; refresh and review again");
                candidate = pending(directory).resolve(before.id() + ".jar");
                Files.createDirectories(candidate.getParent());
                Files.move(staged, candidate); // Never replace another pending candidate.
                candidateMoved = true;
                writeChange(directory, new Change(1, "update", before.id(), originalHash, hash));
                published = true;
                return after;
            } finally {
                Files.deleteIfExists(staged);
                if (!published && candidateMoved) Files.deleteIfExists(candidate);
            }
        });
    }

    public static void stageRemoval(Path current, Path directory) throws Exception {
        stageRemoval(current, directory, null);
    }

    public static void stageRemoval(Path current, Path directory, String expectedCurrentHash) throws Exception {
        MetaPackResources.initialize();
        locked(directory, () -> {
            requireCurrent(current, directory);
            if (expectedCurrentHash != null && !sha256(current).equals(expectedCurrentHash))
                throw new IOException("The installed pack changed after review; refresh and review again");
            var manifest = manifest(current);
            writeChange(directory, new Change(1, "remove", manifest.id(), sha256(current), null));
            return null;
        });
    }

    public static void cancel(Path directory, String id) throws Exception {
        MetaPackResources.initialize();
        locked(directory, () -> {
            Path plan = plan(directory, id);
            // Cancellation removes only this ID's private pending files; malformed plans must be cancellable too.
            Files.deleteIfExists(plan);
            Files.deleteIfExists(pending(directory).resolve(id + ".jar"));
            return null;
        });
    }

    public static List<Path> history(Path directory, String id) throws IOException {
        checkId(id);
        Path history = directory.resolve(".history").resolve(id);
        if (!Files.exists(history)) return List.of();
        try (var files = Files.list(history)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList();
        }
    }

    /** Called once before session discovery. Failure leaves the plan and original/backup available for recovery. */
    static Map<String, String> activatePending(Path directory) {
        var problems = new LinkedHashMap<String, String>();
        if (!Files.exists(pending(directory))) return problems;
        try {
            locked(directory, () -> {
                try (var files = Files.list(pending(directory))) {
                    for (var plan : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                        try { activate(directory, plan); }
                        catch (Exception e) { problems.put(plan.toString(), e.toString()); }
                    }
                }
                return null;
            });
        } catch (Exception e) { problems.put(directory.toString(), e.toString()); }
        return problems;
    }

    private static void activate(Path directory, Path plan) throws Exception {
        Change change = readChange(plan);
        Path current = directory.resolve(change.id() + ".jar");
        Path candidate = pending(directory).resolve(change.id() + ".jar");
        Path backup = directory.resolve(".history").resolve(change.id()).resolve(change.expectedSha256() + ".jar");
        // Recover a completed activation whose plan cleanup was interrupted.
        if ((change.operation().equals("remove") && !Files.exists(current) && Files.exists(backup)
                && sha256(backup).equals(change.expectedSha256()))
                || (change.operation().equals("update") && Files.exists(current) && sha256(current).equals(change.candidateSha256())
                && Files.exists(backup) && sha256(backup).equals(change.expectedSha256()))) {
            Files.deleteIfExists(plan);
            Files.deleteIfExists(candidate);
            return;
        }
        if (!Files.exists(current) || !sha256(current).equals(change.expectedSha256()))
            throw new IOException("Installed pack changed since review; cancel the pending operation and review again");
        if (change.operation().equals("update")) {
            if (!Files.exists(candidate) || !sha256(candidate).equals(change.candidateSha256())
                    || !MetaPackArchive.open(candidate).id().equals(change.id()))
                throw new IOException("Pending update is missing or changed; the installed pack was preserved");
        }
        Files.createDirectories(backup.getParent());
        if (!Files.exists(backup) || !sha256(backup).equals(change.expectedSha256())) {
            Path stagedBackup = Files.createTempFile(backup.getParent(), ".backup-", ".tmp");
            try {
                Files.copy(current, stagedBackup, StandardCopyOption.REPLACE_EXISTING);
                if (!sha256(stagedBackup).equals(change.expectedSha256())) throw new IOException("Previous-revision backup failed integrity verification");
                Files.move(stagedBackup, backup, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(stagedBackup); }
        }
        if (!sha256(current).equals(change.expectedSha256())) throw new IOException("Installed pack changed during activation; review again");
        if (change.operation().equals("update") && !sha256(candidate).equals(change.candidateSha256()))
            throw new IOException("Pending update changed during activation; review again");
        if (change.operation().equals("update")) Files.move(candidate, current, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        else Files.delete(current);
        Files.delete(plan);
    }

    static void requireImmutableRevision(Path directory, MetaPackManifest candidate, String hash) throws Exception {
        for (var historic : history(directory, candidate.id())) {
            if (Objects.equals(manifest(historic).packVersion(), candidate.packVersion()) && !sha256(historic).equals(hash))
                throw new IOException("This packVersion already exists with different content; publish a new revision");
        }
    }

    private static void requireCurrent(Path current, Path directory) throws IOException {
        var archive = MetaPackArchive.open(current);
        if (!current.toAbsolutePath().normalize().equals(directory.toAbsolutePath().normalize().resolve(archive.id() + ".jar")))
            throw new IOException("Only externally installed packs managed by this directory can be changed");
    }
    private static Path pending(Path directory) { return directory.resolve(".pending"); }
    private static Path plan(Path directory, String id) throws IOException { checkId(id); return pending(directory).resolve(id + ".json"); }
    private static void checkId(String id) throws IOException {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) throw new IOException("Invalid pack ID");
    }
    private static Change readChange(Path file) throws IOException {
        Change change = JSON.readValue(file.toFile(), Change.class);
        checkId(change.id());
        if (change.format() != 1 || !Set.of("update", "remove").contains(change.operation())
                || !file.getFileName().toString().equals(change.id() + ".json")
                || change.expectedSha256() == null || !change.expectedSha256().matches("[a-f0-9]{64}")
                || (change.operation().equals("update") && (change.candidateSha256() == null || !change.candidateSha256().matches("[a-f0-9]{64}"))))
            throw new IOException("Invalid pending meta-pack operation");
        return change;
    }
    private static void writeChange(Path directory, Change change) throws IOException {
        Path destination = plan(directory, change.id());
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".plan-", ".tmp");
        try {
            Files.writeString(temporary, JSON.writeValueAsString(change));
            Files.move(temporary, destination);
        } finally { Files.deleteIfExists(temporary); }
    }
    static String sha256(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[32768];
            for (int size; (size = input.read(buffer)) >= 0;) digest.update(buffer, 0, size);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
