package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.ModelMigration;
import org.ikasan.studio.core.persistence.json.IkasanModelDocuments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class IkasanModelDocumentsTest {
    private final ObjectMapper json = new ObjectMapper();

    private String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/org/ikasan/studio", name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"populated_module.json", "populated_module_with_router.json", "populated_module_with_exception_resolver.json",
            "populated_module_just_consumer_and_null_properties.json", "populated_module_with_empty_elements.json"})
    void convertsLegacyDesignWithoutChangingGeneratedApplication(String name) throws Exception {
        var original = ComponentIO.validatePersistedModuleJson(fixture(name), name, false);
        String saved = ModelTemplate.create(original);
        var docs = json.readTree(saved);
        assertThat(docs.path("modelFormat").asText()).isEqualTo(IkasanModelDocuments.FORMAT);
        assertThat(docs.path("module").has("applicationPackageName")).isFalse();
        assertThat(docs.path("studio").path("properties").has("flows")).isFalse();
        var loaded = ComponentIO.validatePersistedModuleJson(saved, name, false);
        assertThat(ModuleConfigTemplate.create(loaded)).isEqualTo(ModuleConfigTemplate.create(original));
        assertThat(PropertiesTemplate.create(loaded)).isEqualTo(PropertiesTemplate.create(original));
        for (int i = 0; i < original.getFlows().size(); i++) {
            var before = original.getFlows().get(i);
            var after = loaded.getFlows().get(i);
            assertThat(FlowTemplate.create("org.example", loaded, after)).isEqualTo(FlowTemplate.create("org.example", original, before));
            assertThat(FlowsComponentFactoryTemplate.create("org.example", loaded, after))
                    .isEqualTo(FlowsComponentFactoryTemplate.create("org.example", original, before));
        }
        assertThat(json.readTree(ModelTemplate.create(loaded))).isEqualTo(docs);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void inferredConfigurationIdsDoNotBecomeExplicitPropertiesOnReload(String pack) throws Exception {
        ObjectNode legacy = (ObjectNode) json.readTree(fixture("populated_module.json"));
        legacy.put("version", pack);
        var flow = (ObjectNode) legacy.path("flows").get(0);
        var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer");
        var consumer = flow.putObject("consumer").put("componentName", "Scheduled input")
                .put("componentType", meta.getComponentType()).put("implementingClass", meta.getImplementingClass())
                .put("cronExpression", "0/5 * * * * ?");
        if (meta.getAdditionalKey() != null) consumer.put("additionalKey", meta.getAdditionalKey());
        flow.remove(java.util.List.of("flowElements", "transitions"));
        var live = ComponentIO.validatePersistedModuleJson(legacy.toString(), "live", false);
        var saved = json.readTree(ModelTemplate.create(live));
        var reloaded = ComponentIO.validatePersistedModuleJson(saved.toString(), "saved", false);
        assertThat(json.readTree(ModelTemplate.create(reloaded))).isEqualTo(saved);
        assertThat(reloaded.getFlows().get(0).getConsumer().getPropertyValue("configurationId")).isNull();
        // An explicit choice must survive even when it happens to equal the automatic ID.
        String id = saved.at("/module/flows/0/flowElements/0/configurationId").asText();
        if (id.isEmpty()) id = saved.at("/module/flows/0/consumer/configurationId").asText();
        assertThat(id).isNotEmpty();
        consumer.put("configurationId", id);
        var explicit = ComponentIO.validatePersistedModuleJson(legacy.toString(), "explicit", false);
        var explicitSaved = json.readTree(ModelTemplate.create(explicit));
        var explicitReloaded = ComponentIO.validatePersistedModuleJson(explicitSaved.toString(), "explicit saved", false);
        assertThat(explicitReloaded.getFlows().get(0).getConsumer().getPropertyValue("configurationId")).isEqualTo(id);
        assertThat(json.readTree(ModelTemplate.create(explicitReloaded))).isEqualTo(explicitSaved);
    }

    @Test void bothMigrationDirectionsRetainTheDocumentFormat() throws Exception {
        String source = ModelTemplate.create(ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "test", false));
        var forward = ModelMigration.analyse(source, "V4.1.6");
        assertThat(forward.canApply()).as(forward.report()).isTrue();
        assertThat(json.readTree(forward.targetJson()).at("/studio/metaPack").asText()).isEqualTo("V4.1.6");
        assertThat(json.readTree(forward.targetJson()).at("/module/ikasanVersion").asText()).isEqualTo("4.1.6");
        var back = ModelMigration.analyse(forward.targetJson(), "V3.3.9");
        assertThat(back.canApply()).as(back.report()).isTrue();
        assertThat(json.readTree(ModelTemplate.create(ComponentIO.validatePersistedModuleJson(back.targetJson(), "test", false))))
                .isEqualTo(json.readTree(source));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void configurationIsAuthoritativeAndUnknownParametersSurvive(String pack) throws Exception {
        ObjectNode legacy = (ObjectNode) json.readTree(fixture("populated_module.json"));
        legacy.put("version", pack);
        var flow = (ObjectNode) legacy.path("flows").get(0);
        var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer");
        ObjectNode consumer = flow.putObject("consumer").put("componentName", "My Consumer")
                .put("componentType", meta.getComponentType()).put("implementingClass", meta.getImplementingClass())
                .put("configurationId", "poll-orders").put("cronExpression", "0/5 * * * * ?");
        if (meta.getAdditionalKey() != null) consumer.put("additionalKey", meta.getAdditionalKey());
        // A consumer-only design is valid while a flow is being edited.
        flow.remove(java.util.List.of("flowElements", "transitions"));
        ObjectNode docs = IkasanModelDocuments.fromLegacy(legacy);
        assertThat(docs.at("/module/flows/0/consumer").has("cronExpression")).isFalse();
        assertThat(docs.path("studio").path("flows").elements().next().path("components").path("My Consumer").path("properties").has("cronExpression")).isFalse();
        var parameters = (com.fasterxml.jackson.databind.node.ArrayNode) docs.path("configuration").get(0).path("parameters");
        ((ObjectNode) parameters.get(0)).put("value", "0/20 * * * * ?");
        parameters.addObject().put("name", "futureFlag").put("value", true);
        var loaded = ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false);
        assertThat(loaded.getFlows().get(0).getConsumer().getPropertyValue("cronExpression")).isEqualTo("0/20 * * * * ?");
        JsonNode saved = json.readTree(ModelTemplate.create(loaded));
        assertThat(saved.path("configuration").get(0).path("parameters").toString()).contains("futureFlag");
        ((ObjectNode) parameters.get(0)).put("value", false);
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false)).hasMessageContaining("value type");
    }

    @Test void nativeExtensionsFollowRenamesAndStayOutOfStudioProperties() throws Exception {
        ObjectNode docs = (ObjectNode) json.readTree(ModelTemplate.create(
                ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "test", false)));
        var flow = (ObjectNode) docs.path("module").path("flows").get(0);
        flow.putObject("futureRuntimeFlowSetting").put("enabled", true);
        var consumer = (ObjectNode) flow.path("consumer");
        consumer.put("futureRuntimeComponentSetting", "retained");
        ((ObjectNode) flow.path("flowElements").get(0)).put("futureRuntimeComponentSetting", "retained");
        var loaded = ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false);
        loaded.getFlows().get(0).setPropertyValue("name", "Renamed flow");
        loaded.getFlows().get(0).getConsumer().setPropertyValue("componentName", "Renamed input");
        JsonNode saved = json.readTree(ModelTemplate.create(loaded));
        assertThat(saved.at("/module/flows/0/futureRuntimeFlowSetting/enabled").asBoolean()).isTrue();
        assertThat(saved.at("/module/flows/0/consumer/futureRuntimeComponentSetting").asText()).isEqualTo("retained");
        assertThat(saved.path("studio").toString()).doesNotContain("_ikasanFlowExtensions", "_ikasanComponentExtensions");
        assertThat(ComponentIO.validatePersistedModuleJson(saved.toString(), "test", false).getFlows().get(0).getIdentity()).isEqualTo("Renamed flow");
    }

    @Test void rejectsUnknownFormatsAndMissingOrConflictingDocuments() throws Exception {
        ObjectNode docs = IkasanModelDocuments.fromLegacy(json.readTree(fixture("populated_module.json")));
        docs.put("formatVersion", 99);
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false)).hasMessageContaining("format");
        docs.put("formatVersion", 1);
        ((ObjectNode) docs.path("studio").path("properties")).put("name", "Conflicting name");
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false)).hasMessageContaining("Duplicated authoritative field");
        ((ObjectNode) docs.path("studio").path("properties")).remove("name");
        docs.remove("configuration");
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(docs.toString(), "test", false)).hasMessageContaining("configuration");
    }
}
