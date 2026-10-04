package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.StringCollectionValues;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class StringCollectionConfigurationTest {
    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6"})
    void preservesTypedCollectionsAndGeneratesSafeCode(String pack) throws Exception {
        var producer = FlowElement.flowElementBuilder().componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer"))
                .componentName("Mail").build();
        List<String> recipients = List.of("first@example.com", "comma,quote\"slash\\newline\n", "first@example.com", "");
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("mail.smtp.auth", "true");
        properties.put("odd.key", "value,\"\\\n${literal}");
        producer.setPropertyValue("toRecipients", recipients);
        producer.setPropertyValue("extendedMailSessionProperties", properties);
        producer.setPropertyValue("configurationId", "mail-config");
        var flow = TestFixtures.getUnbuiltFlow(pack).consumer(TestFixtures.getEventGeneratingConsumer(pack)).build();
        flow.getFlowRoute().getFlowElements().add(producer);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        var mapper = StudioJson.newObjectMapper();
        String saved = ModelTemplate.create(module);
        var document = mapper.readTree(saved);
        var parameters = document.path("configuration").findValue("parameters");
        assertThat(parameters.toString()).contains("ConfigurationParameterListImpl", "ConfigurationParameterMapImpl");
        var imported = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(
                document.path("module").toString(), document.path("configuration").toString(), pack, "org.example");
        var importedModule = ComponentIO.validatePersistedModuleJson(imported.studioJson(), "runtime import", false);
        var importedProducer = importedModule.getFlows().get(0).getFlowRoute().getFlowElements().get(0);
        assertThat(importedProducer.getPropertyValue("toRecipients")).isEqualTo(recipients);
        assertThat(importedProducer.getPropertyValue("extendedMailSessionProperties")).isEqualTo(properties);
        var loaded = ComponentIO.validatePersistedModuleJson(saved, "collections", false);
        var loadedProducer = loaded.getFlows().get(0).getFlowRoute().getFlowElements().get(0);
        assertThat(loadedProducer.getPropertyValue("toRecipients")).isEqualTo(recipients);
        assertThat(loadedProducer.getPropertyValue("extendedMailSessionProperties")).isEqualTo(properties);
        String generated = FlowsComponentFactoryTemplate.create("org.example", loaded, loaded.getFlows().get(0));
        assertThat(generated).contains(".setToRecipients(studioStringList(", ".setExtendedMailSessionProperties(new java.util.LinkedHashMap", "STRICT_DUPLICATE_DETECTION");
        var spring = new Properties();
        spring.load(new java.io.StringReader(PropertiesTemplate.create(loaded)));
        String key = spring.stringPropertyNames().stream().filter(k -> k.endsWith("toRecipients")).findFirst().orElseThrow();
        assertThat(mapper.readValue(spring.getProperty(key), List.class)).isEqualTo(recipients);
        // Empty means an explicit setter value, not absent/default.
        producer.setPropertyValue("toRecipients", List.of());
        producer.setPropertyValue("extendedMailSessionProperties", Map.of());
        assertThat(producer.getProperty("toRecipients").valueNotSet()).isFalse();
        assertThat(ModelTemplate.create(module)).contains("ConfigurationParameterListImpl");
    }

    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6"})
    void loadsLegacyStudioRecipientsAndSavesThemAsNativeConfiguration(String pack) throws Exception {
        var producer = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer"))
                .componentName("Mail").build();
        producer.setPropertyValue("configurationId", "mail-config");
        var flow = TestFixtures.getUnbuiltFlow(pack).consumer(TestFixtures.getEventGeneratingConsumer(pack)).build();
        flow.getFlowRoute().getFlowElements().add(producer);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        var mapper = StudioJson.newObjectMapper();
        var document = mapper.readTree(ModelTemplate.create(module));
        var settings = (com.fasterxml.jackson.databind.node.ObjectNode) document.path("studio")
                .path("flows").elements().next().path("components").path("Mail").path("properties");
        settings.put("toRecipients", "[first@example.com, second@example.com]");
        settings.put("extendedMailSessionProperties", "{\"mail.smtp.auth\":\"true\"}");
        var loaded = ComponentIO.validatePersistedModuleJson(document.toString(), "legacy collections", false);
        var restored = loaded.getFlows().get(0).getFlowRoute().getFlowElements().get(0);
        assertThat(restored.getPropertyValue("toRecipients")).isEqualTo(List.of("first@example.com", "second@example.com"));
        assertThat(restored.getPropertyValue("extendedMailSessionProperties")).isEqualTo(Map.of("mail.smtp.auth", "true"));
        var saved = mapper.readTree(ModelTemplate.create(loaded));
        assertThat(saved.path("studio").path("flows").elements().next().path("components")
                .path("Mail").path("properties").has("toRecipients")).isFalse();
        assertThat(saved.path("configuration").toString()).contains("ConfigurationParameterListImpl", "ConfigurationParameterMapImpl");
        assertThat(ComponentIO.validatePersistedModuleJson(saved.toString(), "round trip", false)
                .getFlows().get(0).getFlowRoute().getFlowElements().get(0).getPropertyValue("toRecipients"))
                .isEqualTo(restored.getPropertyValue("toRecipients"));
        // Ambiguous duplicate settings must still trigger recovery rather than lose user data.
        ((com.fasterxml.jackson.databind.node.ObjectNode) saved.path("studio").path("flows").elements().next()
                .path("components").path("Mail").path("properties")).put("toRecipients", "other@example.com");
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(saved.toString(), "conflict", false))
                .hasMessageContaining("Conflicting Studio and runtime configuration field toRecipients");
    }

    @Test void rejectsNonStringEntriesNestedObjectsAndDuplicateKeys() {
        assertThatThrownBy(() -> StringCollectionValues.normalize(List.class, List.of(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StringCollectionValues.normalize(Map.class, Map.of("k", Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StringCollectionValues.normalize(Map.class, "{\"k\":\"a\",\"k\":\"b\"}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StringCollectionValues.normalize(Map.class, "{} garbage")).isInstanceOf(IllegalArgumentException.class);
        assertThat(StringCollectionValues.normalize(List.class, "one,two,one")).isEqualTo(List.of("one", "two", "one"));
        assertThat(StringCollectionValues.normalize(List.class, "")).isNull();
        assertThat(StringCollectionValues.normalize(List.class, "[]")).isEqualTo(List.of());
    }
}
