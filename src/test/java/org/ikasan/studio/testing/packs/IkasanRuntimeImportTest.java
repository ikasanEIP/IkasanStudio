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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.ikasan.studio.core.diagnostics.RuntimeImportReport;
import org.ikasan.studio.core.importer.IkasanRuntimeImport.ComponentRef;
import org.ikasan.studio.core.importer.IkasanRuntimeImport.PackRecommendation;
import org.ikasan.studio.core.importer.IkasanRuntimeImport.Result;
import org.ikasan.studio.core.importer.IkasanRuntimeImport.ReviewItem;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

@Tag("packs")
class IkasanRuntimeImportTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ObjectNode topology(String pack) throws Exception {
        ObjectNode module = JSON.createObjectNode().put("name", "Imported Orders")
                .put("version", "application-2.7").put("ikasanVersion", pack.substring(1));
        ObjectNode flow = module.putArray("flows").addObject().put("name", "Orders");
        flow.put("flowStartupType", "AUTOMATIC");
        ObjectNode consumer = component(pack, "Scheduled Consumer", "Poll");
        consumer.put("configurationId", "poll-config");
        flow.set("consumer", consumer);
        flow.putArray("flowElements").add(component(pack, "Logging Producer", "Log")).add(consumer.deepCopy());
        flow.putArray("transitions").addObject().put("from", "Poll").put("to", "Log").put("name", "default");
        return module;
    }

    private ObjectNode component(String pack, String key, String name) throws Exception {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, key);
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

    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6", "V5.1.0"})
    void wrappedComponentsImportTheirDelegateContractInsteadOfTheOuterRuntimeClass(String pack) throws Exception {
        ObjectNode input = topology(pack); ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        JsonNode consumer = flow.path("consumer").deepCopy(); ObjectNode log = component(pack, "Logging Producer", "Log");
        flow.putArray("flowElements").add(component(pack, "Default Message Filter", "Wrapped").put("configurationId", "wrapped-config")).add(log).add(consumer);
        ArrayNode transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Poll").put("to", "Wrapped").put("name", "default");
        transitions.addObject().put("from", "Wrapped").put("to", "Log").put("name", "default");
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false);
        Flow importedFlow = module.getFlows().get(0);
        FlowElement wrapped = importedFlow.getFlowElementsNoExternalEndPoints().stream()
                .filter(e -> "Wrapped".equals(e.getComponentName())).findFirst().orElseThrow();
        String contract = wrapped.getComponentMeta().getVerificationImplementationContract();
        assertThat(contract).isEqualTo("org.ikasan.spec.component.filter.FilterRule");
        assertThat(wrapped.getPropertyValueAsString("userImplementedClassName")).isEqualTo(contract);
        assertThat(wrapped.getPropertyValue("requiresStub")).isEqualTo(false);
        String generated = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create("org.ikasan.studio.boot.flow.orders", module, importedFlow);
        assertThat(generated).contains(contract + " wrapped;")
                .contains("new org.ikasan.filter.DefaultMessageFilter(wrapped)")
                .doesNotContain("org.ikasan.filter.DefaultMessageFilter wrapped;")
                .contains("component.setConfiguredResourceId(")
                .doesNotContain("wrapped.setConfiguredResourceId(");
        assertThat(result.review()).anyMatch(line -> line.contains("original delegate implementing " + contract));
    }

    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6", "V5.1.0"})
    void derivesH2PortsFromImportedHttpPort(String pack) throws Exception {
        ObjectNode input = topology(pack).put("port", 12410);
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false);
        assertThat(module.getPort()).isEqualTo("12410");
        assertThat(module.getH2PortNumber()).isEqualTo("12411");
        assertThat(module.getH2WebPortNumber()).isEqualTo("12412");
    }

    @Test void validHighHttpPortDoesNotProduceInvalidH2Defaults() throws Exception {
        Result result = IkasanRuntimeImport.convert(topology("V3.3.9").put("port", 65535).toString(), configuration().toString(), "V3.3.9", "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false);
        assertThat(module.getPort()).isEqualTo("65535");
        assertThat(module.getH2PortNumber()).isEqualTo("12451"); assertThat(module.getH2WebPortNumber()).isEqualTo("12452");
        assertThat(result.review()).anyMatch(line -> line.contains("H2 ports could not be derived"));
    }

    @Test void structuredReviewRetainsOriginalMessagesAndExactComponentIdentities() throws Exception {
        String pack = "V4.1.6";
        ObjectNode input = topology(pack);
        ((ObjectNode) input.path("flows").get(0).path("flowElements").get(0))
                .put("implementingClass", "example.ExistingProducer");
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        assertThat(result.reviewItems()).hasSize(3);
        assertThat(result.reviewItems()).extracting(IkasanRuntimeImport.ReviewItem::kind).containsExactlyInAnyOrder(
                IkasanRuntimeImport.ReviewKind.IMPLEMENTATION, IkasanRuntimeImport.ReviewKind.CONFIGURATION,
                IkasanRuntimeImport.ReviewKind.INFERRED_VARIANT);
        ReviewItem implementation = result.reviewItems().stream()
                .filter(item -> item.kind() == IkasanRuntimeImport.ReviewKind.IMPLEMENTATION).findFirst().orElseThrow();
        assertThat(implementation.subject()).isEqualTo("example.ExistingProducer");
        assertThat(implementation.component()).isEqualTo(new IkasanRuntimeImport.ComponentRef("Orders", "Log"));
        for (ReviewItem item : result.reviewItems()) assertThat(result.review()).contains(item.message());
        assertThat(JSON.readTree(result.studioJson()).path(IkasanRuntimeImport.SOURCE_FIELD).path("review"))
                .isEqualTo(JSON.valueToTree(result.review()));
    }

    @Test void diagnosticsUseActualPackMatchingAndTypedConfigurationWithoutLeakingUnknownNames() throws Exception {
        String pack = "V4.1.6";
        RuntimeImportReport report = new org.ikasan.studio.core.diagnostics.RuntimeImportReport();
        ObjectNode config = configuration();
        ((ObjectNode) config.withArray("parameters").get(0)).put("value", true);
        IkasanRuntimeImport.diagnose(topology(pack).toString(), config.toString(), pack, report);
        String text = report.render("1.0.0-beta.1", "262.123.4");
        assertThat(text).contains("F1/C1/P1", "CONFIGURATION_TYPE_BOOLEAN_EXPECTED_STRING",
                "Studio property: cronExpression", "org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer",
                "CONFIGURATION_PROPERTY_UNMAPPED");
        assertThat(text).doesNotContain("customSecret", "do-not-show-in-report", "Imported Orders", "poll-config");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void importsSeparateContractsAndPreservesOriginalsWithoutTreatingApplicationVersionAsPack(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example.imported", mappings());
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false);
        assertThat(module.getVersion()).isEqualTo(pack);
        assertThat(module.getFlows()).hasSize(1);
        assertThat(module.getFlows().get(0).getConsumer().getPropertyValue("cronExpression")).isEqualTo("0/10 * * * * ?");
        assertThat(module.getFlows().get(0).getPropertyValue("flowStartupType")).isEqualTo("AUTOMATIC");
        JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).get("module")).isEqualTo(input);
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).get("configuration")).isEqualTo(config);
        assertThat(result.review().toString()).contains("customSecret", "not applied").doesNotContain("do-not-show-in-report");
    }

    @Test void importsAnOlderPatchWithExplicitReviewAndKeepsTheSourceVersion() throws Exception {
        ObjectNode input = topology("V4.1.6").put("ikasanVersion", "4.1.3");
        Result result = IkasanRuntimeImport.convert(input.toString(), "", "V4.1.6", "org.example", mappings());
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "patch import", false);
        assertThat(module.getVersion()).isEqualTo("V4.1.6");
        assertThat(module.getFlows().get(0).getPropertyValue("flowStartupType")).isEqualTo("AUTOMATIC");
        assertThat(result.review().toString()).contains("Version upgrade", "4.1.3", "4.1.6", "compatibility is not guaranteed");
        JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("module").path("ikasanVersion").asText()).isEqualTo("4.1.3");
        PackRecommendation choice = IkasanRuntimeImport.recommendPack(input.toString(), java.util.List.of("V3.3.9", "V4.1.6")).orElseThrow();
        assertThat(choice.pack()).isEqualTo("V4.1.6");
        assertThat(choice.exact()).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"4.1.7", "4.1.10", "4.2.0", "3.3.9", "5.0.1-SNAPSHOT", "4.1.3-RC1"})
    void rejectsDowngradesCrossMajorAndUnrecognisedPrereleases(String source) throws Exception {
        ObjectNode input = topology("V4.1.6").put("ikasanVersion", source);
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(input.toString(), "", "V4.1.6", "org.example", mappings()))
                .hasMessageContaining("matching meta-pack");
        assertThat(IkasanRuntimeImport.recommendPack(input.toString(), java.util.List.of("V4.1.6"))).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"5.0.1-SNAPSHOT", "5.0.1", "5.1.0-SNAPSHOT"})
    void proposesAndConvertsNewerSameMajorReleaseWithReview(String source) throws Exception {
        String input = "{\"name\":\"Demo\",\"ikasanVersion\":\"" + source + "\",\"flows\":[]}";
        PackRecommendation choice = IkasanRuntimeImport.recommendPack(input,
                java.util.List.of("V3.3.9", "V4.1.6", "V5.1.0")).orElseThrow();
        assertThat(choice.pack()).isEqualTo("V5.1.0");
        assertThat(choice.exact()).isFalse();
        Result result = IkasanRuntimeImport.convert(input, "", choice.pack(), "org.example");
        assertThat(result.review().toString()).contains("Version upgrade", source, "5.1.0",
                "compatibility is not guaranteed");
        JsonNode saved = JSON.readTree(result.studioJson());
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("module").path("ikasanVersion").asText())
                .isEqualTo(source);
    }

    @Test void offersOnlyForwardSameMajorReleases() throws Exception {
        String input = "{\"ikasanVersion\":\"4.0.9-SNAPSHOT\",\"flows\":[]}";
        assertThat(IkasanRuntimeImport.recommendPack(input, java.util.List.of("V5.1.0", "V4.1.6"))
                .orElseThrow().pack()).isEqualTo("V4.1.6");
        assertThat(IkasanRuntimeImport.recommendPack(input.replace("4.0.9-SNAPSHOT", "5.2.0"),
                java.util.List.of("V3.3.9", "V4.1.6", "V5.1.0"))).isEmpty();
    }

    @Test void recommendsExactVersionsBeforeOtherPacksAndDoesNotGuessMissingVersions() throws Exception {
        ObjectNode input = topology("V3.3.9");
        PackRecommendation choice = IkasanRuntimeImport.recommendPack(input.toString(), java.util.List.of("V4.1.6", "V3.3.9")).orElseThrow();
        assertThat(choice.pack()).isEqualTo("V3.3.9");
        assertThat(choice.exact()).isTrue();
        input.remove("ikasanVersion");
        assertThat(IkasanRuntimeImport.recommendPack(input.toString(), java.util.List.of("V3.3.9", "V4.1.6"))).isEmpty();
    }

    @Test void rejectsMismatchedPack() {
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(), "", "V4.1.6", "org.example"))
                .hasMessageContaining("matching meta-pack");
    }

    @Test void rejectsDanglingEdgesCyclesAndDisconnectedNodes() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        ObjectNode flow = (ObjectNode) tree.path("flows").get(0);
        ((ObjectNode) flow.path("transitions").get(0)).put("to", "Missing");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("missing component");
        ((ObjectNode) flow.path("transitions").get(0)).put("to", "Poll");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("cycles");
        flow.putArray("transitions");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("disconnected");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void preservesRunningApplicationPortFromMetadata(String pack) throws Exception {
        ObjectNode input = topology(pack).put("port", 12450);
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "original port", false);
        assertThat(module.getPort()).isEqualTo("12450");
        assertThat(result.review().toString()).doesNotContain("default port", "ports and build settings use Studio defaults");
        assertThat(JSON.readTree(result.studioJson()).path(IkasanRuntimeImport.SOURCE_FIELD).path("module"))
                .isEqualTo(input);
    }

    @Test void missingPortNeedsReviewAndInvalidPortCannotSilentlyUseDefault() throws Exception {
        String pack = "V4.1.6";
        ObjectNode input = topology(pack);
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        assertThat(result.review().toString()).contains("Application HTTP port was not supplied", "Verify Studio's default port");
        for (String invalid : java.util.List.of("0", "65536", "-1", "1.5", "true", "\"not-a-port\"")) {
            input.set("port", JSON.readTree(invalid));
            assertThatThrownBy(() -> IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example"))
                    .hasMessageContaining("port must be an integer from 1 to 65535");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void preservesMixedFlowStartupControlsInModelAndGeneratedProperties(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ArrayNode flows = (com.fasterxml.jackson.databind.node.ArrayNode) input.path("flows");
        ObjectNode original = (ObjectNode) flows.get(0);
        original.put("name", "Automatic Orders").put("flowStartupType", "AUTOMATIC");
        flows.add(original.deepCopy().put("name", "Manual Orders").put("flowStartupType", "MANUAL"));
        flows.add(original.deepCopy().put("name", "Disabled Orders").put("flowStartupType", "DISABLED"));
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "mixed startup", false);
        assertThat(module.getFlows()).extracting(flow -> flow.getPropertyValue("flowStartupType"))
                .containsExactly("AUTOMATIC", "MANUAL", "DISABLED");
        Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)));
        assertThat(properties.getProperty("ikasan.module.activator.startup.type.flowStartupTypes[0]"))
                .isEqualTo("Automatic Orders,AUTOMATIC");
        assertThat(properties.getProperty("ikasan.module.activator.startup.type.flowStartupTypes[1]"))
                .isEqualTo("Manual Orders,MANUAL");
        assertThat(properties.getProperty("ikasan.module.activator.startup.type.flowStartupTypes[2]"))
                .isEqualTo("Disabled Orders,DISABLED");
        assertThat(result.review().toString()).doesNotContain("Flows are imported MANUAL", "startup type was not supplied");
        assertThat(JSON.readTree(result.studioJson()).path(IkasanRuntimeImport.SOURCE_FIELD).path("module"))
                .isEqualTo(input);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void missingAndNullStartupControlsFallBackToManualWithFlowSpecificReview(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        flow.remove("flowStartupType");
        for (boolean explicitNull : java.util.List.of(false, true)) {
            if (explicitNull) flow.putNull("flowStartupType");
            Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example");
            Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "missing startup", false);
            assertThat(module.getFlows().get(0).getPropertyValue("flowStartupType")).isEqualTo("MANUAL");
            assertThat(result.review()).contains("Flow 'Orders': startup type was not supplied. Imported as MANUAL; verify the original startup setting before running.");
            Properties properties = new java.util.Properties();
            properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)));
            assertThat(properties.getProperty("ikasan.module.activator.startup.type.flowStartupTypes[0]"))
                    .isEqualTo("Orders,MANUAL");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void unknownAndMalformedStartupControlsAreRejectedBeforeImport(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        for (BaseJsonNode invalid : java.util.List.of(JSON.getNodeFactory().textNode("UNKNOWN"),
                JSON.getNodeFactory().textNode("automatic"), JSON.getNodeFactory().textNode(""),
                JSON.getNodeFactory().numberNode(1), JSON.getNodeFactory().booleanNode(true), JSON.createObjectNode())) {
            flow.set("flowStartupType", invalid);
            assertThatThrownBy(() -> IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example"))
                    .hasMessageContaining("Flow 'Orders': unrecognised startup type")
                    .hasMessageContaining("AUTOMATIC, MANUAL or DISABLED");
        }
    }

    @Test void rejectsConflictingConsumerDefinition() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        ((ObjectNode) tree.path("flows").get(0).path("flowElements").get(1)).put("implementingClass", "other.Consumer");
        assertThatThrownBy(() -> convert(tree)).hasMessageContaining("duplicate component");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void linkedConfigurationClassesResolveAllFourScheduledConsumerVariants(String pack) throws Exception {
        for (String variant : java.util.List.of("Scheduled Consumer", "FTP Consumer", "SFTP Consumer", "Local File Consumer")) {
            ObjectNode input = topology(pack);
            ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            String configClass = meta.getRuntimeConfigurationClass();
            if (configClass == null) configClass = meta.getAllowableProperties().get("configuration").getUsageDataType();
            ObjectNode config = configuration().put("implementingClass", configClass);
            // Unmapped/custom fields are retained for review, never used to guess the variant.
            assertThat(IkasanRuntimeImport.mappingChoices(input.toString(), config.toString(), pack)).isEmpty();
            Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
            Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "inferred import", false);
            assertThat(module.getFlows().get(0).getConsumer().getComponentMeta().getName()).isEqualTo(variant);
            assertThat(module.getFlows().get(0).getConsumer().getPropertyValue("cronExpression")).isEqualTo("0/10 * * * * ?");
            assertThat(module.getFlows().get(0).getPropertyValue("flowStartupType")).isEqualTo("AUTOMATIC");
            assertThat(result.review().toString()).contains("inferred variant " + variant, configClass)
                    .doesNotContain("do-not-show-in-report");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void reviewOnlyHighlightsPopulatedUnappliedFieldsAndRequiredWiring(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        ArrayNode parameters = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("parameters");
        parameters.addObject().put("name", "cronExpressions").putNull("value");
        parameters.addObject().put("name", "description");
        parameters.addObject().put("name", "customSetting").put("value", "private-setting-value");
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).filteredOn(line -> line.contains("review configuration fields not applied"))
                .containsExactly("Orders → Poll: review configuration fields not applied: customSecret, customSetting. "
                        + "Compare with the original configuration before running.");
        assertThat(result.review().toString())
                .contains("Review the original message-provider wiring before running.")
                .doesNotContain("applied configuration field", "cronExpression", "description",
                        "do-not-show-in-report", "private-setting-value");
        JsonNode saved = JSON.readTree(result.studioJson());
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);
        assertThat(saved.path("flows").get(0).path("consumer").path("cronExpression").asText())
                .isEqualTo("0/10 * * * * ?");

        // A completely mapped configuration needs no per-field review item.
        parameters.removeAll();
        parameters.addObject().put("name", "cronExpression").put("value", "0/10 * * * * ?");
        Result mapped = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(mapped.review()).noneMatch(line -> line.contains("configuration fields"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void verifiedRuntimeDefaultsDoNotNeedReviewButRemainInProvenance(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        ArrayNode parameters = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("parameters");
        parameters.removeAll();
        parameters.addObject().put("name", "cronExpression").put("value", "0/10 * * * * ?");
        Map<String,JsonNode> defaults = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer").getRuntimeConfigurationDefaults();
        assertThat(defaults.keySet()).containsExactlyInAnyOrder("cronExpressions", "passthroughProperties", "persistentRecovery", "recoveryTolerance");
        defaults.forEach((name, value) -> parameters.addObject().put("name", name).set("value", value.deepCopy()));
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "verified defaults", false);
        assertThat(JSON.readTree(ComponentIO.toValidatedModuleJson(module)).path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration"))
                .isEqualTo(config);
        // A class mismatch cannot borrow defaults from a different configuration type.
        config.put("implementingClass", "custom.OtherConfiguration");
        Result mismatched = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings());
        assertThat(mismatched.review().toString()).contains("cronExpressions", "passthroughProperties", "persistentRecovery", "recoveryTolerance");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void changedDefaultsUnknownEmptyValuesAndWrongTypesStillNeedReview(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        ArrayNode parameters = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("parameters");
        parameters.removeAll();
        parameters.addObject().put("name", "cronExpression").put("value", "0/10 * * * * ?");
        parameters.addObject().put("name", "cronExpressions").putArray("value").add("0/20 * * * * ?");
        parameters.addObject().put("name", "passthroughProperties").putObject("value").put("custom", "do-not-show-in-report");
        parameters.addObject().put("name", "persistentRecovery").put("value", false);
        parameters.addObject().put("name", "recoveryTolerance").put("value", 60000);
        parameters.addObject().put("name", "unknownEmptyList").putArray("value");
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).filteredOn(line -> line.contains("configuration fields not applied"))
                .containsExactly("Orders → Poll: review configuration fields not applied: cronExpressions, passthroughProperties, "
                        + "persistentRecovery, recoveryTolerance, unknownEmptyList. Compare with the original configuration before running.");
        assertThat(result.review().toString()).doesNotContain("do-not-show-in-report");
        parameters.removeAll();
        parameters.addObject().put("name", "persistentRecovery").put("value", "true");
        parameters.addObject().put("name", "recoveryTolerance").put("value", "1800000");
        Result wrongTypes = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(wrongTypes.review().toString()).contains("configuration fields not applied: persistentRecovery, recoveryTolerance");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void ftpDefaultsAreQuietButChangedUnsupportedSettingsStillNeedReview(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        ObjectNode consumer = component(pack, "FTP Consumer", "Poll").put("configurationId", "poll-config");
        flow.set("consumer", consumer);
        ((com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements")).set(1, consumer.deepCopy());
        ObjectNode config = configuration().put("implementingClass", "org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration");
        ArrayNode parameters = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("parameters");
        parameters.removeAll();
        Map<String,JsonNode> defaults = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "FTP Consumer").getRuntimeConfigurationDefaults();
        assertThat(defaults.keySet()).containsExactlyInAnyOrder("cronExpressions", "passthroughProperties",
                "persistentRecovery", "provideRelativePath", "recoveryTolerance");
        defaults.forEach((name, value) -> parameters.addObject().put("name", name).set("value", value.deepCopy()));
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
        assertThat(JSON.readTree(result.studioJson()).path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);
        parameters.removeAll();
        parameters.addObject().put("name", "provideRelativePath").put("value", true);
        parameters.addObject().put("name", "persistentRecovery").put("value", false);
        parameters.addObject().put("name", "cronExpressions").putArray("value").add("0/20 * * * * ?");
        parameters.addObject().put("name", "recoveryTolerance").put("value", 60000);
        parameters.addObject().put("name", "passthroughProperties").putObject("value").put("custom", "hidden");
        Result changed = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(changed.review().toString()).contains("configuration fields not applied", "provideRelativePath",
                "persistentRecovery", "cronExpressions", "recoveryTolerance", "passthroughProperties");
        parameters.removeAll();
        parameters.addObject().put("name", "provideRelativePath").put("value", "false");
        assertThat(IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example").review().toString())
                .contains("configuration fields not applied: provideRelativePath");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void xmlRuntimeClassAndConfigurationAreRecognisedWithoutGuessingInputClass(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        // Use the actual REST class explicitly, independently of the pack descriptor.
        ObjectNode xml = JSON.createObjectNode().put("componentName", "XML converter")
                .put("componentType", "org.ikasan.spec.component.transformation.Converter")
                .put("implementingClass", "org.ikasan.component.converter.xml.ObjectToXMLStringConverter")
                .put("configurationId", "xml-config");
        ((com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements")).add(xml);
        ArrayNode transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Poll").put("to", "XML converter").put("name", "default");
        transitions.addObject().put("from", "XML converter").put("to", "Log").put("name", "default");
        ArrayNode parameters = JSON.createArrayNode();
        List<String> flags = java.util.List.of("fastFailOnConfigurationLoad", "noNamespaceSchema",
                "routeOnValidationException", "useNamespacePrefix", "validate");
        ObjectNode config = JSON.createObjectNode().put("configurationId", "xml-config")
                .put("implementingClass", "org.ikasan.component.converter.xml.XmlConfiguration");
        config.set("parameters", parameters);
        for (boolean enabled : new boolean[]{false, true}) {
            parameters.removeAll();
            flags.forEach(name -> parameters.addObject().put("name", name).put("value", enabled));
            parameters.addObject().put("name", "rootName").put("value", "order");
            Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings());
            assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
            assertThat(result.review().toString()).contains("XML converter: supply objectClass");
            JsonNode tree = JSON.readTree(result.studioJson());
            JsonNode imported = java.util.stream.StreamSupport.stream(tree.path("flows").get(0).path("flowElements").spliterator(), false)
                    .filter(node -> node.path("componentName").asText().equals("XML converter")).findFirst().orElseThrow();
            assertThat(imported.path("implementingClass").asText()).isEqualTo("org.ikasan.component.converter.xml.ObjectToXMLStringConverter");
            flags.forEach(name -> assertThat(imported.path(name).asBoolean()).isEqualTo(enabled));
            assertThat(imported.path("rootName").asText()).isEqualTo("order");
            assertThat(imported.has("objectClass")).isFalse();
            Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "xml import", false);
            // Existing flat Studio models used the builder identifier. Reopening must not turn them generic.
            String historical = result.studioJson().replace("org.ikasan.component.converter.xml.ObjectToXMLStringConverter",
                    "org.ikasan.builder.component.converter.ObjectToXmlStringConverterBuilder");
            Module historicalModule = ComponentIO.validatePersistedModuleJson(historical, "historical xml import", false);
            assertThat(historicalModule.getFlows().get(0).getFlowRoute().getFlowElements().get(0)
                    .getComponentMeta().getName()).isEqualTo("Object To XML String Converter");
            JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
            assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);
            JsonNode oldContainer = JSON.readTree(org.ikasan.studio.core.generator.ModelTemplate.create(module));
            ObjectNode oldProperties = (ObjectNode) oldContainer.path("studio").path("flows").path("Orders")
                    .path("components").path("XML converter").path("properties");
            for (JsonNode record : oldContainer.path("configuration")) {
                if (!record.path("configurationId").asText().equals("xml-config")) continue;
                ArrayNode oldParameters = (com.fasterxml.jackson.databind.node.ArrayNode) record.path("parameters");
                oldParameters.forEach(parameter -> oldProperties.set(parameter.path("name").asText(), parameter.path("value").deepCopy()));
                oldParameters.removeAll();
            }
            String historicalContainer = oldContainer.toString().replace("org.ikasan.component.converter.xml.ObjectToXMLStringConverter",
                    "org.ikasan.builder.component.converter.ObjectToXmlStringConverterBuilder");
            Module reloaded = ComponentIO.validatePersistedModuleJson(historicalContainer, "historical xml reload", false);
            Flow importedFlow = reloaded.getFlows().get(0);
            FlowElement importedXml = importedFlow.getFlowRoute().getFlowElements().get(0);
            flags.forEach(name -> assertThat(importedXml.getPropertyValue(name)).isEqualTo(enabled));
            // The developer supplies the missing JAXB class; generation must retain the imported settings.
            importedXml.setPropertyValue("objectClass", "org.example.Order");
            String generated = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create("org.example", reloaded, importedFlow);
            assertThat(generated).contains(".setObjectClass(org.example.Order.class)", ".setValidate(",
                    ".setFastFailOnConfigurationLoad(", ".setRouteOnValidationException(");
            Properties properties = new java.util.Properties();
            properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(reloaded)));
            for (String name : flags) {
                String key = org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(reloaded,
                        importedFlow, importedXml, importedXml.getProperty(name).getMeta().getPropertyConfigFileLabel());
                assertThat(properties.getProperty(key)).isEqualTo(Boolean.toString(enabled));
            }
        }
        parameters.removeAll();
        parameters.addObject().put("name", "validate").put("value", "true");
        assertThat(IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings()).review().toString())
                .contains("configuration fields not applied: validate");
        config.put("implementingClass", "custom.OtherConfiguration");
        parameters.removeAll();
        parameters.addObject().put("name", "validate").put("value", false);
        assertThat(IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings()).review().toString())
                .contains("configuration fields not applied: validate");
    }

    @Test void missingUnknownAndUnlinkedConfigurationStillRequireExplicitChoices() throws Exception {
        String pack = "V3.3.9";
        ObjectNode input = topology(pack);
        for (String config : java.util.List.of("", "[]",
                configuration().put("implementingClass", "custom.UnknownConfiguration").toString(),
                configuration().put("configurationId", "another-component").toString())) {
            assertThat(IkasanRuntimeImport.mappingChoices(input.toString(), config, pack))
                    .singleElement().satisfies(choice -> assertThat(choice.variants()).contains("Scheduled Consumer", "Local File Consumer"));
            assertThatThrownBy(() -> IkasanRuntimeImport.convert(input.toString(), config, pack, "org.example"))
                    .hasMessageContaining("select a variant explicitly");
        }
        assertThat(IkasanRuntimeImport.mappingChoices(input.toString(), pack)).hasSize(1);
    }

    @Test void duplicateConfigurationEvidenceIsRejectedAndExplicitChoiceRemainsAuthoritative() throws Exception {
        String pack = "V4.1.6";
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        assertThatThrownBy(() -> IkasanRuntimeImport.mappingChoices(input.toString(),
                "[" + config + "," + config + "]", pack)).hasMessageContaining("Duplicate configurationId");
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example",
                java.util.Map.of(new IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "Local File Consumer"));
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "explicit import", false);
        assertThat(module.getFlows().get(0).getConsumer().getComponentMeta().getName()).isEqualTo("Local File Consumer");
        assertThat(result.review().toString()).doesNotContain("selected variant", "inferred variant");
    }

    @Test void restOmittedNullValuesRemainUnsetAndDoNotBlockInference() throws Exception {
        String pack = "V4.1.6";
        ObjectNode input = topology(pack);
        ObjectNode config = configuration();
        ArrayNode parameters = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("parameters");
        parameters.addObject().put("name", "jobName")
                .put("implementingClass", "org.ikasan.configurationService.model.ConfigurationParameterStringImpl");
        parameters.addObject().put("name", "timezone").putNull("value");
        assertThat(IkasanRuntimeImport.mappingChoices(input.toString(), config.toString(), pack)).isEmpty();
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "unset REST values", false);
        assertThat(module.getFlows().get(0).getConsumer().getPropertyValue("timezone")).isNull();
        JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);
        assertThat(result.review().toString()).doesNotContain("jobName", "timezone", "applied configuration field");
        parameters.addObject().put("name", "jobName");
        assertThatThrownBy(() -> IkasanRuntimeImport.mappingChoices(input.toString(), config.toString(), pack))
                .hasMessageContaining("duplicate parameter");
    }

    @Test void rejectsDuplicateConfigurationsAndMalformedParameters() throws Exception {
        ObjectNode config = configuration();
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(),
                "[" + config + "," + config + "]", "V3.3.9", "org.example")).hasMessageContaining("Duplicate configurationId");
        config.remove("parameters");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(topology("V3.3.9").toString(), config.toString(),
                "V3.3.9", "org.example")).hasMessageContaining("parameters must be an array");
    }

    @Test void preservesCustomConverterReferenceAndRequestsNoStub() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        ObjectNode producer = (ObjectNode) tree.path("flows").get(0).path("flowElements").get(0);
        producer.put("implementingClass", "com.acme.OrderProducer");
        Result result = convert(tree);
        JsonNode element = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
        assertThat(element.path("userImplementedClassName").asText()).isEqualTo("com.acme.OrderProducer");
        assertThat(element.path("requiresStub").asBoolean(true)).isFalse();
    }

    @Test void rejectsTrailingJsonAndDuplicateKeysWithoutLeakingContent() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(tree + " {}", "", "V3.3.9", "org.example"))
                .hasMessageContaining("one JSON document");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert("{\"secret\":\"sensitive\",\"secret\":1}", "", "V3.3.9", "org.example"))
                .hasMessageNotContaining("sensitive");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void importsNamedRouterBranchesWithoutChangingTheirEdges(String pack) throws Exception {
        ObjectNode tree = topology(pack);
        ObjectNode flow = (ObjectNode) tree.path("flows").get(0);
        ArrayNode elements = (com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements");
        elements.add(component(pack, "Multi Recipient Router", "Route"));
        elements.add(component(pack, "Logging Producer", "Other log"));
        ArrayNode transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Poll").put("to", "Route").put("name", "default");
        transitions.addObject().put("from", "Route").put("to", "Log").put("name", "orders");
        transitions.addObject().put("from", "Route").put("to", "Other log").put("name", "audit");
        Result result = IkasanRuntimeImport.convert(tree.toString(), configuration().toString(), pack, "org.example", mappings());
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "routed import", false);
        assertThat(module.getFlows().get(0).getFlowRoute().getChildRoutes()).hasSize(2);
        assertThat(module.getFlows().get(0).getFlowRoute().getChildRoutes())
                .extracting(org.ikasan.studio.core.model.ikasan.instance.FlowRoute::getRouteName).containsExactly("orders", "audit");
    }

    @Test void keepsUnknownMetadataAndStructuredConfigurationValuesThroughSave() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        tree.putObject("futureMetadata").put("keep", true);
        ObjectNode configuration = configuration();
        ((com.fasterxml.jackson.databind.node.ArrayNode) configuration.get("parameters")).addObject()
                .put("name", "extraMap").putObject("value").put("nested", 42);
        Result result = IkasanRuntimeImport.convert(tree.toString(), "[" + configuration + "]", "V3.3.9", "org.example", mappings());
        JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(ComponentIO.validatePersistedModuleJson(result.studioJson(), "test", false)));
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("module").path("futureMetadata").path("keep").asBoolean()).isTrue();
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration").get(0)).isEqualTo(configuration);
    }

    @Test void rejectsWrongVariantAndPreservesIncorrectlyTypedParametersForReview() throws Exception {
        ObjectNode tree = topology("V3.3.9");
        assertThatThrownBy(() -> IkasanRuntimeImport.convert(tree.toString(), "", "V3.3.9", "org.example",
                java.util.Map.of(new IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "FTP Producer")))
                .hasMessageContaining("does not match runtime component");
        ObjectNode config = configuration();
        ((ObjectNode) config.path("parameters").get(0)).put("value", 42);
        Result result = IkasanRuntimeImport.convert(tree.toString(), config.toString(), "V3.3.9", "org.example", mappings());
        assertThat(result.review().toString()).contains("review configuration fields not applied: cronExpression");
        assertThat(JSON.readTree(result.studioJson()).path("flows").get(0).path("consumer").has("cronExpression")).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void importsDevNullWithBuilderConstructionAndRetainsItAfterSaving(String pack) throws Exception {
        for (String identity : java.util.List.of("org.ikasan.component.endpoint.util.producer.DevNull",
                "org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl")) {
            ObjectNode input = topology(pack);
            ObjectNode endpoint = component(pack, "Dev Null Producer", "Log").put("implementingClass", identity);
            ((com.fasterxml.jackson.databind.node.ArrayNode) input.path("flows").get(0).path("flowElements")).set(0, endpoint);
            Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example", mappings());
            JsonNode producer = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
            assertThat(producer.path("implementingClass").asText()).isEqualTo("org.ikasan.builder.component.endpoint.DevNullProducerBuilderImpl");
            assertThat(producer.has("userImplementedClassName")).isFalse();
            assertThat(result.reviewItems()).noneMatch(item -> item.kind() == IkasanRuntimeImport.ReviewKind.IMPLEMENTATION
                    && item.component().component().equals("Log"));
            Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "DevNull import", false);
            module = ComponentIO.validatePersistedModuleJson(ComponentIO.toValidatedModuleJson(module), "DevNull reload", false);
            String factory = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create(
                    "org.example", module, module.getFlows().get(0));
            assertThat(factory).contains(".devNullProducer()", ".build()")
                    .doesNotContain("org.ikasan.component.endpoint.util.producer.DevNull", "return log;");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void similarlyNamedCustomProducerRemainsAnExistingUserImplementation(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ((ObjectNode) input.path("flows").get(0).path("flowElements").get(0))
                .put("implementingClass", "example.custom.DevNull");
        Result result = IkasanRuntimeImport.convert(input.toString(), configuration().toString(), pack, "org.example", mappings());
        JsonNode producer = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
        assertThat(producer.path("userImplementedClassName").asText()).isEqualTo("example.custom.DevNull");
        assertThat(producer.path("requiresStub").asBoolean(true)).isFalse();
        assertThat(result.review().toString()).contains("supply existing implementation example.custom.DevNull");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void importsConcreteFtpAndSftpRuntimeClassesWithoutGenericFallback(String pack) throws Exception {
        for (String protocol : java.util.List.of("ftp", "sftp")) {
            String variant = protocol.equals("ftp") ? "FTP Producer" : "SFTP Producer";
            String runtimeClass = "org.ikasan.endpoint." + protocol + ".producer."
                    + (protocol.equals("ftp") ? "FtpProducer" : "SftpProducer");
            ObjectNode tree = topology(pack);
            ObjectNode endpoint = component(pack, variant, "Log").put("implementingClass", runtimeClass)
                    .put("configurationId", "endpoint-config");
            ((com.fasterxml.jackson.databind.node.ArrayNode) tree.path("flows").get(0).path("flowElements")).set(0, endpoint);
            ObjectNode config = JSON.createObjectNode().put("configurationId", "endpoint-config")
                    .put("implementingClass", runtimeClass + "Configuration");
            ObjectNode values = JSON.createObjectNode().put("remoteHost", "ftp.example.test").put("remotePort", 2121)
                    .put("username", "fixture-user").put("password", "fixture-password")
                    .put("clientID", "fixture-client").put("outputDirectory", "/out");
            if (protocol.equals("ftp")) {
                for (String key : java.util.List.of("FTPS", "active", "checksumDelivered", "cleanupJournalOnComplete",
                        "createParentDirectory", "ftpsIsImplicit", "overwrite", "unzip")) values.put(key, true);
                for (String key : java.util.List.of("connectionTimeout", "dataTimeout", "ftpsPort", "maxRetryAttempts", "socketTimeout"))
                    values.put(key, 123);
                for (String key : java.util.List.of("ftpsKeyStoreFilePassword", "ftpsKeyStoreFilePath", "ftpsProtocol",
                        "renameExtension", "systemKey")) values.put(key, "fixture-value");
            }
            ArrayNode parameters = config.putArray("parameters");
            values.fields().forEachRemaining(e -> parameters.addObject().put("name", e.getKey()).set("value", e.getValue()));
            ArrayNode configs = JSON.createArrayNode().add(configuration()).add(config);
            Result result = IkasanRuntimeImport.convert(tree.toString(), configs.toString(), pack, "org.example", mappings());
            JsonNode imported = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
            ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            assertThat(imported.path("implementingClass").asText()).isEqualTo(meta.getImplementingClass());
            assertThat(imported.path("additionalKey").asText()).isEqualTo(meta.getAdditionalKey());
            values.fields().forEachRemaining(e -> assertThat(imported.get(e.getKey().equals("FTPS") ? "ftps" : e.getKey()))
                    .as(pack + " " + variant + " " + e.getKey()).isEqualTo(e.getValue()));
            assertThat(result.review()).noneMatch(line -> line.contains("Log: review configuration fields not applied"));
        }
    }

    @Test void doesNotTreatAnUndeclaredCustomProducerAsFtpFromItsConfigurationClass() throws Exception {
        ObjectNode tree = topology("V4.1.6");
        ObjectNode endpoint = component("V4.1.6", "FTP Producer", "Log")
                .put("implementingClass", "example.CustomFtpProducer").put("configurationId", "endpoint-config");
        ((com.fasterxml.jackson.databind.node.ArrayNode) tree.path("flows").get(0).path("flowElements")).set(0, endpoint);
        ObjectNode config = JSON.createObjectNode().put("configurationId", "endpoint-config")
                .put("implementingClass", "org.ikasan.endpoint.ftp.producer.FtpProducerConfiguration");
        config.putArray("parameters").addObject().put("name", "remoteHost").put("value", "ftp.example.test");
        Result result = IkasanRuntimeImport.convert(tree.toString(), JSON.createArrayNode().add(configuration()).add(config).toString(),
                "V4.1.6", "org.example", mappings());
        JsonNode imported = JSON.readTree(result.studioJson()).path("flows").get(0).path("flowElements").get(0);
        assertThat(imported.path("userImplementedClassName").asText()).isEqualTo("example.CustomFtpProducer");
        assertThat(imported.has("remoteHost")).isFalse();
        assertThat(result.review().toString()).contains("configuration fields not applied: remoteHost");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void sftpImportsFilterAndMoveSettingsWhileKeepingUnsupportedValuesVisible(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration().put("implementingClass", "org.ikasan.endpoint.sftp.consumer.SftpConsumerConfiguration");
        ArrayNode parameters = config.withArray("parameters");
        parameters.removeAll();
        ObjectNode values = (ObjectNode) JSON.readTree("""
                {"cronExpression":"0/10 * * * * ?", "filterDuplicates":false, "filterOnFilename":false,
                 "filterOnLastModifiedDate":false, "moveOnSuccess":true, "moveOnSuccessNewPath":"/processed",
                 "cronExpressions":[], "passthroughProperties":{}, "persistentRecovery":true,
                 "recoveryTolerance":1800000, "provideRelativePath":false}
                """);
        values.fields().forEachRemaining(entry -> parameters.addObject().put("name", entry.getKey()).set("value", entry.getValue()));
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "SFTP import", false);
        FlowElement consumer = module.getFlows().get(0).getConsumer();
        assertThat(consumer.getComponentMeta().getName()).isEqualTo("SFTP Consumer");
        for (String name : java.util.List.of("filterDuplicates", "filterOnFilename", "filterOnLastModifiedDate"))
            assertThat(consumer.getPropertyValue(name)).as(name).isEqualTo(false);
        assertThat(consumer.getPropertyValue("moveOnSuccess")).isEqualTo(true);
        assertThat(consumer.getPropertyValue("moveOnSuccessNewPath")).isEqualTo("/processed");
        Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)));
        assertThat(properties.getProperty("orders.poll.sftp.consumer.filterDuplicates")).isEqualTo("false");
        assertThat(properties.getProperty("orders.poll.sftp.consumer.moveOnSuccess")).isEqualTo("true");
        String factory = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create(
                "org.example", module, module.getFlows().get(0));
        assertThat(factory).contains(".setFilterDuplicates(", ".setFilterOnFilename(",
                ".setFilterOnLastModifiedDate(", ".setMoveOnSuccess(");
        assertThat(JSON.readTree(ComponentIO.toValidatedModuleJson(module))
                .path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);

        // Empty is not the construction default (null). Never hide an unknown credential value.
        parameters.addObject().put("name", "privateKeyPassphrase").put("value", "");
        assertThat(IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example").review().toString())
                .contains("configuration fields not applied: privateKeyPassphrase");
        for (int i = 0; i < parameters.size(); i++) {
            ObjectNode parameter = (ObjectNode) parameters.get(i);
            if (parameter.path("name").asText().equals("provideRelativePath")) parameter.put("value", true);
            if (parameter.path("name").asText().equals("filterDuplicates")) parameter.put("value", "false");
        }
        Result changed = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(changed.review().toString()).contains("filterDuplicates", "provideRelativePath", "privateKeyPassphrase");
        config.put("implementingClass", "custom.OtherConfiguration");
        Result mismatched = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example",
                java.util.Map.of(new IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "SFTP Consumer"));
        assertThat(mismatched.review().toString()).contains("cronExpressions", "passthroughProperties", "persistentRecovery", "recoveryTolerance");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void sftpProducerDefaultsAreQuietButChangedValuesAndCredentialsRequireReview(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode flow = (ObjectNode) input.path("flows").get(0);
        ((com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements")).set(0,
                component(pack, "SFTP Producer", "Log").put("configurationId", "producer-config"));
        ObjectNode config = JSON.createObjectNode().put("configurationId", "producer-config")
                .put("implementingClass", "org.ikasan.endpoint.sftp.producer.SftpProducerConfiguration");
        ArrayNode parameters = config.putArray("parameters");
        ObjectNode rename = parameters.addObject().put("name", "dontUseFileRename").put("value", false);
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings());
        assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
        rename.put("value", true);
        parameters.addObject().put("name", "privateKeyPassphrase").put("value", "private-test-value");
        Result changed = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings());
        assertThat(changed.review().toString()).contains("dontUseFileRename", "privateKeyPassphrase")
                .doesNotContain("private-test-value");
        rename.put("value", "false");
        assertThat(IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example", mappings()).review().toString())
                .contains("dontUseFileRename");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void localFileImportPreservesPatternsAndOnlyReviewsChangedUnmappedSettings(String pack) throws Exception {
        ObjectNode input = topology(pack);
        ObjectNode config = configuration().put("implementingClass",
                "org.ikasan.component.endpoint.filesystem.messageprovider.FileConsumerConfiguration");
        ArrayNode parameters = config.withArray("parameters");
        parameters.removeAll();
        parameters.addObject().put("name", "cronExpression").put("value", "0/10 * * * * ?");
        ArrayNode filenames = JSON.createArrayNode().add("orders.*\\.csv").add("other,orders\\.txt");
        parameters.addObject().put("name", "filenames").set("value", filenames);
        // Effective runtime defaults from FileConsumerConfiguration and ScheduledConsumerConfiguration.
        ObjectNode defaults = (ObjectNode) JSON.readTree("""
                {"cronExpressions": [], "dynamicFileName": false, "followSymbolicLinks": true,
                 "passthroughProperties": {}, "persistentRecovery": true, "recoveryTolerance": 1800000}
                """);
        defaults.fields().forEachRemaining(entry -> parameters.addObject().put("name", entry.getKey())
                .set("value", entry.getValue()));
        Result result = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(result.review()).noneMatch(line -> line.contains("configuration fields not applied"));
        Module module = ComponentIO.validatePersistedModuleJson(result.studioJson(), "local file import", false);
        JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(module));
        assertThat(saved.path("flows").get(0).path("consumer").path("filenames")).isEqualTo(filenames);
        assertThat(saved.path(IkasanRuntimeImport.SOURCE_FIELD).path("configuration")).isEqualTo(config);
        Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)));
        assertThat(JSON.readTree(properties.getProperty("orders.file.consumer.filenames"))).isEqualTo(filenames);

        JsonNode changed = JSON.readTree("""
                {"cronExpressions": ["0/20 * * * * ?"], "dynamicFileName": true, "followSymbolicLinks": false,
                 "passthroughProperties": {"key":"value"}, "persistentRecovery": false, "recoveryTolerance": 1000}
                """);
        for (int i = 2; i < parameters.size(); i++) {
            ObjectNode parameter = (ObjectNode) parameters.get(i);
            parameter.set("value", changed.get(parameter.path("name").asText()));
        }
        Result changedResult = IkasanRuntimeImport.convert(input.toString(), config.toString(), pack, "org.example");
        assertThat(changedResult.review().toString()).contains(
                "configuration fields not applied: cronExpressions, dynamicFileName, followSymbolicLinks, "
                        + "passthroughProperties, persistentRecovery, recoveryTolerance");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void importsVerifiedEndpointConfigurationFields(String pack) throws Exception {
        for (Entry<String,String> example : java.util.Map.of(
                "FTP Producer", "remoteHost", "SFTP Producer", "remoteHost",
                "Spring JMS Producer", "destinationJndiName", "Email Producer", "emailBody",
                "FTP Consumer", "filenamePattern", "SFTP Consumer", "filenamePattern",
                "Spring JMS Consumer", "destinationJndiName", "Logging Producer", "replacementText").entrySet()) {
            ObjectNode tree = topology(pack);
            ObjectNode flow = (ObjectNode) tree.path("flows").get(0);
            ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, example.getKey());
            String name = meta.isConsumer() ? "Poll" : "Log";
            ObjectNode endpoint = component(pack, example.getKey(), name).put("configurationId", "endpoint-config");
            ArrayNode elements = (com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements");
            if (meta.isConsumer()) { flow.set("consumer", endpoint); elements.set(1, endpoint.deepCopy()); }
            else elements.set(0, endpoint);
            String configClass = meta.getRuntimeConfigurationClass() != null ? meta.getRuntimeConfigurationClass()
                    : meta.getAllowableProperties().get("configuration").getUsageDataType();
            ObjectNode config = JSON.createObjectNode().put("configurationId", "endpoint-config").put("implementingClass", configClass);
            config.putArray("parameters").addObject().put("name", example.getValue()).put("value", "fixture-value");
            ArrayNode configs = JSON.createArrayNode().add(configuration()).add(config);
            HashMap<ComponentRef,String> choices = new java.util.HashMap<>(mappings());
            choices.put(new IkasanRuntimeImport.ComponentRef("Orders", name), example.getKey());
            Result result = IkasanRuntimeImport.convert(tree.toString(), configs.toString(), pack, "org.example", choices);
            JsonNode importedFlow = JSON.readTree(result.studioJson()).path("flows").get(0);
            JsonNode imported = meta.isConsumer() ? importedFlow.get("consumer") : importedFlow.path("flowElements").get(0);
            assertThat(imported.path(example.getValue()).asText()).as(example.getKey()).isEqualTo("fixture-value");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void acceptsTopologyFixturesFromCoreIkasan(String pack) throws Exception {
        for (String resource : java.util.List.of("simpleFlow.json", "multiRecipientFlow.json")) {
            try (InputStream stream = getClass().getResourceAsStream("/studio/templates/runtime-json/" + resource)) {
                assertThat(stream).isNotNull();
                JsonNode flow = JSON.readTree(stream);
                ObjectNode module = JSON.createObjectNode().put("name", "Core fixture").put("version", "application-version")
                        .put("ikasanVersion", pack.substring(1));
                module.putArray("flows").add(flow);
                Result result = IkasanRuntimeImport.convert(module.toString(), "", pack, "org.example.fixture");
                JsonNode saved = JSON.readTree(ComponentIO.toValidatedModuleJson(ComponentIO.validatePersistedModuleJson(result.studioJson(), "core fixture", false)));
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
