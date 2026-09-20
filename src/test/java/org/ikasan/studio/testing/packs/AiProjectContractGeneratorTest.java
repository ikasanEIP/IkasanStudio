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
            String release = ComponentLibrary.getMetaPackManifest(version).ikasanVersion();
            assertThat(catalogue.path("ikasanVersion").asText()).isEqualTo(release);
            var reference = catalogue.path("frameworkReference");
            assertThat(reference.path("releaseTag").asText()).isEqualTo("ikasaneip-" + release);
            assertThat(reference.path("releaseSource").asText()).isEqualTo("https://github.com/ikasanEIP/ikasan/tree/ikasaneip-" + release);
            assertThat(reference.path("navigation").path("interfaces").get(0).asText()).isEqualTo("ikasaneip/spec");
            assertThat(reference.path("offlineFallback").asText()).contains("resolved dependency version");

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
            assertThat(component(catalogue, "Splitter").path("completionChecks").toString()).contains("empty list", "intended order");
            assertThat(component(catalogue, "Translator").path("completionChecks").toString()).contains("in-place mutation");
            assertThat(component(catalogue, "Message Filter").path("completionChecks").toString()).contains("accepted and rejected");
            assertThat(component(catalogue, "Broker").path("completionChecks").toString()).contains("enrichment");
            assertThat(component(catalogue, "Generic Producer").path("completionChecks").toString()).contains("empty invoke");
            assertThat(component(catalogue, "Generic Consumer").path("completionChecks").toString()).contains("start/stop lifecycle");
            assertThat(converter.path("completionChecks").toString()).contains("UnsupportedOperationException");
            JsonNode fileRecipe = null;
            for (JsonNode recipe : converter.path("recipeConfigurations")) {
                if (recipe.path("conversionRecipeId").asText().equals("single-local-file-to-string")) fileRecipe = recipe;
            }
            assertThat(fileRecipe).isNotNull();
            assertThat(fileRecipe.path("fromType").asText()).isEqualTo("java.util.List<java.io.File>");
            assertThat(fileRecipe.path("toType").asText()).isEqualTo("java.lang.String");
            assertThat(component(catalogue, "Local File Consumer").path("producedOutputType").asText())
                    .isEqualTo(fileRecipe.path("fromType").asText());
            for (String name : java.util.List.of("Single Recipient Router", "Multi Recipient Router")) {
                assertThat(component(catalogue, name).path("proposalOperations").toString()).contains("addComponent", "configureRoutes");
            }
            assertThat(component(catalogue, "Flow").path("proposalOperations").toString()).contains("setFlowProperty");
            assertThat(component(catalogue, "Flow").path("completionChecks").toString()).contains("Run module", "MANUAL");
            var resolver = component(catalogue, "Exception Resolver");
            assertThat(resolver.path("proposalOperations").toString()).contains("setExceptionResolution");
            assertThat(resolver.path("exceptionActions").toString()).contains("excludeEvent", "retry", "maximum retry count");
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
            assertThat(provider.path("beanRequirement").asText()).contains("Spring registration", "Do not assume", "user/");
            boolean externalBeanChecked = false;
            for (JsonNode entry : catalogue.path("components")) {
                for (JsonNode property : entry.path("properties")) {
                    if (property.path("userSuppliedClass").asBoolean() && property.path("noStubRequired").asBoolean()) {
                        assertThat(property.path("beanRequirement").asText()).contains("existing configured bean", "No stub");
                        externalBeanChecked = true;
                    }
                }
            }
            assertThat(externalBeanChecked).isTrue();
        }
    }

    @Test
    void emailAddressRulesAcceptLongDomainSuffixesAndRejectMalformedAddresses() throws Exception {
        for (String version : PackExpectations.metaPacksToTest().toList()) {
            JsonNode catalogue = StudioJson.newObjectMapper().readTree(AiProjectContractGenerator.componentCatalogue(version));
            java.util.Set<String> checked = new java.util.HashSet<>();
            for (JsonNode property : component(catalogue, "Email Producer").path("properties")) {
                String name = property.path("name").asText();
                if (!java.util.Set.of("toRecipient", "from", "ccRecipient", "bccRecipient").contains(name)) continue;
                var rule = java.util.regex.Pattern.compile(property.path("validation").asText());
                for (String valid : java.util.List.of("audience@example.invalid", "studio@example.test",
                        "developer@example.technology", "first.last+demo@example.com")) {
                    assertThat(rule.matcher(valid).matches()).as("%s %s accepts %s", version, name, valid).isTrue();
                }
                for (String invalid : java.util.List.of("audience", "@example.invalid", "audience@",
                        "audience example@test.com", "audience@example.invalid,other@example.com")) {
                    assertThat(rule.matcher(invalid).matches()).as("%s %s rejects %s", version, name, invalid).isFalse();
                }
                checked.add(name);
            }
            assertThat(checked).containsExactlyInAnyOrder("toRecipient", "from", "ccRecipient", "bccRecipient");
        }
    }

    private static JsonNode component(JsonNode catalogue, String key) {
        for (JsonNode component : catalogue.path("components")) {
            if (component.path("key").asText().equals(key)) return component;
        }
        throw new AssertionError("Missing component: " + key);
    }

}
