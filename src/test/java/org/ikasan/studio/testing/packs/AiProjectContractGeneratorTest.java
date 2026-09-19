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

    @Test
    void agentsReceiveImplementationHelpAndExactRecipeTypesWithoutClaimingUnsupportedOperations() throws Exception {
        for (String version : PackExpectations.metaPacksToTest().toList()) {
            JsonNode catalogue = StudioJson.newObjectMapper().readTree(AiProjectContractGenerator.componentCatalogue(version));
            JsonNode converter = component(catalogue, "Converter");
            assertThat(converter.path("helpText").asText()).isNotBlank();
            assertThat(converter.path("documentation").asText()).isNotBlank();
            assertThat(converter.path("generatesUserImplementedClass").asBoolean()).isTrue();
            assertThat(converter.path("implementationGuidance").asText()).contains("stub", "preserving existing developer logic");
            assertThat(converter.path("proposalOperations").toString()).contains("addComponent", "replaceComponent");
            JsonNode className = null;
            for (JsonNode property : converter.path("properties")) {
                if (property.path("name").asText().equals("userImplementedClassName")) className = property;
            }
            assertThat(className).isNotNull();
            assertThat(className.has("default")).isFalse();
            assertThat(className.path("defaultExpression").asText()).isEqualTo("__fieldName:componentName");
            assertThat(className.path("setPropertySupported").asBoolean()).isFalse();
            assertThat(className.path("editGuidance").asText()).contains("unresolved", "setProperty");
            assertThat(component(catalogue, "FTP Producer").path("endpointKey").asText()).isEqualTo("FTP Endpoint");
            JsonNode fileRecipe = null;
            for (JsonNode recipe : converter.path("recipeConfigurations")) {
                if (recipe.path("conversionRecipeId").asText().equals("single-local-file-to-string")) fileRecipe = recipe;
            }
            assertThat(fileRecipe).isNotNull();
            assertThat(fileRecipe.path("fromType").asText()).isEqualTo("java.util.List<java.io.File>");
            assertThat(fileRecipe.path("toType").asText()).isEqualTo("java.lang.String");
            assertThat(component(catalogue, "Local File Consumer").path("producedOutputType").asText())
                    .isEqualTo(fileRecipe.path("fromType").asText());
            for (String name : java.util.List.of("Single Recipient Router", "Multi Recipient Router", "Exception Resolver")) {
                assertThat(component(catalogue, name).path("proposalOperations")).isEmpty();
                assertThat(component(catalogue, name).path("proposalSupport").asText()).contains("Configure in Studio");
            }
            assertThat(component(catalogue, "File Endpoint").path("proposalSupport").asText()).contains("owning component");
            JsonNode provider = null;
            for (JsonNode property : component(catalogue, "Event Generating Consumer").path("properties")) {
                if (property.path("name").asText().equals("endpointEventProvider")) provider = property;
            }
            assertThat(provider).isNotNull();
            assertThat(provider.path("helpText").asText()).isNotBlank();
            assertThat(provider.path("userSuppliedClass").asBoolean()).isTrue();
            assertThat(provider.path("protectFromOverwrite").asBoolean()).isTrue();
            assertThat(provider.path("affectsUserImplementedClass").asBoolean()).isTrue();
            assertThat(provider.path("noStubRequired").asBoolean()).isFalse();
        }
    }

    private static JsonNode component(JsonNode catalogue, String key) {
        for (JsonNode component : catalogue.path("components")) {
            if (component.path("key").asText().equals(key)) return component;
        }
        throw new AssertionError("Missing component: " + key);
    }

}
