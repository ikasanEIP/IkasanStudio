package org.ikasan.studio.core.metapack;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.persistence.json.IkasanModelDocuments;
import org.ikasan.studio.core.persistence.json.ProtectedModelFileWriter;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deliberate adoption changes only the document's revision lock; normal loading never rewrites it. */
public final class MetaPackRevisionLock {
    private MetaPackRevisionLock() { }
    public record Plan(String packId, String previousRevision, String availableRevision, String sourceJson, String targetJson) { }

    public static Plan preview(String source) throws Exception {
        var json = StudioJson.newObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        var parsed = json.readTree(source.startsWith("\uFEFF") ? source.substring(1) : source);
        if (!(parsed instanceof ObjectNode root) || !IkasanModelDocuments.isDocumentContainer(root)
                || !(root.get("studio") instanceof ObjectNode studio)) throw new IOException("A saved Studio model document is required");
        if (!studio.path("metaPack").isTextual() || !studio.path("metaPackRevision").isTextual())
            throw new IOException("The model must contain a pack ID and an existing revision lock");
        String id = studio.get("metaPack").asText();
        String previous = studio.get("metaPackRevision").asText();
        String available = ComponentLibrary.getMetaPackManifest(id).packVersion();
        if (available == null || Objects.equals(previous, available)) throw new IOException("The model already uses the available revision, or the pack has no revision metadata");
        studio.put("metaPackRevision", available);
        String target = json.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        ComponentIO.validatePersistedModuleJson(target, "meta-pack revision review", false);
        return new Plan(id, previous, available, source, target);
    }

    public static void apply(Path model, Plan reviewed) throws Exception {
        if (!preview(reviewed.sourceJson()).equals(reviewed)) throw new IOException("The revision review is stale; prepare it again");
        if (!Files.readString(model).equals(reviewed.sourceJson())) throw new IOException("The model changed after review; prepare the revision review again");
        ProtectedModelFileWriter.write(model, reviewed.targetJson(), text -> {
            if (!text.equals(reviewed.sourceJson()) && !text.equals(reviewed.targetJson()))
                throw new IOException("The model changed after review; revision adoption was cancelled");
            // The previous lock may be unavailable. Its exact reviewed contents are preserved as a backup;
            // every other model field has already been validated with the newly selected revision.
            ComponentIO.validatePersistedModuleJson(reviewed.targetJson(), "reviewed revision", false);
        });
    }
}
