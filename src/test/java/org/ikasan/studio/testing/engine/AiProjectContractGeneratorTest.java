package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.generator.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("engine")
class AiProjectContractGeneratorTest {
    @Test
    void archetypeDiscoveryInstructionsMatchGeneratedInstructions() throws Exception {
        Path discoveryFile = Path.of("ikasan-studio-ancillary", "ikasan-studio-project-archetype",
                "src/main/resources/archetype-resources/AGENTS.md");

        assertThat(Files.readString(discoveryFile)).isEqualTo(AiProjectContractGenerator.agentsGuide());
    }

    @Test
    void schemaAndGuidanceIdentifyTheProtectedSourceOfTruth() throws Exception {
        JsonNode schema = StudioJson.newObjectMapper().readTree(AiProjectContractGenerator.modelSchema());

        assertThat(schema.path("$defs").path("flow").isObject()).isTrue();
        assertThat(AiProjectContractGenerator.agentsGuide()).contains("model.json", "preserve unknown", "fields");
        assertThat(AiProjectContractGenerator.studioGuide("SyntheticGuide"))
                .contains("SyntheticGuide", "component-catalogue.json", "developer-owned");
    }
}
