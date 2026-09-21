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
    void flowStartupPropertyPersistsGeneratesAndSupportsUndoRedo(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        live.setPropertyValue("flowStartupType", "AUTOMATIC");
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree(CREATE))).apply();
        var flow = live.getFlows().get(0);
        var consumer = flow.getConsumer();
        String before = ComponentIO.toValidatedModuleJson(live);
        var prepared = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"setFlowProperty","flow":"Transfer","property":"flowStartupType","value":"MANUAL"}]
                """));
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0)).isSameAs(flow);
        assertThat(flow.getConsumer()).isSameAs(consumer);
        assertThat(flow.getPropertyValue("flowStartupType")).isEqualTo("MANUAL");
        String after = ComponentIO.toValidatedModuleJson(live);
        assertThat(ComponentIO.validatePersistedModuleJson(after, "test", false).getFlows().get(0)
                .getPropertyValue("flowStartupType")).isEqualTo("MANUAL");
        assertThat(org.ikasan.studio.core.generator.PropertiesTemplate.create(live))
                .contains("defaultStartupType=AUTOMATIC", "flowStartupTypes[0]=Transfer,MANUAL");
        changes.undo();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
        changes.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(after);
        var clear = ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"setFlowProperty","flow":"Transfer","property":"flowStartupType","value":null}]
                """)));
        clear.apply();
        assertThat(org.ikasan.studio.core.generator.PropertiesTemplate.create(live)).doesNotContain("flowStartupTypes[0]");
        clear.undo();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(after);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void flowPropertyRejectsInvalidChoicesTypesAndStructuralEditsWithoutChangingLiveModel(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree(CREATE))).apply();
        String before = ComponentIO.toValidatedModuleJson(live);
        for (String property : List.of("name", "version", "testHarnessOwner", "configurationId", "unknown")) {
            var op = JSON.createObjectNode().put("type", "setFlowProperty").put("flow", "Transfer")
                    .put("property", property).put("value", "changed");
            assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.createArrayNode().add(op)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("property");
        }
        for (String value : List.of("\"BOGUS\"", "true", "[]")) {
            assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree(
                    "[{\"type\":\"setFlowProperty\",\"flow\":\"Transfer\",\"property\":\"flowStartupType\",\"value\":" + value + "}]")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void nestedFanoutRoutesPersistAndNewBranchesOnExistingFlowsUndo(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"addFlow","flow":"Nested"},
                 {"type":"addComponent","flow":"Nested","key":"Generic Consumer","name":"Input"}]
                """))).apply();
        var original = live.getFlows().get(0);
        var consumer = original.getConsumer();
        String before = ComponentIO.toValidatedModuleJson(live);
        var operations = JSON.readTree("""
                [{"type":"addComponent","flow":"Nested","key":"Single Recipient Router","name":"Choice"},
                 {"type":"configureRoutes","flow":"Nested","component":"Choice","names":["Accepted","Rejected"]},
                 {"type":"addComponent","flow":"Nested","route":["Accepted"],"key":"Multi Recipient Router","name":"Fanout"},
                 {"type":"configureRoutes","flow":"Nested","component":"Fanout","names":["Audit","Delivery"]},
                 {"type":"addComponent","flow":"Nested","route":["Accepted","Audit"],"key":"Logging Producer","name":"AuditLog"},
                 {"type":"addComponent","flow":"Nested","route":["Accepted","Delivery"],"key":"Logging Producer","name":"DeliveryLog"},
                 {"type":"addComponent","flow":"Nested","route":["Rejected"],"key":"Dev Null Producer","name":"Discard"},
                 {"type":"connect","flow":"Nested","order":["Input","Choice"]},
                 {"type":"setExceptionResolution","flow":"Nested","exception":"org.ikasan.spec.component.routing.RouterException","action":"excludeEvent"}]
                """);
        var edit = ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), operations));
        edit.apply();
        assertThat(live.getFlows().get(0)).isSameAs(original);
        assertThat(original.getConsumer()).isSameAs(consumer);
        assertThat(original.getFlowIntegrityStatus()).isBlank();
        var nested = original.getFlowRoute().getChildRoutes().get(0).getChildRoutes().get(0);
        assertThat(nested.getFlowElements()).allSatisfy(e -> {
            assertThat(e.getContainingFlow()).isSameAs(original);
            assertThat(e.getContainingFlowRoute()).isSameAs(nested);
        });
        String saved = ComponentIO.toValidatedModuleJson(live);
        var reloaded = ComponentIO.validatePersistedModuleJson(saved, "route test", false);
        assertThat(reloaded.getFlows().get(0).getFlowIntegrityStatus()).isBlank();
        assertThat(saved).contains("AuditLog", "Fanout", "excludeEvent");
        String generated = org.ikasan.studio.core.generator.FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, reloaded, reloaded.getFlows().get(0));
        assertThat(generated).contains("Accepted", "Rejected", "Audit", "Delivery", "OnException.excludeEvent()");
        for (var component : reloaded.getFlows().get(0).ftlGetConsumerAndFlowElements()) {
            if (component.getComponentMeta().isRouter()) {
                String implementation = org.ikasan.studio.core.generator.FlowsUserImplementedComponentTemplate.create(
                        TestFixtures.DEFAULT_PACKAGE, reloaded, reloaded.getFlows().get(0), component);
                assertThat(implementation).contains("route");
            }
        }

        edit.undo();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
        edit.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(saved);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void buildsBranchesAndExceptionPolicyAndPreservesObjectsAcrossUndo(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var initial = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"addFlow","flow":"Routing"},
                 {"type":"addComponent","flow":"Routing","key":"Generic Consumer","name":"Input"},
                 {"type":"addComponent","flow":"Routing","key":"Single Recipient Router","name":"Choose"},
                 {"type":"configureRoutes","flow":"Routing","component":"Choose","names":["Accepted","Rejected"]},
                 {"type":"addComponent","flow":"Routing","route":["Accepted"],"key":"Logging Producer","name":"LogAccepted"},
                 {"type":"addComponent","flow":"Routing","route":["Rejected"],"key":"Dev Null Producer","name":"DiscardRejected"},
                 {"type":"setExceptionResolution","flow":"Routing","exception":"org.ikasan.spec.component.routing.RouterException","action":"excludeEvent"}]
                """));
        ModelProposal.changes(live, initial).apply();
        var flow = live.getFlows().get(0);
        var router = flow.getFlowRoute().getFlowElements().get(0);
        var branch = flow.getFlowRoute().getChildRoutes().get(0);
        var producer = branch.getFlowElements().get(branch.getFlowElements().size()-1);
        String before = ComponentIO.toValidatedModuleJson(live);
        var prepared = ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"renameComponent","flow":"Routing","component":"LogAccepted","name":"AcceptedLog"},
                 {"type":"addComponent","flow":"Routing","route":["Rejected"],"key":"Converter","name":"Inspect"},
                 {"type":"connect","flow":"Routing","route":["Rejected"],"order":["Inspect","DiscardRejected"]},
                 {"type":"setExceptionResolution","flow":"Routing","exception":"org.ikasan.spec.component.routing.RouterException.class","action":"retry","properties":{"delay":5,"interval":3}}]
                """));
        var change = ModelProposal.changes(live, prepared);
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
        change.apply();
        assertThat(flow.getFlowRoute().getFlowElements().get(0)).isSameAs(router);
        assertThat(flow.getFlowRoute().getChildRoutes().get(0)).isSameAs(branch);
        assertThat(branch.getFlowElements()).contains(producer);
        assertThat(producer.getIdentity()).isEqualTo("AcceptedLog");
        assertThat(flow.getExceptionResolver().getExceptionResolutionList().get(0).getTheAction()).isEqualTo("retry");
        String after = ComponentIO.toValidatedModuleJson(live);
        change.undo();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(before);
        change.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(after);
        for (String operation : List.of(
                "{\"type\":\"configureRoutes\",\"flow\":\"Routing\",\"component\":\"Choose\",\"names\":[\"NewA\",\"NewB\"]}",
                "{\"type\":\"deleteComponent\",\"flow\":\"Routing\",\"component\":\"Choose\"}",
                "{\"type\":\"setExceptionResolution\",\"flow\":\"Routing\",\"exception\":\"Bad();\",\"action\":\"excludeEvent\"}",
                "{\"type\":\"setExceptionResolution\",\"flow\":\"Routing\",\"exception\":\"java.lang.Exception\",\"action\":\"invented\"}")) {
            assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("[" + operation + "]")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

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
    void existingFlowWithNameOutsideNewNamePatternCanStillBeEditedAndDeleted(String version) throws Exception {
        // Studio does not restrict flow names in the UI, so a developer can legitimately have "Order-Flow".
        // The new-name pattern must apply only to names the proposal introduces, not to existing flows.
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live),
                JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"Transfer\"}]"))).apply();
        live.getFlows().get(0).setName("Order-Flow");
        var snapshot = LiveModelSnapshot.capture(live);

        var addComponent = ModelProposal.prepare(snapshot, JSON.readTree(
                "[{\"type\":\"addComponent\",\"flow\":\"Order-Flow\",\"key\":\"FTP Consumer\",\"name\":\"ReadFiles\","
                        + "\"properties\":{\"cronExpression\":\"0/5 * * * * ?\",\"sourceDirectory\":\"/incoming\"}}]"));
        assertThat(addComponent.summary()).anyMatch(line -> line.contains("Order-Flow"));

        var delete = ModelProposal.prepare(snapshot, JSON.readTree("[{\"type\":\"deleteFlow\",\"flow\":\"Order-Flow\"}]"));
        assertThat(delete.summary()).anyMatch(line -> line.contains("Delete entire flow: Order-Flow"));

        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"New-Flow\"}]")))
                .hasMessageContaining("Names must start with a letter");
    }

    /** The AI reads this message to correct itself, so "Meta cant be null" (no key, no suggestion) is not enough. */
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void anUnknownComponentKeyNamesTheKeyAndHowToFixIt(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var snapshot = LiveModelSnapshot.capture(live);
        String add = "[{\"type\":\"addFlow\",\"flow\":\"T\"},{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"%s\",\"name\":\"X\"}]";

        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree(String.format(add, "ftp consumer"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown component key 'ftp consumer'")
                .hasMessageContaining("Did you mean 'FTP Consumer'");
        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree(String.format(add, "No Such Thing"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown component key 'No Such Thing'")
                .hasMessageContaining("FTP Consumer")
                .hasMessageContaining("Dev Null Producer");
    }

    /** A rejected value should say what is acceptable, without echoing the value (it may be a credential). */
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void aRejectedValueExplainsTheRuleOrTheAllowedChoices(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var snapshot = LiveModelSnapshot.capture(live);
        String start = "{\"type\":\"addFlow\",\"flow\":\"T\"},{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"Event Generating Consumer\",\"name\":\"Start\"},";

        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("[" + start
                + "{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"Object To XML String Converter\",\"name\":\"X\","
                + "\"properties\":{\"objectClass\":\"not a class!\"}}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objectClass")
                .hasMessageContaining("Provide a fully qualified Java class name")
                .hasMessageNotContaining("not a class!");

        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("[" + start
                + "{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"Converter\",\"name\":\"X\","
                + "\"properties\":{\"conversionRecipeId\":\"nope\"}}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversionRecipeId")
                .hasMessageContaining("string-to-bytes")
                .hasMessageNotContaining("nope");
    }

    /** An AI is likely to write a sentence or a Unix cron; Quartz rejects both and the flow then cannot start. */
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void aCronExpressionQuartzWouldRejectIsExplainedToTheAi(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var snapshot = LiveModelSnapshot.capture(live);
        for (String bad : new String[]{"every 5 minutes", "*/5 * * * *"}) {
            assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree(
                    "[{\"type\":\"addFlow\",\"flow\":\"T\"},{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"Scheduled Consumer\","
                            + "\"name\":\"Tick\",\"properties\":{\"cronExpression\":\"" + bad + "\"}}]")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cronExpression")
                    .hasMessageContaining("Quartz cron expression")
                    .hasMessageContaining("0 0/5 * * * ?");
        }
        // A valid expression is accepted.
        ModelProposal.prepare(snapshot, JSON.readTree(
                "[{\"type\":\"addFlow\",\"flow\":\"T\"},{\"type\":\"addComponent\",\"flow\":\"T\",\"key\":\"Scheduled Consumer\","
                        + "\"name\":\"Tick\",\"properties\":{\"cronExpression\":\"0 0/5 * * * ?\"}}]"));
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

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void explicitRecipeTypeMismatchExplainsTheRequiredConfiguration(String version) throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("""
                [{"type":"addFlow","flow":"Delivery"},
                 {"type":"addComponent","flow":"Delivery","key":"Converter","name":"Build Payload",
                  "properties":{"conversionRecipeId":"string-to-file-transfer-payload","toType":"java.lang.String"}}]
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Delivery / Build Payload", "fromType=java.lang.String",
                        "toType=org.ikasan.filetransfer.Payload", "recipeConfigurations");
        assertThat(empty.getFlows()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void genericProducerAcceptsAndGeneratesParameterizedPayloadType(String version) throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var prepared = ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("""
                [{"type":"addFlow","flow":"Map Delivery"},
                 {"type":"addComponent","flow":"Map Delivery","key":"Generic Producer","name":"Record Processed Line",
                  "properties":{"fromType":"java.util.Map<java.lang.String,java.lang.String>"}}]
                """));
        var module = prepared.draft();
        var flow = module.getFlows().get(0);
        var producer = flow.getFlowRoute().getFlowElements().stream()
                .filter(e -> "Record Processed Line".equals(e.getIdentity())).findFirst().orElseThrow();
        String generated = org.ikasan.studio.core.generator.FlowsUserImplementedComponentTemplate.create(
                TestFixtures.DEFAULT_PACKAGE, module, flow, producer);
        assertThat(generated).contains("Producer<java.util.Map<java.lang.String,java.lang.String>>",
                "invoke(java.util.Map<java.lang.String,java.lang.String> payload)");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void newRecipeConvertersInferOnlyOmittedTypes(String version) throws Exception {
        for (String extra : List.of("", ",\"fromType\":\"java.lang.String\"",
                ",\"toType\":\"org.ikasan.filetransfer.Payload\"")) {
            Module empty = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
            var prepared = ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("""
                    [{"type":"addFlow","flow":"Delivery"},
                     {"type":"addComponent","flow":"Delivery","key":"Converter","name":"Encode",
                      "properties":{"conversionRecipeId":"string-to-file-transfer-payload"%s}},
                     {"type":"addComponent","flow":"Delivery","key":"Converter","name":"Decode",
                      "properties":{"conversionRecipeId":"file-transfer-payload-to-string"}}]
                    """.formatted(extra)));
            var components = prepared.draft().getFlows().get(0).getFlowRoute().getFlowElements();
            var encode = components.stream().filter(e -> "Encode".equals(e.getIdentity())).findFirst().orElseThrow();
            var decode = components.stream().filter(e -> "Decode".equals(e.getIdentity())).findFirst().orElseThrow();
            assertThat(encode.getPropertyValueAsString("fromType")).isEqualTo("java.lang.String");
            assertThat(encode.getPropertyValueAsString("toType")).isEqualTo("org.ikasan.filetransfer.Payload");
            assertThat(decode.getPropertyValueAsString("fromType")).isEqualTo("org.ikasan.filetransfer.Payload");
            assertThat(decode.getPropertyValueAsString("toType")).isEqualTo("java.lang.String");
            assertThat(empty.getFlows()).isEmpty();
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

    @Test void numberedFlowNameFailureIdentifiesTheNameAndLeavesModelUntouched() throws Exception {
        var live = TestFixtures.getMyFirstModuleIkasanModule("V3.3.9", new java.util.ArrayList<>());
        var before = LiveModelSnapshot.capture(live);
        assertThatThrownBy(() -> ModelProposal.prepare(before, JSON.readTree(
                "[{\"type\":\"addFlow\",\"flow\":\"01 JMS Publish Orders\"}]")))
                .hasMessageContaining("01 JMS Publish Orders").hasMessageContaining("Flow01");
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        assertThat(ModelProposal.prepare(before, JSON.readTree(
                "[{\"type\":\"addFlow\",\"flow\":\"Demo01 JMS Publish Orders\"}]")).draft().getFlows())
                .hasSize(1);
    }

}
