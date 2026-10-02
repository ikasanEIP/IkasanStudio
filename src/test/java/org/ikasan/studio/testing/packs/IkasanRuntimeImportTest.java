package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@Tag("packs")
class IkasanRuntimeImportTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ObjectNode topology(String pack) throws Exception {
        ObjectNode module = JSON.createObjectNode().put("name", "Imported Orders")
                .put("version", "application-2.7").put("ikasanVersion", pack.substring(1));
        ObjectNode flow = module.putArray("flows").addObject().put("name", "Orders");
        flow.put("flowStartupType", "AUTOMATIC");
        var consumer = component(pack, "Scheduled Consumer", "Poll");
        consumer.put("configurationId", "poll-config");
        flow.set("consumer", consumer);
        flow.putArray("flowElements").add(component(pack, "Logging Producer", "Log")).add(consumer.deepCopy());
        flow.putArray("transitions").addObject().put("from", "Poll").put("to", "Log").put("name", "default");
        return module;
    }

    private ObjectNode component(String pack, String key, String name) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, key);
        return JSON.createObjectNode().put("componentName", name).put("componentType", meta.getComponentType())
                .put("implementingClass", meta.getImplementingClass()).put("configurable", false);
    }

    private ObjectNode configuration() throws Exception {
        return (ObjectNode) JSON.readTree("""
                {"configurationId":"poll-config",
                 "implementingClass":"org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumerConfiguration",
                 "parameters":[{"name":"cronExpression","value":"0/10 * * * * ?"},
                               {"name":"customSecret","value":"do-not-show-in-report"}]}
                """);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void importsSeparateContractsAndPreservesOriginalsWithoutTreatingApplicationVersionAsPack(String pack) throws Exception {
        var input = topology(pack);
        var config = configuration();
        var result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example.imported", mappings());
        var module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false);
        assertThat(module.getVersion()).isEqualTo(pack);
        assertThat(module.getFlows()).hasSize(1);
        assertThat(module.getFlows().get(0).getConsumer().getPropertyValue("cronExpression")).isEqualTo("0/10 * * * * ?");
        assertThat(module.getFlows().get(0).getPropertyValue("flowStartupType")).isEqualTo("MANUAL");
        var saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).get("module")).isEqualTo(input);
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).get("configuration")).isEqualTo(config);
        assertThat(result.review().toString()).contains("customSecret", "without applying").doesNotContain("do-not-show-in-report");
    }

    @Test void rejectsMismatchedPack() {
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(), "", "V4.1.6", "org.example"))
                .hasMessageContaining("matching meta-pack");
    }

    @Test void rejectsDanglingEdgesCyclesAndDisconnectedNodes() throws Exception {
        var tree = topology("V3.3.9");
        var flow = (ObjectNode) tree.path("flows").get(0);
        ((ObjectNode) flow.path("transitions").get(0)).put("to", "Missing");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("missing component");
        ((ObjectNode) flow.path("transitions").get(0)).put("to", "Poll");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("cycles");
        flow.putArray("transitions");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("disconnected");
    }

    @Test void rejectsConflictingConsumerDefinition() throws Exception {
        var tree = topology("V3.3.9");
        ((ObjectNode) tree.path("flows").get(0).path("flowElements").get(1)).put("implementingClass", "other.Consumer");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("duplicate component");
    }

    @Test void requiresExplicitChoiceForSharedRuntimeClassEvenWithConfiguration() throws Exception {
        assertThat(IkasanRuntimeImport.mappingChoices(topology("V3.3.9").toString(), "V3.3.9"))
                .singleElement().satisfies(choice -> assertThat(choice.variants()).contains("Scheduled Consumer", "Local File Consumer"));
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(), configuration().toString(), "V3.3.9", "org.example"))
                .hasMessageContaining("select a variant explicitly");
    }

    @Test void rejectsDuplicateConfigurationsAndMalformedParameters() throws Exception {
        var config = configuration();
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(),
                "[" + config + "," + config + "]", "V3.3.9", "org.example")).hasMessageContaining("Duplicate configurationId");
        config.remove("parameters");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(), config.toString(),
                "V3.3.9", "org.example")).hasMessageContaining("parameters must be an array");
    }

    @Test void preservesCustomConverterReferenceAndRequestsNoStub() throws Exception {
        var tree = topology("V3.3.9");
        var producer = (ObjectNode) tree.path("flows").get(0).path("flowElements").get(0);
        producer.put("implementingClass", "com.acme.OrderProducer");
        var result = convert(tree);
        var element = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
        assertThat(element.path("userImplementedClassName").asText()).isEqualTo("com.acme.OrderProducer");
        assertThat(element.path("requiresStub").asBoolean(true)).isFalse();
    }

    @Test void rejectsTrailingJsonAndDuplicateKeysWithoutLeakingContent() throws Exception {
        var tree = topology("V3.3.9");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(tree + " {}", "", "V3.3.9", "org.example"))
                .hasMessageContaining("one JSON document");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert("{\"secret\":\"sensitive\",\"secret\":1}", "", "V3.3.9", "org.example"))
                .hasMessageNotContaining("sensitive");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void importsNamedRouterBranchesWithoutChangingTheirEdges(String pack) throws Exception {
        var tree = topology(pack);
        var flow = (ObjectNode) tree.path("flows").get(0);
        var elements = (com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements");
        elements.add(component(pack, "Multi Recipient Router", "Route"));
        elements.add(component(pack, "Logging Producer", "Other log"));
        var transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Poll").put("to", "Route").put("name", "default");
        transitions.addObject().put("from", "Route").put("to", "Log").put("name", "orders");
        transitions.addObject().put("from", "Route").put("to", "Other log").put("name", "audit");
        var result = IkasanRuntimeImport.convert(tree.toString(), configuration().toString(), pack, "org.example", mappings());
        var module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "routed import", false);
        assertThat(module.getFlows().get(0).getFlowRoute().getChildRoutes()).hasSize(2);
        assertThat(module.getFlows().get(0).getFlowRoute().getChildRoutes())
                .extracting(org.ikasan.studio.core.model.ikasan.instance.FlowRoute::getRouteName).containsExactly("orders", "audit");
    }

    @Test void keepsUnknownMetadataAndStructuredConfigurationValuesThroughSave() throws Exception {
        var tree = topology("V3.3.9");
        tree.putObject("futureMetadata").put("keep", true);
        var configuration = configuration();
        ((com.fasterxml.jackson.databind.node.ArrayNode) configuration.get("parameters")).addObject()
                .put("name", "extraMap").putObject("value").put("nested", 42);
        var result = IkasanRuntimeImport.convert(tree.toString(), "[" + configuration + "]", "V3.3.9", "org.example", mappings());
        var saved = JSON.readTree(ComponentIO.toValidatedModuleJson(ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false)));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("module").path("futureMetadata").path("keep").asBoolean()).isTrue();
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration").get(0)).isEqualTo(configuration);
    }

    @Test void rejectsWrongVariantAndPreservesIncorrectlyTypedParametersForReview() throws Exception {
        var tree = topology("V3.3.9");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(tree.toString(), "", "V3.3.9", "org.example",
                java.util.Map.of(new IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "FTP Producer")))
                .hasMessageContaining("does not match runtime component");
        var config = configuration();
        ((ObjectNode) config.path("parameters").get(0)).put("value", 42);
        var result = IkasanRuntimeImport.convert(tree.toString(), config.toString(), "V3.3.9", "org.example", mappings());
        assertThat(result.review().toString()).contains("retained configuration field cronExpression");
        assertThat(JSON.readTree(result.studioJson()).path("flows").get(0).path("consumer").has("cronExpression")).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void importsVerifiedEndpointConfigurationFields(String pack) throws Exception {
        for (var example : java.util.Map.of(
                "FTP Producer", "remoteHost", "SFTP Producer", "remoteHost",
                "Spring JMS Producer", "destinationJndiName", "Email Producer", "emailBody",
                "FTP Consumer", "filenamePattern", "SFTP Consumer", "filenamePattern",
                "Spring JMS Consumer", "destinationJndiName", "Logging Producer", "replacementText").entrySet()) {
            var tree = topology(pack);
            var flow = (ObjectNode) tree.path("flows").get(0);
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, example.getKey());
            String name = meta.isConsumer() ? "Poll" : "Log";
            var endpoint = component(pack, example.getKey(), name).put("configurationId", "endpoint-config");
            var elements = (com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements");
            if (meta.isConsumer()) { flow.set("consumer", endpoint); elements.set(1, endpoint.deepCopy()); }
            else elements.set(0, endpoint);
            String configClass = meta.getRuntimeConfigurationClass() != null ? meta.getRuntimeConfigurationClass()
                    : meta.getAllowableProperties().get("configuration").getUsageDataType();
            var config = JSON.createObjectNode().put("configurationId", "endpoint-config").put("implementingClass", configClass);
            config.putArray("parameters").addObject().put("name", example.getValue()).put("value", "fixture-value");
            var configs = JSON.createArrayNode().add(configuration()).add(config);
            var choices = new java.util.HashMap<>(mappings());
            choices.put(new IkasanRuntimeImport.ComponentRef("Orders", name), example.getKey());
            var result = IkasanRuntimeImport.convert(tree.toString(), configs.toString(), pack, "org.example", choices);
            var importedFlow = JSON.readTree(result.studioJson()).path("flows").get(0);
            var imported = meta.isConsumer() ? importedFlow.get("consumer") : importedFlow.path("flowElements").get(0);
            assertThat(imported.path(example.getValue()).asText()).as(example.getKey()).isEqualTo("fixture-value");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void acceptsTopologyFixturesFromCoreIkasan(String pack) throws Exception {
        for (String resource : java.util.List.of("simpleFlow.json", "multiRecipientFlow.json")) {
            try (var stream = getClass().getResourceAsStream("/studio/templates/runtime-json/" + resource)) {
                assertThat(stream).isNotNull();
                var flow = JSON.readTree(stream);
                ObjectNode module = JSON.createObjectNode().put("name", "Core fixture").put("version", "application-version")
                        .put("ikasanVersion", pack.substring(1));
                module.putArray("flows").add(flow);
                var result = IkasanRuntimeImport.convert(module.toString(), "", pack, "org.example.fixture");
                var saved = JSON.readTree(ComponentIO.toValidatedModuleJson(ComponentIO.validatePersistedModuleJson(result.studioJson(), "core fixture", false)));
                assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("module")).isEqualTo(module);
                assertThat(saved.path("flows").get(0).path("consumer").path("componentName")).isEqualTo(flow.path("consumer").path("componentName"));
            }
        }
    }

    private java.util.Map<IkasanRuntimeImport.ComponentRef, String> mappings() {
        return java.util.Map.of(new IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "Scheduled Consumer");
    }

    private IkasanRuntimeImport.Result convert(ObjectNode tree) throws Exception {
        return IkasanRuntimeImport.convert(tree.toString(), configuration().toString(), "V3.3.9", "org.example", mappings());
    }
}
