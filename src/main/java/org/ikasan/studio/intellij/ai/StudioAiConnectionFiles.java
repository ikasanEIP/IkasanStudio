package org.ikasan.studio.intellij.ai;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Stable per-project discovery, with an exclusive owner and atomically rotated session data. */
final class StudioAiConnectionFiles implements AutoCloseable {
    private final Path directory;
    private final FileChannel channel;
    private final FileLock lock;

    static StudioAiConnectionFiles open(Path root, String projectPath, byte[] adapter) throws Exception {
        Path project = Path.of(projectPath).toAbsolutePath().normalize();
        if (Files.exists(project)) project = project.toRealPath();
        String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(project.toString().getBytes(StandardCharsets.UTF_8)));
        Path directory = root.resolve("ikasan-ai").resolve(key);
        Files.createDirectories(directory);
        restrict(directory, "rwx------");
        FileChannel channel = FileChannel.open(directory.resolve("owner.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try { lock = channel.tryLock(); }
        catch (OverlappingFileLockException busy) { channel.close(); throw new IOException("Studio AI is already connected for this project in another IDE session."); }
        catch (IOException failure) { channel.close(); throw failure; }
        if (lock == null) { channel.close(); throw new IOException("Studio AI is already connected for this project in another IDE session."); }
        StudioAiConnectionFiles files = new StudioAiConnectionFiles(directory, channel, lock);
        try {
            Path jar = files.adapter();
            // This adapter is stored in the local IDE system directory for a local client process.
            // Avoid replacing a JAR held open by an existing client on Windows.
            //noinspection UseOptimizedEelFunctions
            if (!Files.exists(jar) || !java.util.Arrays.equals(Files.readAllBytes(jar), adapter)) files.write(jar, adapter);
            return files;
        } catch (Exception failure) { files.close(); throw failure; }
    }

    private StudioAiConnectionFiles(Path directory, FileChannel channel, FileLock lock) {
        this.directory = directory; this.channel = channel; this.lock = lock;
    }
    Path adapter() { return directory.resolve("studio-mcp-adapter.jar"); }
    Path connection() { return directory.resolve("connection.json"); }
    void publish(String json) throws IOException { write(connection(), json.getBytes(StandardCharsets.UTF_8)); }
    private void write(Path destination, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(directory, "session-", ".tmp");
        try {
            restrict(temporary, "rw-------");
            Files.write(temporary, bytes);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static void restrict(Path path, String permissions) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
    }
    @Override public void close() throws IOException {
        try { Files.deleteIfExists(connection()); }
        finally { try { lock.release(); } finally { channel.close(); } }
    }
}
