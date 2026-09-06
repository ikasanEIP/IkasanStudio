package org.ikasan.studio.core.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.persistence.json.StudioJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generates the local, tool-neutral contract used by coding assistants working in a Studio project. */
public final class AiProjectContractGenerator {
    public static final String CONTRACT_VERSION = "1";

    private AiProjectContractGenerator() { }

    public static String agentsGuide() {
        return """
                # Ikasan Studio agent instructions

                This project is managed by Ikasan Studio. Before editing `generated/src/main/model/model.json`,
                read `generated/IKASAN_STUDIO.md` and
                `generated/src/main/model/component-catalogue.json`.

                `model.json` is the version-neutral source of truth. Make minimal changes, preserve unknown
                fields, validate the result, and let Ikasan Studio regenerate its owned files. Do not directly
                edit files under `generated/` other than `model.json`. Developer-owned implementations belong
                under `user/` and must never be overwritten without explicit permission.
                """;
    }

    public static String studioGuide(String metapackVersion) {
        return """
                # Working with Ikasan Studio using an AI coding assistant

                This is an offline contract for both developers and coding agents. It does not require an MCP
                server, network access, or an external AI service. The selected meta-pack is `%s`.

                ## Sources of truth

                - `src/main/model/model.json` (relative to this generated module) is the editable Studio model.
                - `src/main/model/model.schema.json` describes its stable JSON shape.
                - `src/main/model/component-catalogue.json` is generated from the selected meta-pack and lists
                  valid component keys, roles, properties, defaults, types and choices.
                - Java, Maven and configuration files in `generated/` are Studio-owned derived output.
                - Files in `user/` are developer-owned. Never replace them without explicit confirmation.

                ## Safe editing workflow

                1. Read the current model and component catalogue; never guess property names.
                2. Make the smallest possible edit and preserve fields you do not understand.
                3. Give every flow and component a unique name within its scope.
                4. A flow has one `consumer`; ordered body components live in `flowElements`.
                5. Keep `transitions` consistent with that order. A normal edge is
                   `{ "from": "sourceName", "to": "targetName", "name": "default" }`.
                6. Use the catalogue `key` as `additionalKey` whenever the catalogue supplies one. Retain the
                   catalogue's `componentType` and `implementingClass` exactly.
                7. Reopen or reload the model in Studio. Studio validates external JSON before accepting it,
                   preserves rejected content, and maintains last-known-good backups.
                8. Regenerate through Studio and compile the project before considering the change complete.

                JSON Schema checks structure, but Studio performs additional semantic checks including
                component lookup, required properties, transitions, route integrity and meta-pack compatibility.
                A syntactically valid JSON document is not necessarily a valid Ikasan module.
                """.formatted(metapackVersion);
    }

    public static String modelSchema() {
        return """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$id": "https://ikasan.org/studio/model.schema.json",
                  "title": "Ikasan Studio module model",
                  "type": "object",
                  "required": ["applicationPackageName", "name", "version", "flows"],
                  "properties": {
                    "applicationPackageName": { "type": "string", "minLength": 1 },
                    "flowStartupType": { "type": "string" },
                    "h2DbPortNumber": { "type": ["string", "integer"] },
                    "h2WebPortNumber": { "type": ["string", "integer"] },
                    "name": { "type": "string", "minLength": 1 },
                    "port": { "type": ["string", "integer"] },
                    "useEmbeddedH2": { "type": "boolean" },
                    "version": { "type": "string", "minLength": 1 },
                    "flows": { "type": "array", "items": { "$ref": "#/$defs/flow" } }
                  },
                  "additionalProperties": true,
                  "$defs": {
                    "component": {
                      "type": "object",
                      "required": ["componentName", "componentType", "implementingClass"],
                      "properties": {
                        "componentName": { "type": "string", "minLength": 1 },
                        "componentType": { "type": "string", "minLength": 1 },
                        "implementingClass": { "type": "string" },
                        "additionalKey": { "type": "string" }
                      },
                      "additionalProperties": true
                    },
                    "transition": {
                      "type": "object",
                      "required": ["from", "to", "name"],
                      "properties": {
                        "from": { "type": "string", "minLength": 1 },
                        "to": { "type": "string", "minLength": 1 },
                        "name": { "type": "string", "minLength": 1 }
                      },
                      "additionalProperties": false
                    },
                    "flow": {
                      "type": "object",
                      "required": ["name", "consumer", "transitions", "flowElements"],
                      "properties": {
                        "name": { "type": "string", "minLength": 1 },
                        "consumer": { "$ref": "#/$defs/component" },
                        "transitions": { "type": "array", "items": { "$ref": "#/$defs/transition" } },
                        "flowElements": { "type": "array", "items": { "$ref": "#/$defs/component" } },
                        "exceptionResolver": { "$ref": "#/$defs/component" }
                      },
                      "additionalProperties": true
                    }
                  }
                }
                """;
    }

    public static String componentCatalogue(String metapackVersion)
            throws StudioBuildException, JsonProcessingException {
        List<Map<String, Object>> components = new ArrayList<>();
        ComponentLibrary.getIkasanComponents(metapackVersion).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> components.add(component(entry.getKey(), entry.getValue())));
        Map<String, Object> catalogue = new LinkedHashMap<>();
        catalogue.put("contractVersion", CONTRACT_VERSION);
        catalogue.put("metapackVersion", metapackVersion);
        catalogue.put("components", components);
        return StudioJson.newObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(catalogue) + "\n";
    }

    private static Map<String, Object> component(String key, ComponentMeta meta) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("name", meta.getName());
        result.put("role", meta.getComponentTypeMeta() == null ? null : meta.getComponentTypeMeta().getComponentShortType());
        result.put("componentType", meta.getComponentType());
        result.put("implementingClass", meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) result.put("additionalKey", meta.getAdditionalKey());
        if (meta.getExpectedInputTypes() != null) result.put("acceptedInputTypes", meta.getExpectedInputTypes());
        if (meta.getProducedOutputType() != null) result.put("producedOutputType", meta.getProducedOutputType());
        List<Map<String, Object>> properties = new ArrayList<>();
        meta.getAllowableProperties().forEach((name, property) -> properties.add(property(name, property)));
        result.put("properties", properties);
        return result;
    }

    private static Map<String, Object> property(String name, ComponentPropertyMeta meta) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("required", meta.isMandatory());
        result.put("type", meta.getPropertyDataType() == null ? "java.lang.String" : meta.getPropertyDataType().getName());
        if (meta.getUsageDataType() != null && !meta.getUsageDataType().isBlank()) result.put("usageType", meta.getUsageDataType());
        if (meta.getDefaultValue() != null) result.put("default", meta.getDefaultValue());
        if (meta.getChoices() != null && !meta.getChoices().isEmpty()) result.put("choices", meta.getChoices());
        if (meta.isHiddenProperty()) result.put("hidden", true);
        return result;
    }
}
