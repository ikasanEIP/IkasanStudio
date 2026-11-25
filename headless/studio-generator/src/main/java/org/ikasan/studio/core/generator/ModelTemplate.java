package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Template to create the JSON representation of the module
 */
public class ModelTemplate extends Generator {
    public static String create(final Module module) {
        try {
            ObjectMapper mapper = org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper();
            ObjectNode documents = org.ikasan.studio.core.persistence.json.IkasanModelDocuments.fromLegacy(
                    mapper.readTree(ComponentIO.toValidatedModuleJson(module)));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents) + "\n";
            ComponentIO.validatePersistedModuleJson(json, "model documents", false);
            return json;
        } catch (Exception e) {
            throw new org.ikasan.studio.StudioRuntimeException("Cannot save Ikasan model documents. The existing model is unchanged.", e);
        }
    }
}

