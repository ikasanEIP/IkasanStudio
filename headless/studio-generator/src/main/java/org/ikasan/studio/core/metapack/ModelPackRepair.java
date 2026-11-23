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

/** Explicit, validated model-only recovery. Normal loading never changes pack identities or revision locks. */
public final class ModelPackRepair {
    private ModelPackRepair() { }

    public record Plan(String previousPack, String targetPack, String previousRevision, String targetRevision,
                       String previousRuntime, String targetRuntime, String javaVersion,
                       String sourceJson, String targetJson) { }

    public static Plan preview(String source) throws Exception {
        var mapper = StudioJson.newObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        var parsed = mapper.readTree(source.startsWith("\uFEFF") ? source.substring(1) : source);
        if (!(parsed instanceof ObjectNode root) || !IkasanModelDocuments.isDocumentContainer(root)
                || !(root.get("studio") instanceof ObjectNode studio)
                || !(root.get("module") instanceof ObjectNode module))
            throw new IOException("A saved Studio model document is required. Restore a validated backup for damaged or legacy files.");
        String previousPack = text(studio, "metaPack");
        String previousRuntime = text(module, "ikasanVersion");
        String previousRevision = studio.has("metaPackRevision") ? text(studio, "metaPackRevision") : "";
        // This is a deliberately bounded retirement path, not a general runtime migration heuristic.
        boolean retiredSnapshot = previousPack.equals("V5.0.1-SNAPSHOT") && previousRuntime.equals("5.0.1-SNAPSHOT");
        String targetPack = retiredSnapshot ? "V5.1.0" : previousPack;
        var manifest = ComponentLibrary.getMetaPackManifest(targetPack);
        if (!retiredSnapshot && !Objects.equals(manifest.ikasanVersion(), previousRuntime))
            throw new IOException("Runtime and pack identity disagree. Review the original project before choosing a runtime; no repair was applied.");
        if (manifest.packVersion() == null || manifest.packVersion().isBlank())
            throw new IOException("The installed pack does not supply a revision for a validated repair.");
        if (!retiredSnapshot && previousRevision.equals(manifest.packVersion()))
            throw new IOException("The model already selects the installed pack revision. If you have just applied a repair and Studio still shows the previous error, close and reopen the project to reload the repaired model. If loading still fails, restore a validated backup or correct the reported model error.");
        studio.put("metaPack", targetPack);
        studio.put("metaPackRevision", manifest.packVersion());
        module.put("ikasanVersion", manifest.ikasanVersion());
        String target = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        ComponentIO.validatePersistedModuleJson(target, "model repair preview", false);
        return new Plan(previousPack, targetPack, previousRevision, manifest.packVersion(), previousRuntime,
                manifest.ikasanVersion(), manifest.javaVersion(), source, target);
    }

    public static void apply(Path model, Plan reviewed) throws Exception {
        if (!preview(reviewed.sourceJson()).equals(reviewed))
            throw new IOException("The repair review changed; prepare it again.");
        if (!Files.readString(model).equals(reviewed.sourceJson()))
            throw new IOException("The model changed after review; prepare the repair again.");
        ProtectedModelFileWriter.write(model, reviewed.targetJson(), text -> {
            if (!text.equals(reviewed.sourceJson()) && !text.equals(reviewed.targetJson()))
                throw new IOException("The model changed after review; repair was cancelled.");
            // Preserve the exact reviewed original even though its old pack cannot load.
            ComponentIO.validatePersistedModuleJson(reviewed.targetJson(), "reviewed model repair", false);
        });
    }

    private static String text(ObjectNode object, String field) throws IOException {
        if (!object.path(field).isTextual() || object.get(field).asText().isBlank())
            throw new IOException("Missing or invalid model field: " + field);
        return object.get(field).asText();
    }
}
