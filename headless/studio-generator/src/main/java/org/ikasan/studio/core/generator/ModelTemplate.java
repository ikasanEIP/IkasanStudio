package org.ikasan.studio.core.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.IkasanModelDocuments;
import org.ikasan.studio.core.persistence.json.StudioJson;

/**
 * Template to create the JSON representation of the module
 */
public class ModelTemplate extends Generator {
    public static String create(final Module module) {
        try {
            ObjectMapper mapper = StudioJson.newObjectMapper();
            ObjectNode documents = IkasanModelDocuments.fromLegacy(
                    mapper.readTree(ComponentIO.toValidatedModuleJson(module)));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents) + "\n";
            ComponentIO.validatePersistedModuleJson(json, "model documents", false);
            return json;
        } catch (Exception e) {
            throw new StudioRuntimeException("Cannot save Ikasan model documents. The existing model is unchanged.", e);
        }
    }
}

