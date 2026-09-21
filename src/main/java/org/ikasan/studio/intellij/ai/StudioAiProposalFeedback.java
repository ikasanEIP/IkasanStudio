package org.ikasan.studio.intellij.ai;

import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

/** Ordered, atomic sidecar receipts. Never writes the proposal or the Studio model. */
final class StudioAiProposalFeedback {
    private final Path source;
    private final String hash;
    private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
    StudioAiProposalFeedback(Path source, String json) {
        this.source = source.toAbsolutePath().normalize();
        this.hash = sha256(json.getBytes(StandardCharsets.UTF_8));
    }
    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static Path resultPath(Path source) { return source.resolveSibling(source.getFileName() + ".result.json"); }
    synchronized void publish(String status, String message) {
        writes = writes.handle((unused, failure) -> null).thenRunAsync(() -> {
            try { write(status, message); }
            catch (Exception failure) {
                com.intellij.openapi.diagnostic.Logger.getInstance(StudioAiProposalFeedback.class)
                        .warn("Could not write AI proposal result: " + resultPath(source), failure);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    void write(String status, String message) throws Exception {
        // A later proposal may reuse the filename while an earlier generation finishes.
        byte[] current;
        try (var input = Files.newInputStream(source)) { current = input.readNBytes(1_048_577); }
        if (!hash.equals(sha256(current))) return;
        var data = new LinkedHashMap<String, Object>();
        data.put("formatVersion", 1);
        data.put("proposalFile", source.getFileName().toString());
        data.put("proposalSha256", hash);
        data.put("status", status);
        data.put("updatedAt", java.time.Instant.now().toString());
        data.put("message", message == null ? "" : message);
        data.put("nextStep", switch (status) {
            case "applied" -> "Read the updated model and generated files, then compile and test. Do not replay this proposal.";
            case "awaiting_review" -> "Ask the developer to review in Studio; do not submit replacement proposals while review is pending.";
            case "generating" -> "Wait for a terminal result before compiling or submitting another proposal.";
            case "generation_failed" -> "The model may have changed. Read current model and generation diagnostics; do not blindly replay.";
            case "cancelled", "undone" -> "Do not retry automatically. Respect the developer decision.";
            default -> "Read the error, current model and catalogue. Correct recoverable problems and use a fresh baseModelSha256. Retry at most twice; then ask for help. Never bypass Studio with model.json edits.";
        });
        Path temp = Files.createTempFile(source.getParent(), ".studio-result-", ".tmp");
        try {
            Files.writeString(temp, StudioJson.newObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(data));
            try { Files.move(temp, resultPath(source), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, resultPath(source), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
