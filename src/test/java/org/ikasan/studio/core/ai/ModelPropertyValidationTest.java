package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.ikasan.studio.core.ai.ImplementationReadiness.Finding;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.persistence.json.IkasanModelDocuments;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ModelPropertyValidationTest {
    @TempDir Path root;

    @Test
    void hidesCredentialsAndUnknownValuesInPersistedDiagnostics() throws Exception {
        ObjectMapper json = StudioJson.newObjectMapper();
        for (String key : List.of("password", "apiToken", "privateKey", "connectionUrl", "customProperty", "connectionTimeout"))
            assertThat(ModelPropertyValidation.safeDisplayValue(key, json.readTree("\"sensitive-value\""))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("properties", json.readTree("{\"password\":\"secret\"}"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("toRecipients", json.readTree("[{\"password\":\"secret\"}]"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("mailSmtpPort", json.readTree("-1"))).isEqualTo("-1");
        assertThat(ModelPropertyValidation.safeDisplayValue("password", json.readTree("1234"))).isEqualTo("(hidden)");
        assertThat(ModelPropertyValidation.safeDisplayValue("toRecipient", json.nullNode())).isEqualTo("(not set)");
    }


    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void semanticPackageRulesAreEnforcedInSavedModelReadiness(String pack) {
        ObjectNode model = model(pack);
        model.put("applicationPackageName", "com.class.app");
        assertThat(ModelPropertyValidation.scan(model)).anySatisfy(f -> {
            assertThat(f.target()).isEqualTo(ImplementationReadiness.Target.MODULE);
            assertThat(f.property()).isEqualTo("applicationPackageName");
        });
        model.put("applicationPackageName", "com.日本.app");
        assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.property()).isEqualTo("applicationPackageName"));
    }

    private ObjectNode model(String pack) {
        ObjectNode model = StudioJson.newObjectMapper().createObjectNode();
        model.put("version", pack).put("name", "Example").put("applicationPackageName", "org.example");
        model.putArray("flows").addObject().put("name", "Router flow").putArray("flowElements");
        return model;
    }
    private ObjectNode component(ObjectNode model, String key, String name) throws Exception {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(model.path("version").asText(), key);
        ObjectNode value = ((ArrayNode) model.path("flows").get(0).path("flowElements")).addObject();
        value.put("componentName", name).put("componentType", meta.getComponentType())
                .put("implementingClass", meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) value.put("additionalKey", meta.getAdditionalKey());
        return value;
    }

    @Test
    void sensitiveKeysIncludingLongRepeatedNamesAreRedactedWithoutBacktracking() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            TextNode value = new TextNode("never-display-this");
            for (String key : List.of("apiTOKEN", "customerSecret", "PASSWORD", "credentials", "key".repeat(50_000) + "\n"))
                assertThat(ModelPropertyValidation.safeDisplayValue(key, value)).isEqualTo("(hidden)");
        });
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void reportsSavedInvalidRecipientsWithNavigationAndSafeValues(String pack) throws Exception {
        ObjectNode model = model(pack);
        component(model, "Email Producer", "output 2").put("toRecipient", "bar.@bar.com")
                .put("emailFormat", "text/plain");
        component(model, "Email Producer", "output 3").putArray("toRecipients")
                .add("valid@example.com").add("bad..address@example.com");
        List<Finding> findings = ImplementationReadiness.scan(root, model).findings();
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
        ObjectNode model = model(pack);
        model.put("flowStartupType", "invalid-startup-mode");
        ObjectNode ftp = component(model, "FTP Consumer", "Receive");
        ftp.put("ftps", true).put("connectionTimeout", "secret-not-a-number").put("cronExpression", "daily");
        ObjectNode email = component(model, "Email Producer", "Mail");
        email.put("emailFormat", "text/custom"); // Editable choice: valid.
        email.putArray("toRecipients"); // Empty list cannot satisfy a recipient requirement.
        List<Finding> findings = ModelPropertyValidation.scan(model);
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
        ObjectNode model = model(pack);
        component(model, "Email Producer", "Mail").put("toRecipient", "valid@example.com")
                .put("emailFormat", "text/plain").put("mailDebug", false).put("mailSmtpPort", 0);
        ObjectNode documents = IkasanModelDocuments.fromLegacy(model);
        Path file = root.resolve("generated/src/main/model/model.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, documents.toString());
        assertThat(ImplementationReadiness.scanProject(root).findings())
                .noneSatisfy(f -> assertThat(f.component()).isEqualTo("Mail"));
        // Change the saved model, rather than relying on an editor edit to trigger validation.
        ((ObjectNode) model.path("flows").get(0).path("flowElements").get(0)).put("toRecipient", "bad.@example.com");
        Files.writeString(file, IkasanModelDocuments.fromLegacy(model).toString());
        assertThat(ImplementationReadiness.scanProject(root).findings())
                .anySatisfy(f -> assertThat(f.message()).startsWith("toRecipient:"));
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void checksExceptionActionPropertiesAndReportsUnresolvedMetadata(String pack) throws Exception {
        ObjectNode model = model(pack);
        ObjectNode flow = (ObjectNode) model.path("flows").get(0);
        ObjectNode retry = flow.putObject("exceptionResolver").putObject("java.lang.RuntimeException");
        retry.put("theAction", "retry").putObject("properties").put("delay", "not-an-integer").put("interval", 0);
        List<Finding> findings = ModelPropertyValidation.scan(model);
        assertThat(findings).anySatisfy(f -> {
            assertThat(f.component()).startsWith("Exception Resolver:");
            assertThat(f.message()).startsWith("delay: Value has an incompatible data type.");
        });
        assertThat(findings).noneSatisfy(f -> assertThat(f.message()).startsWith("interval:"));
        ObjectNode unknown = component(model, "Email Producer", "Unknown");
        unknown.put("implementingClass", "not.a.KnownComponent").put("componentType", "not.a.KnownType");
        assertThat(ModelPropertyValidation.scan(model)).anySatisfy(f -> {
            assertThat(f.component()).isEqualTo("Unknown");
            assertThat(f.code()).isEqualTo("PROPERTIES_NOT_CHECKED");
        });
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void routerListsAcceptSavedArraysAndLegacyTextButKeepCardinalityAndNameRules(String pack) throws Exception {
        ObjectNode model = model(pack);
        ObjectNode router = component(model, "Single Recipient Router", "Either or Router");
        for (String text : List.of("odd,even", "odd, even", "route1,route2")) {
            router.put("routeNames", text);
            assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        }
        router.putArray("routeNames").add("odd").add("even");
        assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        for (List<String> names : List.of(List.of("odd"), List.of("odd", "odd"), List.of("odd", ""), List.of("odd", "bad-name"))) {
            ArrayNode array = router.putArray("routeNames");
            names.forEach(array::add);
            assertThat(ModelPropertyValidation.scan(model)).anySatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
        }
        ObjectNode multi = component(model, "Multi Recipient Router", "Fan out");
        // Keep the single-recipient router valid when checking one-route multi-recipient input.
        router.putArray("routeNames").add("odd").add("even");
        multi.putArray("routeNames").add("odd");
        assertThat(ModelPropertyValidation.scan(model)).noneSatisfy(f -> assertThat(f.message()).startsWith("routeNames:"));
    }

}
