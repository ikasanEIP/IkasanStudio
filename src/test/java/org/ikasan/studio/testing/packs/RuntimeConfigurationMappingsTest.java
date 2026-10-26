package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.generator.PropertiesTemplate;
import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.validation.MetaPackValidator;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class RuntimeConfigurationMappingsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> EMAIL = Map.of(
            "mailHost", "smtp.import.example", "password", "fixture-mail-secret", "runtimeEnvironment", "import-test",
            "subject", "Imported subject", "mailFrom", "sender@example.test", "mailTransportProtocol", "smtp");
    private record Fixture(ObjectNode module, ArrayNode configuration) { }

    private Fixture fixture(String pack) throws Exception {
        var module = JSON.createObjectNode().put("name", "Imported").put("ikasanVersion", pack.substring(1)).put("port", 12450);
        var flow = module.putArray("flows").addObject().put("name", "Imported Flow").put("flowStartupType", "AUTOMATIC");
        var elements = flow.putArray("flowElements");
        var records = JSON.createArrayNode();
        for (String variant : List.of("FTP Consumer", "FTP Producer", "Email Producer")) {
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            var element = elements.addObject().put("componentName", variant).put("componentType", meta.getComponentType())
                    .put("implementingClass", meta.getImplementingClass()).put("configurationId", variant + "-config");
            if (meta.isConsumer()) flow.set("consumer", element.deepCopy());
            String type = meta.getRuntimeConfigurationClass();
            if (type == null) type = meta.getAllowableProperties().get("configuration").getUsageDataType();
            var parameters = records.addObject().put("configurationId", variant + "-config").put("implementingClass", type).putArray("parameters");
            if (variant.equals("Email Producer")) {
                EMAIL.forEach((name, value) -> parameters.addObject().put("name", name).put("value", value));
                parameters.addObject().put("name", "hasAttachment").put("value", true);
            } else parameters.addObject().put("name", "FTPS").put("value", variant.equals("FTP Producer"));
        }
        var edges = flow.putArray("transitions");
        edges.addObject().put("from", "FTP Consumer").put("to", "FTP Producer").put("name", "default");
        edges.addObject().put("from", "FTP Producer").put("to", "Email Producer").put("name", "default");
        return new Fixture(module, records);
    }

    private Module imported(String pack, Fixture fixture) throws Exception {
        var result = IkasanRuntimeImport.convert(fixture.module().toString(), fixture.configuration().toString(), pack, "org.example");
        assertThat(result.review().toString()).doesNotContain("configuration fields not applied", "fixture-mail-secret");
        return ComponentIO.validatePersistedModuleJson(result.studioJson(), "runtime names", false);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void runtimeNamesImportSaveReloadAndExportAsTheOriginalApiNames(String pack) throws Exception {
        var module = imported(pack, fixture(pack));
        for (int pass = 0; pass < 2; pass++) {
            var flow = module.getFlows().get(0);
            assertThat(flow.getConsumer().getPropertyValue("ftps")).isEqualTo(false);
            assertThat(flow.getFlowRoute().getFlowElements().get(0).getPropertyValue("ftps")).isEqualTo(true);
            var email = flow.getFlowRoute().getFlowElements().get(1);
            var meta = email.getComponentMeta();
            var properties = new Properties();
            properties.load(new StringReader(PropertiesTemplate.create(module)));
            for (var entry : EMAIL.entrySet()) {
                String property = meta.runtimeConfigurationProperty(entry.getKey());
                assertThat(email.getPropertyValue(property)).isEqualTo(entry.getValue());
                String key = StudioBuildUtils.substitutePlaceholderInLowerCase(module, flow, email,
                        email.getProperty(property).getMeta().getPropertyConfigFileLabel());
                assertThat(properties.getProperty(key)).isEqualTo(entry.getValue());
            }
            String generated = FlowsComponentFactoryTemplate.create("org.example", module, flow);
            assertThat(generated).contains(".setMailhost(", ".setPassword(", ".setRuntimeEnvironment(",
                    ".setMailSubject(", ".setFrom(", ".setTransportProtocol(", ".setFTPS(");
            String saved = ModelTemplate.create(module);
            var document = JSON.readTree(saved);
            for (var record : document.path("configuration")) {
                var parameters = record.path("parameters");
                var names = new java.util.ArrayList<String>();
                parameters.forEach(p -> names.add(p.path("name").asText()));
                if (record.path("configurationId").asText().equals("Email Producer-config")) {
                    assertThat(names).containsAll(EMAIL.keySet()).doesNotContain("mailhost", "mailPassword", "mailRuntimeEnvironment", "mailSubject", "from", "transportProtocol");
                } else assertThat(names).contains("FTPS").doesNotContain("ftps");
            }
            module = ComponentIO.validatePersistedModuleJson(saved, "runtime names reload", false);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void previousStudioContainerPropertiesMoveToRuntimeRecordsWithoutValueLoss(String pack) throws Exception {
        var module = imported(pack, fixture(pack));
        ObjectNode document = (ObjectNode) JSON.readTree(ModelTemplate.create(module));
        var settings = document.path("studio").path("flows").path("Imported Flow").path("components");
        for (var record : document.path("configuration")) {
            String id = record.path("configurationId").asText();
            String variant = id.substring(0, id.length() - "-config".length());
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            var properties = (ObjectNode) settings.path(variant).path("properties");
            var parameters = (ArrayNode) record.path("parameters");
            for (int i = parameters.size() - 1; i >= 0; i--) {
                JsonNode parameter = parameters.get(i);
                String property = meta.getRuntimeConfigurationMappings().get(parameter.path("name").asText());
                if (property != null) {
                    properties.set(property, parameter.get("value").deepCopy());
                    parameters.remove(i);
                }
            }
        }
        var restored = ComponentIO.validatePersistedModuleJson(document.toString(), "prior Studio settings", false);
        assertThat(PropertiesTemplate.create(restored)).isEqualTo(PropertiesTemplate.create(module));
        // A second authority for the same property must not silently win, even with equal values.
        var emailRecord = (ObjectNode) document.path("configuration").get(2);
        ((ArrayNode) emailRecord.path("parameters")).addObject().put("name", "mailHost").put("value", "other.example");
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(document.toString(), "conflicting mapping", false))
                .hasMessageContaining("Conflicting Studio and runtime configuration field mailhost");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void wrongClassWrongTypeAndUnknownSpellingAreNotGuessed(String pack) throws Exception {
        var fixture = fixture(pack);
        var email = (ObjectNode) fixture.configuration().get(2);
        var parameters = (ArrayNode) email.path("parameters");
        parameters.addObject().put("name", "mailSmtHost").put("value", "never-guess.example");
        for (var parameter : parameters) {
            if (parameter.path("name").asText().equals("mailHost")) ((ObjectNode) parameter).put("value", 42);
        }
        var result = IkasanRuntimeImport.convert(fixture.module().toString(), fixture.configuration().toString(), pack, "org.example");
        assertThat(result.review().toString()).contains("mailHost", "mailSmtHost").doesNotContain("fixture-mail-secret");
        var restored = ComponentIO.validatePersistedModuleJson(result.studioJson(), "wrong type", false);
        assertThat(restored.getFlows().get(0).getFlowRoute().getFlowElements().get(1).getPropertyValue("mailhost")).isNotEqualTo(42);
        email.put("implementingClass", "example.OtherConfiguration");
        var mismatch = IkasanRuntimeImport.convert(fixture.module().toString(), fixture.configuration().toString(), pack, "org.example");
        assertThat(mismatch.review().toString()).contains("password", "subject", "mailTransportProtocol");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void packValidationRejectsRedundantUnknownAmbiguousAndShadowingMappings(String pack) throws Exception {
        var original = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer");
        for (var invalid : List.of(Map.of("mailhost", "mailhost"), Map.of("mailHost", "missingProperty"),
                Map.of("mailHost", "mailhost", "otherHost", "mailhost"), Map.of("mailSmtpHost", "mailhost"))) {
            var components = new LinkedHashMap<>(ComponentLibrary.getIkasanComponents(pack));
            components.put("Email Producer", original.toBuilder().runtimeConfigurationMappings(invalid).build());
            assertThatThrownBy(() -> MetaPackValidator.validate(pack, ComponentLibrary.getMetaPackManifest(pack), components))
                    .hasMessageContaining("runtimeConfigurationMappings");
        }
    }
}
