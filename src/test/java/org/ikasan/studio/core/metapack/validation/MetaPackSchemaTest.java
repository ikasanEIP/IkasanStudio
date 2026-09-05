package org.ikasan.studio.core.metapack.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.io.ComponentIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaPackSchemaTest {
    @Test
    void publishedSchemasAreValidJsonSchemas() {
        for (String name : new String[]{"metapack", "component-type", "component"}) {
            JsonNode schema = assertDoesNotThrow(() -> ComponentIO.deserializeResource(
                    "studio/metapack/schema/" + name + ".schema.json", JsonNode.class));
            assertTrue(schema.hasNonNull("$schema"), name);
            assertTrue(schema.hasNonNull("$id"), name);
            assertTrue(schema.hasNonNull("type"), name);
        }
    }
}
