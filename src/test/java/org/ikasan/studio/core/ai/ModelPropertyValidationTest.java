package org.ikasan.studio.core.ai;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ModelPropertyValidationTest {
    @TempDir Path root;

    @org.junit.jupiter.api.Test
    void hidesCredentialsAndUnknownValuesInPersistedDiagnostics() throws Exception {
        var json = StudioJson.newObjectMapper();
        for (String key : java.util.List.of("password", "apiToken", "privateKey", "connectionUrl", "customProperty", "connectionTimeout"))
            assertThat(ModelPropertyValidation.safeDisplayValue(key, json.readTree("\"sensitive-value\""))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("properties", json.readTree("{\"password\":\"secret\"}"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("toRecipients", json.readTree("[{\"password\":\"secret\"}]"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("mailSmtpPort", json.readTree("-1"))).isEqualTo("-1");
        assertThat(ModelPropertyValidation.safeDisplayValue("password", json.readTree("1234"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("toRecipient", json.nullNode())).isEqualTo("(not set)");
    }


    private ObjectNode model(String pack) {
        var model = StudioJson.newObjectMapper().createObjectNode();
        model.put("version", pack).put("name", "Example").put("applicationPackageName", "org.example");
        model.putArray("flows").addObject().put("name", "Router flow").putArray("flowElements");
        return model;
    }
    private ObjectNode component(ObjectNode model, String key, String name) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(model.path("version").asText(), key);
        var value = ((com.fasterxml.jackson.databind.node.ArrayNode) model.path("flows").get(0).path("flowElements")).addObject();
        value.put("componentName", name).put("componentType", meta.getComponentType())
                .put("implementingClass", meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) value.put("additionalKey", meta.getAdditionalKey());
        return value;
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void reportsSavedInvalidRecipientsWithNavigationAndSafeValues(String pack) throws Exception {
        var model = model(pack);
        component(model, "Email Producer", "output 2").put("toRecipient", "bar.@bar.com")
                .put("emailFormat", "text/plain");
        component(model, "Email Producer", "output 3").putArray("toRecipients")
                .add("valid@example.com").add("bad..address@example.com");
        var findings = ImplementationReadiness.scan(root, model).findings();
        assertThat(findings).anySatisfy(f -> {
            assertThat(f.flow()).isEqualTo("Router flow");
            assertThat(f.component()).isEqualTo("output 2");
            assertThat(f.message()).startsWith("toRecipient:");
            assertThat(f.file()).isEqualTo("generated/src/main/model/model.json");
        });
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("toRecipients: Entry 2:"));
        assertThat(findings).anySatisfy(f -> {
            assertThat(f.target()).isEqualTo(ImplementationReadiness.Target.COMPONENT);
            assertThat(f.property()).isEqualTo("toRecipient");
            assertThat(f.displayValue()).isEqualTo("\"bar.@bar.com\"");
        });
        // The second producer omitted emailFormat; its mandatory text/plain default is valid.
        assertThat(findings).noneSatisfy(f -> assertThat(f.message()).startsWith("emailFormat:"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void checksRequiredConditionalTypesChoicesAndPatterns(String pack) throws Exception {
        var model = model(pack);
        model.put("flowStartupType", "invalid-startup-mode");
        var ftp = component(model, "FTP Consumer", "Receive");
        ftp.put("ftps", true).put("connectionTimeout", "secret-not-a-number").put("cronExpression", "daily");
        var email = component(model, "Email Producer", "Mail");
        email.put("emailFormat", "text/custom"); // Editable choice: valid.
        email.putArray("toRecipients"); // Empty list cannot satisfy a recipient requirement.
        var findings = ModelPropertyValidation.scan(model);
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("ftpsKeyStoreFilePath: A value is required"));
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("connectionTimeout: Value has an incompatible"));
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("cronExpression:"));
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("flowStartupType: Choose one of:"));
        assertThat(findings).anySatisfy(f -> assertThat(f.message()).startsWith("toRecipient: A value is required unless"));
        assertThat(findings).noneSatisfy(f -> assertThat(f.message()).startsWith("emailFormat:"));
        assertThat(findings.toString()).doesNotContain("secret-not-a-number");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void scanProjectChecksDocumentModelsAndExplicitZeroFalseRemainValid(String pack) throws Exception {
        var model = model(pack);
        component(model, "Email Producer", "Mail").put("toRecipient", "valid@example.com")
                .put("emailFormat", "text/plain").put("mailDebug", false).put("mailSmtpPort", 0);
        var documents = org.ikasan.studio.core.persistence.json.IkasanModelDocuments.fromLegacy(model);
        Path file = root.resolve("generated/src/main/model/model.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, documents.toString());
        assertThat(ImplementationReadiness.scanProject(root).findings())
                .noneSatisfy(f -> assertThat(f.component()).isEqualTo("Mail"));
        // Change the saved model, rather than relying on an editor edit to trigger validation.
        ((ObjectNode) model.path("flows").get(0).path("flowElements").get(0)).put("toRecipient", "bad.@example.com");
        Files.writeString(file, org.ikasan.studio.core.persistence.json.IkasanModelDocuments.fromLegacy(model).toString());
        assertThat(ImplementationReadiness.scanProject(root).findings())
                .anySatisfy(f -> assertThat(f.message()).startsWith("toRecipient:"));
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void checksExceptionActionPropertiesAndReportsUnresolvedMetadata(String pack) throws Exception {
        var model = model(pack);
        var flow = (ObjectNode) model.path("flows").get(0);
        var retry = flow.putObject("exceptionResolver").putObject("java.lang.RuntimeException");
        retry.put("theAction", "retry").putObject("properties").put("delay", "not-an-integer").put("interval", 0);
        var findings = ModelPropertyValidation.scan(model);
        assertThat(findings).anySatisfy(f -> {
            assertThat(f.component()).startsWith("Exception Resolver:");
            assertThat(f.message()).startsWith("delay: Value has an incompatible data type.");
        });
        assertThat(findings).noneSatisfy(f -> assertThat(f.message()).startsWith("interval:"));
        var unknown = component(model, "Email Producer", "Unknown");
        unknown.put("implementingClass", "not.a.KnownComponent").put("componentType", "not.a.KnownType");
        assertThat(ModelPropertyValidation.scan(model)).anySatisfy(f -> {
            assertThat(f.component()).isEqualTo("Unknown");
            assertThat(f.code()).isEqualTo("PROPERTIES_NOT_CHECKED");
        });
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void routerListsAcceptSavedArraysAndLegacyTextButKeepCardinalityAndNameRules(String pack) throws Exception {
        var model = model(pack);
        var router = component(model, "Single Recipient Router", "Either or Router");
        for (String text : List.of("odd,even", "odd, even", "route1,route2")) {
            router.put("routeNames", text);
            assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        }
        router.putArray("routeNames").add("odd").add("even");
        assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        for (List<String> names : List.of(List.of("odd"), List.of("odd", "odd"), List.of("odd", ""), List.of("odd", "bad-name"))) {
            var array = router.putArray("routeNames");
            names.forEach(array::add);
            assertThat(ModelPropertyValidation.scan(model)).anySatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        }
        var multi = component(model, "Multi Recipient Router", "Fan out");
        // Keep the single-recipient router valid when checking one-route multi-recipient input.
        router.putArray("routeNames").add("odd").add("even");
        multi.putArray("routeNames").add("odd");
        assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
    }

}
