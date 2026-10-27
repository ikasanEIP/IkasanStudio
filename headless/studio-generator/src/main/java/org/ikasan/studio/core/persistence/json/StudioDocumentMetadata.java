package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import java.io.IOException;
import java.util.List;

/** Narrow compatibility repairs for recognised Studio document metadata, never runtime configuration. */
final class StudioDocumentMetadata {
    private StudioDocumentMetadata() { }

    static void normalize(ObjectNode studio) throws IOException {
        JsonNode propertiesNode = studio.get("properties");
        if (!(propertiesNode instanceof ObjectNode properties))
            throw new IOException("studio.properties must be an object. No model files have been changed.");
        for (String key : List.of("applicationPackageName", "flowStartupType", "h2DbPortNumber", "h2WebPortNumber", "useEmbeddedH2")) {
            if (!studio.has(key)) continue;
            move(studio, properties, key, "studio.properties");
        }
        if (studio.has("review")) {
            JsonNode imported = properties.get("ikasanRuntimeImport");
            if (imported != null && !(imported instanceof ObjectNode))
                throw new IOException("studio.properties.ikasanRuntimeImport must be an object. No model files have been changed.");
            ObjectNode provenance = imported == null ? properties.putObject("ikasanRuntimeImport") : (ObjectNode) imported;
            move(studio, provenance, "review", "studio.properties.ikasanRuntimeImport");
        }
        JsonNode pack = studio.get("metaPack");
        if (pack != null && pack.isTextual())
            studio.put("metaPack", resolvePackId(pack.asText(), ComponentLibrary.getMetapackList()));
    }

    static String resolvePackId(String requested, List<String> installed) throws IOException {
        if (installed.contains(requested)) return requested;
        var matches = installed.stream().filter(id -> id.equalsIgnoreCase(requested)).toList();
        if (matches.size() > 1)
            throw new IOException("Ambiguous meta-pack identifier; use the exact registered pack ID. No model files have been changed.");
        return matches.size() == 1 ? matches.get(0) : requested;
    }

    private static void move(ObjectNode source, ObjectNode target, String key, String targetPath) throws IOException {
        JsonNode value = source.get(key);
        if (target.has(key) && !target.get(key).equals(value))
            throw new IOException("Conflicting Studio metadata field " + key + " between studio and " + targetPath
                    + ". No model files have been changed.");
        target.set(key, value.deepCopy());
        source.remove(key);
    }
}
