package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("packs")
class AiProjectContractGeneratorTest {
    @Test
    void catalogueIsDerivedFromEveryShippedMetapack() throws Exception {
        for (String version : PackExpectations.metaPacksToTest().toList()) {
            JsonNode catalogue = StudioJson.newObjectMapper()
                    .readTree(AiProjectContractGenerator.componentCatalogue(version));

            assertThat(catalogue.path("metapackVersion").asText()).isEqualTo(version);
            assertThat(catalogue.path("components")).hasSize(ComponentLibrary.getNumberOfComponents(version));
            assertThat(catalogue.path("components").findValuesAsText("key"))
                    .contains("Module", "Flow", "Spring JMS Producer", "Spring JMS Consumer");
        }
    }

}
