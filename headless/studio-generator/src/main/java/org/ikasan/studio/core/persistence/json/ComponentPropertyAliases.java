package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.metapack.model.ComponentMeta;

import java.io.IOException;

/** Applies pack-declared historical property names without changing the supplied document. */
public final class ComponentPropertyAliases {
    private ComponentPropertyAliases() { }

    public static ObjectNode normalize(ObjectNode input, ComponentMeta meta) throws IOException {
        ObjectNode result = input.deepCopy();
        for (var alias : meta.getLegacyPropertyAliases().entrySet()) {
            if (!result.has(alias.getKey())) continue;
            var old = result.remove(alias.getKey());
            if (result.has(alias.getValue()) && !old.equals(result.get(alias.getValue())))
                throw new IOException("Conflicting legacy and canonical component properties: "
                        + alias.getKey() + " and " + alias.getValue() + ". No model files have been changed.");
            result.set(alias.getValue(), old);
        }
        return result;
    }
}
