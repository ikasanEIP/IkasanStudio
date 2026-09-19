package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

public class ModelProposalTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    public static final String CREATE = """
            [{"type":"addFlow","flow":"Transfer"},
             {"type":"addComponent","flow":"Transfer","key":"FTP Consumer","name":"ReadFiles",
              "properties":{"cronExpression":"0/5 * * * * ?","sourceDirectory":"/incoming"}},
             {"type":"addComponent","flow":"Transfer","key":"FTP Producer","name":"WriteFiles",
              "properties":{"outputDirectory":"/outgoing"}},
             {"type":"connect","flow":"Transfer","order":["ReadFiles","WriteFiles"]}]
            """;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void validatesDetachedThenAppliesAndUndoesCompleteFlow(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var before = LiveModelSnapshot.capture(live);
        var prepared = ModelProposal.prepare(before, JSON.readTree(CREATE));
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows()).hasSize(1);
        assertThat(live.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/incoming");
        String persisted = ComponentIO.toValidatedModuleJson(live);
        assertThat(persisted).contains("ReadFiles", "WriteFiles", "transitions");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(persisted);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void emptyFlowCanBeReviewedAppliedGeneratedAndUndoneThroughEitherProposalRoute(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var before = LiveModelSnapshot.capture(live);
        var operations = JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"bob\"}]");
        var prepared = ModelProposal.prepare(before, operations);
        byte[] saved = ComponentIO.toValidatedModuleJson(live).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String file = JSON.writeValueAsString(java.util.Map.of("formatVersion", 1,
                "baseModelSha256", OfflineModelProposal.sha256(saved), "operations", operations));
        assertThat(OfflineModelProposal.prepare(file, saved, before).summary()).isEqualTo(prepared.summary());
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        assertThat(prepared.summary()).anyMatch(line -> line.contains("is incomplete"));
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        var bob = live.getFlows().get(0);
        assertThat(bob.getIdentity()).isEqualTo("bob");
        assertThat(bob.getConsumer()).isNull();
        assertThat(bob.getFlowIntegrityStatus()).isNotBlank();
        String persisted = ComponentIO.toValidatedModuleJson(live);
        assertThat(ComponentIO.validatePersistedModuleJson(persisted, "test", false).getFlows()).hasSize(1);
        // Generation must support the same incomplete scaffold as a manually added Studio flow.
        assertThat(org.ikasan.studio.core.generator.FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, live, bob))
                .contains("class Bob");
        assertThat(org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, live, bob))
                .contains("ComponentFactoryBob");
        assertThat(org.ikasan.studio.core.generator.ModuleConfigTemplate.create(live)).contains("getBob()");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(persisted);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void buildsFlowIncrementallyAcrossSeparateReviews(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var create = ModelProposal.prepare(LiveModelSnapshot.capture(live),
                JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"bob\"}]"));
        ModelProposal.changes(live, create).apply();
        var consumer = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"addComponent","flow":"bob","key":"FTP Consumer","name":"ReadFiles",
                  "properties":{"cronExpression":"0/5 * * * * ?","sourceDirectory":"/incoming"}}]
                """));
        assertThat(consumer.summary()).anyMatch(line -> line.contains("is incomplete"));
        ModelProposal.changes(live, consumer).apply();
        assertThat(live.getFlows().get(0).getConsumer()).isNotNull();
        assertThat(live.getFlows().get(0).getFlowIntegrityStatus()).contains("producer");
        var producer = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"addComponent","flow":"bob","key":"FTP Producer","name":"WriteFiles",
                  "properties":{"outputDirectory":"/outgoing"}}]
                """));
        var changes = ModelProposal.changes(live, producer);
        changes.apply();
        assertThat(live.getFlows().get(0).getFlowIntegrityStatus()).isBlank();
        changes.undo();
        assertThat(live.getFlows().get(0).getConsumer()).isNotNull();
        assertThat(live.getFlows().get(0).getFlowRoute().getFlowElements()).isEmpty();
    }

    @Test void canAddProducerToAnExistingFlowBeforeItsConsumer() throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", new java.util.ArrayList<>());
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live),
                JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"bob\"}]"))).apply();
        var proposal = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"addComponent","flow":"bob","key":"Dev Null Producer","name":"Discard"}]
                """));
        var changes = ModelProposal.changes(live, proposal);
        changes.apply();
        assertThat(live.getFlows().get(0).getConsumer()).isNull();
        assertThat(live.getFlows().get(0).getFlowRoute().getFlowElements()).hasSize(1);
        changes.undo();
        assertThat(live.getFlows().get(0).getFlowRoute().getFlowElements()).isEmpty();
    }

    @Test void editsRetainOriginalObjectsAndUndoRestoresValues() throws Exception {
        Module live = model();
        var flow = live.getFlows().get(0);
        var consumer = flow.getConsumer();
        var producer = flow.getFlowRoute().getFlowElements().get(0);
        var before = LiveModelSnapshot.capture(live);
        var prepared = ModelProposal.prepare(before, JSON.readTree("""
                [{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"sourceDirectory","value":"/new"}]
                """));
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0)).isSameAs(flow);
        assertThat(flow.getConsumer()).isSameAs(consumer);
        assertThat(flow.getFlowRoute().getFlowElements().get(0)).isSameAs(producer);
        assertThat(consumer.getPropertyValue("sourceDirectory")).isEqualTo("/new");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
    }

    @Test void rejectsInvalidProposalsWithoutChangingLiveModel() throws Exception {
        Module live = model();
        var snapshot = LiveModelSnapshot.capture(live);
        for (String operation : List.of(
                "{\"type\":\"deleteFlow\",\"flow\":\"Missing\"}",
                "{\"type\":\"addFlow\",\"flow\":\"Transfer\"}",
                "{\"type\":\"setProperty\",\"flow\":\"Transfer\",\"component\":\"ReadFiles\",\"property\":\"notReal\",\"value\":true}",
                "{\"type\":\"setProperty\",\"flow\":\"Transfer\",\"component\":\"ReadFiles\",\"property\":\"ftps\",\"value\":\"yes\"}",
                "{\"type\":\"connect\",\"flow\":\"Transfer\",\"order\":[\"WriteFiles\",\"ReadFiles\"]}",
                "{\"type\":\"addComponent\",\"flow\":\"Transfer\",\"key\":\"Imaginary\",\"name\":\"Missing\"}")) {
            assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("[" + operation + "]"))).isInstanceOf(Exception.class);
            assertThat(LiveModelSnapshot.capture(live)).isEqualTo(snapshot);
        }
    }

    @Test void rejectsMissingRequiredProperties() throws Exception {
        Module live = model();
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"cronExpression","value":null}]
                """))).hasMessageContaining("missing");
    }

    @Test void snapshotIsDetachedAndPreservesUnknownData() throws Exception {
        Module live = model();
        live.getUnknownJsonProperties().put("future", JSON.readTree("{\"nested\":[1,2]}"));
        var captured = LiveModelSnapshot.capture(live);
        var restored = ComponentIO.validatePersistedModuleJson(JSON.writeValueAsString(captured), "test", false);
        assertThat(ComponentIO.toJson(restored)).isEqualTo(ComponentIO.toJson(live));
        live.getFlows().get(0).getConsumer().setPropertyValue("remoteHost", "different");
        assertThat(LiveModelSnapshot.capture(live)).isNotEqualTo(captured);
    }

    @Test void snapshotRoundTripsRoutersDecoratorsAndResolvers() throws Exception {
        String version = TestFixtures.BASE_META_PACK;
        for (var flow : List.of(TestFixtures.getEventGeneratingConsumerRouterFlow(version),
                TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(version),
                TestFixtures.getExceptionResolverFlow(version))) {
            // The legacy fixture uses "ignore"; a loaded Studio model uses "ignoreException".
            if (flow.getExceptionResolver() != null) {
                flow.getExceptionResolver().getIkasanExceptionResolutionMap().values().forEach(resolution -> {
                    if ("ignore".equals(resolution.getTheAction())) resolution.setTheAction("ignoreException");
                });
            }
            Module live = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
            var restored = ComponentIO.validatePersistedModuleJson(
                    JSON.writeValueAsString(LiveModelSnapshot.capture(live)), "snapshot round trip", false);
            assertThat(JSON.readTree(ComponentIO.toJson(restored))).isEqualTo(JSON.readTree(ComponentIO.toJson(live)));
        }
    }

    @Test void rejectsKnownPayloadMismatch() throws Exception {
        var snapshot = LiveModelSnapshot.capture(model());
        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("""
                [{"type":"addComponent","flow":"Transfer","key":"Converter","name":"WrongType",
                  "properties":{"fromType":"java.lang.String","toType":"java.lang.Integer","userImplementedClassName":"WrongType"}}]
                """))).hasMessageContaining("mismatch");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void wiresProtectedProviderWithReviewApplyAndUndo(String version) throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, List.of());
        Module live = ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("""
                [{"type":"addFlow","flow":"flow1"},
                 {"type":"addComponent","flow":"flow1","key":"Event Generating Consumer","name":"my egc"},
                 {"type":"addComponent","flow":"flow1","key":"Dev Null Producer","name":"Discard"}]
                """)).draft();
        var before = LiveModelSnapshot.capture(live);
        var operations = JSON.readTree("""
                [{"type":"setProperty","flow":"flow1","component":"my egc",
                  "property":"endpointEventProvider","value":"MinuteEventProvider"}]
                """);
        var prepared = ModelProposal.prepare(before, operations);
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        var consumer = live.getFlows().get(0).getConsumer();
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(consumer.getPropertyValue("endpointEventProvider")).isEqualTo("MinuteEventProvider");
        assertThat(consumer.getProperty("endpointEventProvider").isOverwriteEnabled()).isFalse();
        assertThat(ComponentIO.toValidatedModuleJson(live)).contains("MinuteEventProvider");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(consumer.getPropertyValue("endpointEventProvider")).isEqualTo("MinuteEventProvider");
        consumer.getProperty("endpointEventProvider").setOverwriteEnabled(true);
        ((com.fasterxml.jackson.databind.node.ObjectNode) operations.get(0)).put("value", "OtherProvider");
        var overwriteProposal = ModelProposal.prepare(LiveModelSnapshot.capture(live), operations);
        assertThatThrownBy(() -> ModelProposal.changes(live, overwriteProposal))
                .hasMessageContaining("Turn off source overwrite");
        assertThat(consumer.getPropertyValue("endpointEventProvider")).isEqualTo("MinuteEventProvider");
        ((com.fasterxml.jackson.databind.node.ObjectNode) operations.get(0)).put("value", "minuteEventProvider");
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), operations))
                .hasMessageContaining("validation rule");
        ((com.fasterxml.jackson.databind.node.ObjectNode) operations.get(0)).put("property", "managedEventIdentifierService")
                .put("value", "CustomIdentifierService");
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), operations))
                .hasMessageContaining("Edit implementation class properties in Studio");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void renamesKeepIdentityTransitionsAndUndo(String version) throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, List.of());
        Module live = ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree(CREATE)).draft();
        var before = LiveModelSnapshot.capture(live);
        var consumer = live.getFlows().get(0).getConsumer();
        var producer = live.getFlows().get(0).getFlowRoute().getFlowElements().get(0);
        var prepared = ModelProposal.prepare(before, JSON.readTree("""
                [{"type":"renameComponent","flow":"Transfer","component":"ReadFiles","name":"ReadEvents"},
                 {"type":"setProperty","flow":"Transfer","component":"ReadEvents","property":"sourceDirectory","value":"/new"},
                 {"type":"renameComponent","flow":"Transfer","component":"WriteFiles","name":"WriteEvents"},
                 {"type":"renameComponent","flow":"Transfer","component":"ReadEvents","name":"ReadAgain"}]
                """));
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0).getConsumer()).isSameAs(consumer);
        assertThat(live.getFlows().get(0).getFlowRoute().getFlowElements().get(0)).isSameAs(producer);
        var json = JSON.readTree(ComponentIO.toValidatedModuleJson(live));
        assertThat(json.path("flows").get(0).path("transitions").get(0).path("from").asText()).isEqualTo("ReadAgain");
        assertThat(json.path("flows").get(0).path("transitions").get(0).path("to").asText()).isEqualTo("WriteEvents");
        assertThat(consumer.getPropertyValue("sourceDirectory")).isEqualTo("/new");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(consumer.getIdentity()).isEqualTo("ReadAgain");
    }

    @Test void rejectsRenameCollisionsAndInvalidNames() throws Exception {
        var snapshot = LiveModelSnapshot.capture(model());
        for (String name : List.of("WriteFiles", "Write Files", "../escape", "")) {
            var operation = JSON.createObjectNode().put("type", "renameComponent").put("flow", "Transfer")
                    .put("component", "ReadFiles").put("name", name);
            assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.createArrayNode().add(operation)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    public static Module model() throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of());
        return ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree(CREATE)).draft();
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void derivedImplementationDefaultsGenerateDistinctClassesAndAllowOnlyPlaceholderRepairs(String version) throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var prepared = ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("""
                [{"type":"addFlow","flow":"Delivery"},
                 {"type":"addComponent","flow":"Delivery","key":"Scheduled Consumer","name":"Schedule"},
                 {"type":"addComponent","flow":"Delivery","key":"Converter","name":"Create Text",
                  "properties":{"fromType":"org.quartz.JobExecutionContext","toType":"java.lang.String"}},
                 {"type":"addComponent","flow":"Delivery","key":"Converter","name":"Build Payload",
                  "properties":{"conversionRecipeId":"string-to-file-transfer-payload","fromType":"java.lang.String","toType":"org.ikasan.filetransfer.Payload"}},
                 {"type":"addComponent","flow":"Delivery","key":"FTP Producer","name":"Send"}]
                """));
        var module = prepared.draft();
        var flow = module.getFlows().get(0);
        var first = flow.getFlowRoute().getFlowElements().get(0);
        var second = flow.getFlowRoute().getFlowElements().get(1);
        assertThat(first.getPropertyValueAsString("userImplementedClassName")).isEqualTo("CreateText");
        assertThat(second.getPropertyValueAsString("userImplementedClassName")).isEqualTo("BuildPayload");
        for (var component : java.util.List.of(first, second)) {
            assertThat(org.ikasan.studio.core.generator.FlowsUserImplementedComponentTemplate.create(
                    TestFixtures.DEFAULT_PACKAGE, module, flow, component))
                    .contains("class " + component.getPropertyValueAsString("userImplementedClassName"))
                    .doesNotContain("__fieldName:");
        }
        var repair = JSON.readTree("""
                [{"type":"setProperty","flow":"Delivery","component":"Create Text",
                  "property":"userImplementedClassName","value":"CreateDeliveryText"}]
                """);
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(module), repair))
                .hasMessageContaining("Edit implementation class properties in Studio");
        first.setPropertyValue("userImplementedClassName", "__fieldName:componentName");
        var recovery = ModelProposal.prepare(LiveModelSnapshot.capture(module), repair);
        var changes = ModelProposal.changes(module, recovery);
        changes.apply();
        assertThat(first.getPropertyValueAsString("userImplementedClassName")).isEqualTo("CreateDeliveryText");
        changes.undo();
        assertThat(first.getPropertyValueAsString("userImplementedClassName")).isEqualTo("__fieldName:componentName");
        for (String invalid : java.util.List.of("../Bad", "class", "bad-name")) {
            var invalidRepair = repair.deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) invalidRepair.get(0)).put("value", invalid);
            assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(module), invalidRepair))
                    .hasMessageContaining("Edit implementation class properties in Studio");
        }
    }

}
