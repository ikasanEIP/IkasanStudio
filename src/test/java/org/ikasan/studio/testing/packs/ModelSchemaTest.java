package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.ai.LiveModelSnapshot;
import org.ikasan.studio.core.ai.ModelProposal;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generated model.schema.json is what an AI or an editor checks a model against. It used to reject models Studio
 * itself writes: it required a consumer, transitions and flowElements on every flow (an empty or half-built flow
 * persists only the keys it has) and typed exceptionResolver as a component (it is a map of exception to resolution).
 * Verified independently with networknt json-schema-validator; this test carries a small subset validator so it needs
 * no extra dependency.
 */
@ExtendWith(SharedResourceExtension.class)
@org.junit.jupiter.api.Tag("packs")
class ModelSchemaTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void everyModelShapeStudioWritesValidatesAgainstTheSchema(String pack) throws Exception {
        JsonNode schema = JSON.readTree(AiProjectContractGenerator.modelSchema());
        Module live = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        String ops = """
                [{"type":"addFlow","flow":"Empty"},
                 {"type":"addFlow","flow":"ConsumerOnly"},
                 {"type":"addComponent","flow":"ConsumerOnly","key":"Event Generating Consumer","name":"In"},
                 {"type":"addFlow","flow":"BodyOnly"},
                 {"type":"addComponent","flow":"BodyOnly","key":"Converter","name":"Conv"},
                 {"type":"addFlow","flow":"Resolved"},
                 {"type":"setExceptionResolution","flow":"Resolved","exception":"java.io.IOException","action":"excludeEvent"},
                 {"type":"setExceptionResolution","flow":"Resolved","exception":"java.lang.Exception","action":"retry","properties":{"delay":"1000","interval":"3"}},
                 {"type":"addFlow","flow":"Complete"},
                 {"type":"addComponent","flow":"Complete","key":"Event Generating Consumer","name":"Start"},
                 {"type":"addComponent","flow":"Complete","key":"Dev Null Producer","name":"End"},
                 {"type":"addFlow","flow":"Routed"},
                 {"type":"addComponent","flow":"Routed","key":"Event Generating Consumer","name":"RIn"},
                 {"type":"addComponent","flow":"Routed","key":"Multi Recipient Router","name":"Split"},
                 {"type":"configureRoutes","flow":"Routed","component":"Split","names":["a","b"]},
                 {"type":"addComponent","flow":"Routed","key":"Dev Null Producer","name":"SinkA","route":["a"]},
                 {"type":"addComponent","flow":"Routed","key":"Dev Null Producer","name":"SinkB","route":["b"]}]""";
        ModelProposal.changes(live, ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree(ops))).apply();

        List<String> errors = new ArrayList<>();
        validate(schema, JSON.readTree(ComponentIO.toValidatedModuleJson(live)), schema, "$", errors);
        assertThat(errors).as(pack + " model written by Studio").isEmpty();
    }

    @Test
    void theSchemaStillRejectsGenuinelyDamagedModels() throws Exception {
        JsonNode schema = JSON.readTree(AiProjectContractGenerator.modelSchema());
        for (String damaged : new String[]{
                "{\"applicationPackageName\":\"a.b\",\"name\":\"m\",\"version\":\"V4.1.6\"}",
                "{\"applicationPackageName\":\"a.b\",\"name\":\"m\",\"version\":\"V4.1.6\",\"flows\":[{}]}",
                "{\"applicationPackageName\":\"a.b\",\"name\":\"m\",\"version\":\"V4.1.6\",\"flows\":[{\"name\":\"f\",\"exceptionResolver\":{\"x\":\"not an object\"}}]}",
                "{\"applicationPackageName\":\"a.b\",\"name\":\"m\",\"version\":\"V4.1.6\",\"flows\":[{\"name\":\"f\",\"exceptionResolver\":{\"x\":{\"action\":\"retry\"}}}]}",
                "{\"applicationPackageName\":\"a.b\",\"name\":\"m\",\"version\":\"V4.1.6\",\"flows\":[{\"name\":\"f\",\"transitions\":[{\"from\":\"a\"}]}]}"}) {
            List<String> errors = new ArrayList<>();
            validate(schema, JSON.readTree(damaged), schema, "$", errors);
            assertThat(errors).as(damaged).isNotEmpty();
        }
    }

    /** A deliberately small JSON Schema subset: $ref, type, required, properties, items, additionalProperties, minLength. */
    private static void validate(JsonNode root, JsonNode node, JsonNode schema, String path, List<String> errors) {
        if (schema.has("$ref")) {
            JsonNode target = root;
            for (String part : schema.get("$ref").asText().substring(2).split("/")) target = target.path(part);
            validate(root, node, target, path, errors);
            return;
        }
        if (schema.has("type") && !typeMatches(schema.get("type"), node)) { errors.add(path + ": wrong type"); return; }
        if (schema.has("minLength") && node.isTextual() && node.asText().length() < schema.get("minLength").asInt()) errors.add(path + ": too short");
        if (node.isObject()) {
            for (JsonNode required : schema.path("required")) if (!node.has(required.asText())) errors.add(path + ": missing " + required.asText());
            var properties = schema.path("properties");
            for (var field : node.properties()) {
                if (properties.has(field.getKey())) validate(root, field.getValue(), properties.get(field.getKey()), path + "." + field.getKey(), errors);
                else if (schema.path("additionalProperties").isObject()) validate(root, field.getValue(), schema.get("additionalProperties"), path + "." + field.getKey(), errors);
                else if (schema.path("additionalProperties").isBoolean() && !schema.get("additionalProperties").asBoolean()) errors.add(path + ": unexpected " + field.getKey());
            }
        }
        if (node.isArray() && schema.has("items")) for (int i = 0; i < node.size(); i++) validate(root, node.get(i), schema.get("items"), path + "[" + i + "]", errors);
    }

    private static boolean typeMatches(JsonNode type, JsonNode node) {
        if (type.isArray()) { for (JsonNode t : type) if (typeMatches(t, node)) return true; return false; }
        return switch (type.asText()) {
            case "object" -> node.isObject(); case "array" -> node.isArray(); case "string" -> node.isTextual();
            case "integer" -> node.isIntegralNumber(); case "boolean" -> node.isBoolean(); case "number" -> node.isNumber();
            default -> true;
        };
    }
}
