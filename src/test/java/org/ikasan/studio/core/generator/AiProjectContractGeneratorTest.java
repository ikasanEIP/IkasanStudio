package org.ikasan.studio.core.generator;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SharedResourceExtension.class)
class AiProjectContractGeneratorTest {
    @Test
    void archetypeDiscoveryInstructionsMatchGeneratedInstructions() throws Exception {
        Path discoveryFile = Path.of("ikasan-studio-ancillary", "ikasan-studio-project-archetype",
                "src/main/resources/archetype-resources/AGENTS.md");

        assertThat(Files.readString(discoveryFile)).isEqualTo(AiProjectContractGenerator.agentsGuide());
    }

    @Test
    void catalogueIsDerivedFromEveryShippedMetapack() throws Exception {
        for (String version : ComponentLibrary.getMetapackList()) {
            JsonNode catalogue = StudioJson.newObjectMapper()
                    .readTree(AiProjectContractGenerator.componentCatalogue(version));

            assertThat(catalogue.path("metapackVersion").asText()).isEqualTo(version);
            assertThat(catalogue.path("components")).hasSize(ComponentLibrary.getNumberOfComponents(version));
            assertThat(catalogue.path("components").findValuesAsText("key"))
                    .contains("Module", "Flow", "Spring JMS Producer", "Spring JMS Consumer");
        }
    }

    @Test
    void schemaAndGuidanceIdentifyTheProtectedSourceOfTruth() throws Exception {
        JsonNode schema = StudioJson.newObjectMapper().readTree(AiProjectContractGenerator.modelSchema());

        assertThat(schema.path("$defs").path("flow").isObject()).isTrue();
        assertThat(AiProjectContractGenerator.agentsGuide()).contains("model.json", "preserve unknown", "fields");
        assertThat(AiProjectContractGenerator.studioGuide("V4.1.6"))
                .contains("V4.1.6", "component-catalogue.json", "developer-owned");
    }
}
