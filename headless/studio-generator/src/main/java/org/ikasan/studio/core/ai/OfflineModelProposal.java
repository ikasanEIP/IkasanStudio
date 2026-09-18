package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/** File exchange contract: no transport or IDE dependency, and no model writes. */
public final class OfflineModelProposal {
    private OfflineModelProposal() { }

    public static ModelProposal.Prepared prepare(String proposalJson, byte[] savedModel,
                                                 Map<String, Object> liveModel) throws Exception {
        if (proposalJson.length() > 1_048_576) throw new IllegalArgumentException("Proposal exceeds 1 MiB.");
        var json = StudioJson.newObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        var proposal = json.readTree(proposalJson);
        if (proposal == null || !proposal.isObject()) throw new IllegalArgumentException("Expected a Studio proposal JSON object.");
        var allowed = Set.of("formatVersion", "baseModelSha256", "operations");
        proposal.fieldNames().forEachRemaining(key -> {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown proposal field: " + key);
        });
        if (!proposal.path("formatVersion").isIntegralNumber() || proposal.path("formatVersion").intValue() != 1)
            throw new IllegalArgumentException("Expected proposal formatVersion 1.");
        String digest = proposal.path("baseModelSha256").asText();
        if (!digest.matches("[a-fA-F0-9]{64}") || !digest.equalsIgnoreCase(sha256(savedModel)))
            throw new IllegalArgumentException("The saved model has changed. Ask the AI to read it again and create a fresh proposal.");
        var persisted = ComponentIO.validatePersistedModuleJson(new String(savedModel, StandardCharsets.UTF_8), "Proposal base model", false);
        // Compare JSON values: persisted parsing may use a different Java numeric wrapper
        // (for example Integer versus Long) for an otherwise identical property.
        var savedSnapshot = json.readTree(json.writeValueAsString(LiveModelSnapshot.capture(persisted)));
        var currentSnapshot = json.readTree(json.writeValueAsString(liveModel));
        if (!savedSnapshot.equals(currentSnapshot))
            throw new IllegalArgumentException("Studio has changes that are not in the saved model. Save through Studio, then ask the AI for a fresh proposal.");
        return ModelProposal.prepare(liveModel, proposal.path("operations"));
    }

    public static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
