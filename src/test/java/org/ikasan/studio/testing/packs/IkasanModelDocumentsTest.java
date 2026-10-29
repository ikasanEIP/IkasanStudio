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

    @Test void savedDocumentsLockTheirPackRevisionAndOldDocumentsAcquireALockOnSave() throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "legacy", false);
        ObjectNode saved = (ObjectNode) json.readTree(ModelTemplate.create(module));
        String current = org.ikasan.studio.core.metapack.ComponentLibrary.getMetaPackManifest(module.getVersion()).packVersion();
        assertThat(saved.at("/studio/metaPackRevision").asText()).isEqualTo(current);
        ((ObjectNode) saved.get("studio")).remove("metaPackRevision");
        var olderDocument = ComponentIO.validatePersistedModuleJson(saved.toString(), "older document", false);
        assertThat(json.readTree(ModelTemplate.create(olderDocument)).at("/studio/metaPackRevision").asText()).isEqualTo(current);
    }

    @Test void mismatchedRevisionIsRejectedAndExplicitAdoptionRetainsTheOriginal(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "legacy", false);
        ObjectNode original = (ObjectNode) json.readTree(ModelTemplate.create(module));
        ((ObjectNode) original.get("studio")).put("metaPackRevision", "0.0.1");
        String source = original.toString();
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(source, "locked", false))
                .hasMessageContaining("locked to revision 0.0.1");
        var plan = org.ikasan.studio.core.metapack.MetaPackRevisionLock.preview(source);
        ObjectNode expected = original.deepCopy();
        ((ObjectNode) expected.get("studio")).put("metaPackRevision", plan.availableRevision());
        assertThat(json.readTree(plan.targetJson())).isEqualTo(expected);
        Path model = directory.resolve("model.json");
        Files.writeString(model, source);
        org.ikasan.studio.core.metapack.MetaPackRevisionLock.apply(model, plan);
        assertThat(Files.readString(model.resolveSibling("model.json.bak.1"))).isEqualTo(source);
        assertThat(ComponentIO.validatePersistedModuleJson(Files.readString(model), "adopted", false).getVersion()).isEqualTo(module.getVersion());
    }

    @Test void revisionAdoptionRejectsStaleOrForgedReviews(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        ObjectNode original = (ObjectNode) json.readTree(ModelTemplate.create(ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "legacy", false)));
        ((ObjectNode) original.get("studio")).put("metaPackRevision", "0.0.1");
        var plan = org.ikasan.studio.core.metapack.MetaPackRevisionLock.preview(original.toString());
        Path model = directory.resolve("model.json");
        Files.writeString(model, original + "\n");
        assertThatThrownBy(() -> org.ikasan.studio.core.metapack.MetaPackRevisionLock.apply(model, plan)).hasMessageContaining("changed after review");
        Files.writeString(model, original.toString());
        var forged = new org.ikasan.studio.core.metapack.MetaPackRevisionLock.Plan(plan.packId(), plan.previousRevision(), plan.availableRevision(), plan.sourceJson(), "{}");
        assertThatThrownBy(() -> org.ikasan.studio.core.metapack.MetaPackRevisionLock.apply(model, forged)).hasMessageContaining("stale");
        assertThat(Files.readString(model)).isEqualTo(original.toString());
        assertThat(Files.exists(model.resolveSibling("model.json.bak.1"))).isFalse();
    }

    @Test void malformedRevisionLocksAreRejected() throws Exception {
        ObjectNode source = (ObjectNode) json.readTree(ModelTemplate.create(ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "legacy", false)));
        for (var value : java.util.List.of(json.nullNode(), json.getNodeFactory().numberNode(42), json.getNodeFactory().textNode(""))) {
            ((ObjectNode) source.get("studio")).set("metaPackRevision", value);
            assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(source.toString(), "malformed lock", false)).hasMessageContaining("metaPackRevision");
        }
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

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
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

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
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

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT", "V5.1.0"})
    void omittedRuntimeValuesStayUnsetAcrossImportSaveReloadAndGeneration(String pack) throws Exception {
        var topology = json.createObjectNode().put("name", "Imported JMS").put("ikasanVersion", pack.substring(1));
        var flow = topology.putArray("flows").addObject().put("name", "Orders").put("flowStartupType", "AUTOMATIC");
        var elements = flow.putArray("flowElements");
        var configuration = json.createArrayNode();
        for (String variant : java.util.List.of("Spring JMS Consumer", "Spring JMS Producer")) {
            var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            String name = meta.isConsumer() ? "Receive" : "Send";
            var element = elements.addObject().put("componentName", name).put("componentType", meta.getComponentType())
                    .put("implementingClass", meta.getImplementingClass()).put("configurationId", name + "-config");
            if (meta.isConsumer()) flow.set("consumer", element.deepCopy());
            String type = meta.getRuntimeConfigurationClass();
            if (type == null) type = meta.getAllowableProperties().get("configuration").getUsageDataType();
            var parameters = configuration.addObject().put("configurationId", name + "-config")
                    .put("implementingClass", type).putArray("parameters");
            parameters.addObject().put("name", "destinationJndiName").put("value", "queue/orders");
            if (!meta.isConsumer()) {
                parameters.addObject().put("id", 12).put("name", "priority")
                        .put("implementingClass", "org.ikasan.configurationService.model.ConfigurationParameterIntegerImpl");
                parameters.addObject().put("name", "receiveTimeout").putNull("value");
                parameters.addObject().put("name", "deliveryPersistent").put("value", false);
                parameters.addObject().put("name", "timeToLive").put("value", 0L);
            }
        }
        flow.putArray("transitions").addObject().put("from", "Receive").put("to", "Send").put("name", "default");
        var imported = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(
                topology.toString(), configuration.toString(), pack, "org.example");
        var module = ComponentIO.validatePersistedModuleJson(imported.studioJson(), "REST import", false);
        ObjectNode saved = (ObjectNode) json.readTree(ModelTemplate.create(module));
        // Exercise loading a native saved document with the exact REST shape, independently of the writer.
        saved.set("configuration", configuration.deepCopy());
        for (int pass = 0; pass < 3; pass++) {
            module = ComponentIO.validatePersistedModuleJson(saved.toString(), "REST-shaped native model", false);
            var importedFlow = module.getFlows().get(0);
            var producer = importedFlow.getFlowRoute().getFlowElements().get(0);
            assertThat(producer.getPropertyValue("priority")).isNull();
            assertThat(producer.getPropertyValue("receiveTimeout")).isNull();
            assertThat(producer.getPropertyValue("deliveryPersistent")).isEqualTo(false);
            assertThat(producer.getPropertyValue("timeToLive")).isEqualTo(0L);
            assertThat(FlowsComponentFactoryTemplate.create("org.example", module, importedFlow))
                    .doesNotContain(".setPriority(", ".setReceiveTimeout(")
                    .contains(".setDeliveryPersistent(", ".setTimeToLive(");
            saved = (ObjectNode) json.readTree(ModelTemplate.create(module));
            var record = java.util.stream.StreamSupport.stream(saved.path("configuration").spliterator(), false)
                    .filter(node -> node.path("configurationId").asText().equals("Send-config")).findFirst().orElseThrow();
            var priority = java.util.stream.StreamSupport.stream(record.path("parameters").spliterator(), false)
                    .filter(node -> node.path("name").asText().equals("priority")).findFirst().orElseThrow();
            assertThat(priority.has("value")).isFalse();
            assertThat(priority.path("id").asInt()).isEqualTo(12);
            assertThat(saved.path("studio").path("properties").path("ikasanRuntimeImport").path("configuration"))
                    .isEqualTo(json.readTree(configuration.toString()));
        }
        var activeProducer = module.getFlows().get(0).getFlowRoute().getFlowElements().get(0);
        activeProducer.setPropertyValue("priority", 7);
        var configured = json.readTree(ModelTemplate.create(module));
        assertThat(configured.path("configuration").get(1).path("parameters"))
                .anySatisfy(parameter -> {
                    assertThat(parameter.path("name").asText()).isEqualTo("priority");
                    assertThat(parameter.path("value").asInt()).isEqualTo(7);
                });
        var configuredModule = ComponentIO.validatePersistedModuleJson(configured.toString(), "configured priority", false);
        configuredModule.getFlows().get(0).getFlowRoute().getFlowElements().get(0).setPropertyValue("priority", null);
        var cleared = json.readTree(ModelTemplate.create(configuredModule));
        assertThat(cleared.path("configuration").get(1).path("parameters"))
                .noneSatisfy(parameter -> {
                    assertThat(parameter.path("name").asText()).isEqualTo("priority");
                    assertThat(parameter.hasNonNull("value")).isTrue();
                });
        var producerRecord = (ObjectNode) saved.path("configuration").get(1);
        var priority = (ObjectNode) java.util.stream.StreamSupport.stream(producerRecord.path("parameters").spliterator(), false)
                .filter(node -> node.path("name").asText().equals("priority")).findFirst().orElseThrow();
        priority.put("value", "not-an-integer");
        String invalid = saved.toString();
        assertThatThrownBy(() -> ComponentIO.validatePersistedModuleJson(invalid, "wrong type", false))
                .hasMessageContaining("Invalid configuration value type for priority");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT", "V5.1.0"})
    void recognisedWrapperVariationsNormalizeWithoutChangingSettingsOrInput(String pack) throws Exception {
        ObjectNode legacy = (ObjectNode) json.readTree(fixture("populated_module.json"));
        legacy.put("version", pack).put("applicationPackageName", "org.example.imported");
        ObjectNode canonical = (ObjectNode) json.readTree(ModelTemplate.create(
                ComponentIO.validatePersistedModuleJson(legacy.toString(), "wrapper baseline", false)));
        ObjectNode properties = (ObjectNode) canonical.path("studio").path("properties");
        properties.put("applicationPackageName", "org.example.imported").put("flowStartupType", "AUTOMATIC")
                .put("h2DbPortNumber", "12451").put("h2WebPortNumber", "12452").put("useEmbeddedH2", true);
        var report = json.createArrayNode().add("Review existing integration wiring.");
        properties.withObject("/ikasanRuntimeImport").put("referenceMarker", "preserve-me").set("review", report);
        var expectedModule = ComponentIO.validatePersistedModuleJson(canonical.toString(), "standard wrapper", false);
        var expected = json.readTree(ModelTemplate.create(expectedModule));
        ObjectNode reconstructed = canonical.deepCopy();
        ObjectNode studio = (ObjectNode) reconstructed.path("studio");
        ObjectNode settings = (ObjectNode) studio.path("properties");
        studio.put("metaPack", pack.toLowerCase(java.util.Locale.ROOT));
        for (String key : java.util.List.of("applicationPackageName", "flowStartupType", "h2DbPortNumber", "h2WebPortNumber", "useEmbeddedH2"))
            studio.set(key, settings.remove(key));
        studio.set("review", ((ObjectNode) settings.path("ikasanRuntimeImport")).remove("review"));
        String source = reconstructed.toString();
        IkasanModelDocuments.toLegacy(reconstructed);
        assertThat(reconstructed.toString()).isEqualTo(source);
        var loaded = ComponentIO.validatePersistedModuleJson(source, "reconstructed wrapper", false);
        assertThat(json.readTree(ModelTemplate.create(loaded))).isEqualTo(expected);
        assertThat(PropertiesTemplate.create(loaded)).isEqualTo(PropertiesTemplate.create(expectedModule));
        assertThat(loaded.getVersion()).isEqualTo(pack);
        assertThat(loaded.getApplicationPackageName()).isEqualTo("org.example.imported");
        var saved = json.readTree(ModelTemplate.create(loaded));
        assertThat(saved.at("/studio/properties/ikasanRuntimeImport/review")).isEqualTo(report);
        assertThat(saved.at("/studio/properties/ikasanRuntimeImport/referenceMarker").asText()).isEqualTo("preserve-me");
        assertThat(json.readTree(ModelTemplate.create(ComponentIO.validatePersistedModuleJson(saved.toString(), "normalized reload", false))))
                .isEqualTo(saved);
    }

    @Test void wrapperNormalizationRejectsConflictsAndUnknownFieldsWithoutOverwriting() throws Exception {
        ObjectNode baseline = (ObjectNode) json.readTree(ModelTemplate.create(
                ComponentIO.validatePersistedModuleJson(fixture("populated_module.json"), "baseline", false)));
        for (String key : java.util.List.of("applicationPackageName", "flowStartupType", "h2DbPortNumber", "h2WebPortNumber", "useEmbeddedH2")) {
            ObjectNode input = baseline.deepCopy();
            ((ObjectNode) input.path("studio").path("properties")).put(key, "existing-setting");
            ((ObjectNode) input.path("studio")).put(key, "different-setting");
            String source = input.toString();
            assertThatThrownBy(() -> IkasanModelDocuments.toLegacy(input)).hasMessageContaining("Conflicting Studio metadata field " + key)
                    .hasMessageNotContaining("existing-setting").hasMessageNotContaining("different-setting");
            assertThat(input.toString()).isEqualTo(source);
        }
        ObjectNode equalSettings = baseline.deepCopy();
        ((ObjectNode) equalSettings.path("studio").path("properties")).put("h2DbPortNumber", "12451");
        ((ObjectNode) equalSettings.path("studio")).put("h2DbPortNumber", "12451");
        assertThat(IkasanModelDocuments.toLegacy(equalSettings).path("h2DbPortNumber").asText()).isEqualTo("12451");
        ObjectNode input = baseline.deepCopy();
        var studio = (ObjectNode) input.path("studio");
        var properties = (ObjectNode) studio.path("properties");
        var report = json.createArrayNode().add("existing-report");
        properties.withObject("/ikasanRuntimeImport").set("review", report);
        studio.set("review", report.deepCopy());
        // Equal duplicates are safe; differing reports must never overwrite each other.
        assertThat(IkasanModelDocuments.toLegacy(input).path("ikasanRuntimeImport").path("review")).isEqualTo(report);
        studio.putArray("review").add("different-report");
        assertThatThrownBy(() -> IkasanModelDocuments.toLegacy(input)).hasMessageContaining("Conflicting Studio metadata field review");
        studio.remove("review");
        studio.put("unknownFutureMetadata", true);
        assertThatThrownBy(() -> IkasanModelDocuments.toLegacy(input)).hasMessageContaining("Unknown document metadata field unknownFutureMetadata");
        studio.remove("unknownFutureMetadata");
        studio.put("metaPack", "v4.1.3");
        assertThatThrownBy(() -> IkasanModelDocuments.toLegacy(input)).hasMessageContaining("selected meta-pack");
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
