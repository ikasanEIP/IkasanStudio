package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.generator.PropertiesTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class MetaPackPropertyAliasesTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Module module(String pack, boolean attachment) throws Exception {
        var flow = TestFixtures.getUnbuiltFlow(pack).build();
        var consumer = FlowElementFactory.createFlowElement(pack,
                ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "SFTP Consumer"), flow, flow.getFlowRoute(), "Input");
        consumer.setPropertyValue("knownHostsFilename", "/keys/input_known_hosts");
        flow.setConsumer(consumer);
        for (String variant : List.of("Email Producer", "SFTP Producer")) {
            FlowElement endpoint = FlowElementFactory.createFlowElement(pack,
                    ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant), flow, flow.getFlowRoute(), variant);
            if (variant.equals("Email Producer")) endpoint.setPropertyValue("hasAttachment", attachment);
            else {
                endpoint.setPropertyValue("knownHostsFilename", "/keys/output_known_hosts");
                endpoint.setPropertyValue("cleanUpChunks", false);
            }
            flow.getFlowRoute().getFlowElements().add(endpoint);
        }
        return TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
    }

    private void alias(ObjectNode node, String canonical, String old) {
        var value = node.remove(canonical);
        assertThat(value).isNotNull();
        node.set(old, value);
    }

    private void assertValuesAndGeneration(Module module, boolean attachment) throws Exception {
        var flow = module.getFlows().get(0);
        assertThat(flow.getConsumer().getPropertyValue("knownHostsFilename")).isEqualTo("/keys/input_known_hosts");
        assertThat(flow.getFlowRoute().getFlowElements().get(0).getPropertyValue("hasAttachment")).isEqualTo(attachment);
        assertThat(flow.getFlowRoute().getFlowElements().get(1).getPropertyValue("knownHostsFilename")).isEqualTo("/keys/output_known_hosts");
        assertThat(flow.getFlowRoute().getFlowElements().get(1).getPropertyValue("cleanUpChunks")).isEqualTo(false);
        String saved = ModelTemplate.create(module);
        assertThat(saved).doesNotContain("\"hasAttachments\"", "\"knownHostFilename\"", "\"cleanupChunks\"");
        String source = FlowsComponentFactoryTemplate.create("org.example", module, flow);
        assertThat(source).contains(".setHasAttachment(", ".setKnownHostsFilename(", ".setCleanUpChunks(");
        var properties = new Properties();
        properties.load(new StringReader(PropertiesTemplate.create(module)));
        assertThat(properties.stringPropertyNames()).anyMatch(key -> key.endsWith(".hasAttachments"));
        assertThat(properties.stringPropertyNames()).anyMatch(key -> key.endsWith(".cleanupChunks"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void historicalFlatModelsKeepTrueFalseAndSftpSettings(String pack) throws Exception {
        for (boolean attachment : List.of(false, true)) {
            ObjectNode legacy = (ObjectNode) JSON.readTree(ComponentIO.toJson(module(pack, attachment)));
            var flow = legacy.path("flows").get(0);
            alias((ObjectNode) flow.path("consumer"), "knownHostsFilename", "knownHostFilename");
            for (var element : flow.path("flowElements")) {
                if (element.has("hasAttachment")) alias((ObjectNode) element, "hasAttachment", "hasAttachments");
                if (element.has("cleanUpChunks")) alias((ObjectNode) element, "cleanUpChunks", "cleanupChunks");
                if (element.has("knownHostsFilename")) alias((ObjectNode) element, "knownHostsFilename", "knownHostFilename");
            }
            var restored = ComponentIO.validatePersistedModuleJson(legacy.toString(), "historical flat model", false);
            assertValuesAndGeneration(restored, attachment);
            assertValuesAndGeneration(ComponentIO.validatePersistedModuleJson(ModelTemplate.create(restored), "reload", false), attachment);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void historicalDocumentContainerPropertiesMigrateWithoutLosingValues(String pack) throws Exception {
        ObjectNode document = (ObjectNode) JSON.readTree(ModelTemplate.create(module(pack, true)));
        var flowSettings = document.path("studio").path("flows").elements().next().path("components");
        for (var record : document.path("configuration")) {
            var parameters = (com.fasterxml.jackson.databind.node.ArrayNode) record.path("parameters");
            for (int i = parameters.size() - 1; i >= 0; i--) {
                String name = parameters.get(i).path("name").asText();
                if (List.of("hasAttachment", "knownHostsFilename", "cleanUpChunks").contains(name)) parameters.remove(i);
            }
        }
        ((ObjectNode) flowSettings.path("Input").path("properties")).put("knownHostFilename", "/keys/input_known_hosts");
        ((ObjectNode) flowSettings.path("Email Producer").path("properties")).put("hasAttachments", true);
        ((ObjectNode) flowSettings.path("SFTP Producer").path("properties"))
                .put("knownHostFilename", "/keys/output_known_hosts").put("cleanupChunks", false);
        var restored = ComponentIO.validatePersistedModuleJson(document.toString(), "historical documents", false);
        assertValuesAndGeneration(restored, true);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void correctedNamesImportDirectlyFromRuntimeConfiguration(String pack) throws Exception {
        var topology = JSON.createObjectNode().put("name", "Imported").put("ikasanVersion", pack.substring(1));
        var flow = topology.putArray("flows").addObject().put("name", "Imported Flow").put("flowStartupType", "AUTOMATIC");
        var elements = flow.putArray("flowElements");
        var records = JSON.createArrayNode();
        for (String variant : List.of("SFTP Consumer", "SFTP Producer", "Email Producer")) {
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            var element = elements.addObject().put("componentName", variant).put("componentType", meta.getComponentType())
                    .put("implementingClass", meta.getImplementingClass()).put("configurationId", variant + "-config");
            if (meta.isConsumer()) flow.set("consumer", element.deepCopy());
            String configClass = meta.getRuntimeConfigurationClass();
            if (configClass == null) configClass = meta.getAllowableProperties().get("configuration").getUsageDataType();
            var parameters = records.addObject().put("configurationId", variant + "-config")
                    .put("implementingClass", configClass).putArray("parameters");
            if (variant.equals("Email Producer")) parameters.addObject().put("name", "hasAttachment").put("value", true);
            else parameters.addObject().put("name", "knownHostsFilename").put("value", "/keys/known_hosts");
            if (variant.equals("SFTP Producer")) parameters.addObject().put("name", "cleanUpChunks").put("value", false);
        }
        var edges = flow.putArray("transitions");
        edges.addObject().put("from", "SFTP Consumer").put("to", "SFTP Producer").put("name", "default");
        edges.addObject().put("from", "SFTP Producer").put("to", "Email Producer").put("name", "default");
        var imported = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(topology.toString(), records.toString(), pack, "org.example");
        assertThat(imported.review().toString()).doesNotContain("configuration fields not applied");
        var restored = ComponentIO.validatePersistedModuleJson(imported.studioJson(), "runtime corrected fields", false);
        var restoredFlow = restored.getFlows().get(0);
        assertThat(restoredFlow.getConsumer().getPropertyValue("knownHostsFilename")).isEqualTo("/keys/known_hosts");
        assertThat(restoredFlow.getFlowRoute().getFlowElements().get(0).getPropertyValue("cleanUpChunks")).isEqualTo(false);
        assertThat(restoredFlow.getFlowRoute().getFlowElements().get(1).getPropertyValue("hasAttachment")).isEqualTo(true);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void conflictingHistoricalAndCanonicalValuesAreRejected(String pack) throws Exception {
        ObjectNode legacy = (ObjectNode) JSON.readTree(ComponentIO.toJson(module(pack, true)));
        var email = (ObjectNode) legacy.path("flows").get(0).path("flowElements").get(0);
        email.put("hasAttachments", false);
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(legacy.toString(), "conflicting names", false))
                .hasMessageContaining("Conflicting legacy and canonical component properties");
    }
}
