package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.util.Map.Entry;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;

/** Independent API inventory; see docs/RuntimeImportConfigurationAudit.md for sources and limitations. */
@Tag("packs")
class RuntimeImportConfigurationCoverageTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void everyAuditedRuntimeFieldHasAnExplicitDispositionAndCompatibleMapping(String pack) throws Exception {
        JsonNode inventory;
        try (InputStream input = getClass().getResourceAsStream("/studio/runtime-import/configuration-fields.json")) {
            assertThat(input).isNotNull();
            inventory = new ObjectMapper().readTree(input);
        }
        Set<String> concreteComponents = new HashSet<>();
        for (ComponentMeta meta : ComponentLibrary.getPaletteComponentList(pack)) {
            String configClass = meta.getRuntimeConfigurationClass();
            ComponentPropertyMeta configuration = meta.getAllowableProperties().get("configuration");
            if (configClass == null && configuration != null) configClass = configuration.getUsageDataType();
            if (configClass == null || configClass.equals("configurationDefined")) continue;
            concreteComponents.add(meta.getName());
            JsonNode contract = inventory.path(meta.getName());
            assertThat(contract.isMissingNode()).as("Audited configuration for %s/%s", pack, meta.getName()).isFalse();
            assertThat(configClass).isEqualTo(contract.path("class").asText());
            JsonNode fields = contract.path("fields");
            Set<String> unaccounted = new HashSet<>();
            fields.fieldNames().forEachRemaining(unaccounted::add);
            for (String property : meta.getRuntimeConfigurationProperties()) {
                String parameter = meta.runtimeConfigurationName(property);
                assertThat(fields.has(parameter)).as("%s/%s/%s is a real runtime field", pack, meta.getName(), parameter).isTrue();
                Class<?> type = meta.getAllowableProperties().get(property).getPropertyDataType();
                assertThat(boxed(type.getSimpleName())).as("%s/%s/%s type", pack, meta.getName(), parameter)
                        .isEqualTo(boxed(fields.path(parameter).asText()));
                assertThat(meta.getRuntimeConfigurationDefaults()).doesNotContainKey(parameter);
                unaccounted.remove(parameter);
            }
            for (Entry<String,JsonNode> entry : meta.getRuntimeConfigurationDefaults().entrySet()) {
                assertThat(fields.has(entry.getKey())).isTrue();
                assertThat(matches(fields.path(entry.getKey()).asText(), entry.getValue()))
                        .as("%s/%s/%s default type", pack, meta.getName(), entry.getKey()).isTrue();
                unaccounted.remove(entry.getKey());
            }
            assertThat(unaccounted).as("Explicit unsupported fields for %s/%s", pack, meta.getName())
                    .containsExactlyInAnyOrderElementsOf(unsupported(meta.getName()));
        }
        Set<String> expected = new HashSet<>();
        inventory.fieldNames().forEachRemaining(expected::add);
        assertThat(concreteComponents).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Set<String> unsupported(String component) {
        return switch (component) {
            case "Scheduled Consumer" -> Set.of("jobName", "jobGroupName", "description");
            case "Local File Consumer" -> Set.of("jobName", "jobGroupName", "description",
                    "filePath", "fileNameSpelExpression", "filePathSpelExpression");
            case "FTP Consumer" -> Set.of("jobName", "jobGroupName", "description", "sourceDirectoryURLFactory");
            case "SFTP Consumer" -> Set.of("jobName", "jobGroupName", "description",
                    "sourceDirectoryURLFactory", "privateKeyPassphrase");
            case "FTP Producer" -> Set.of("passwordFilePath");
            case "SFTP Producer" -> Set.of("privateKeyPassphrase");
            default -> Set.of();
        };
    }

    private static String boxed(String type) {
        return switch (type) {
            case "boolean" -> "Boolean";
            case "int" -> "Integer";
            case "long" -> "Long";
            default -> type;
        };
    }

    private static boolean matches(String type, JsonNode value) {
        return switch (boxed(type)) {
            case "Boolean" -> value.isBoolean();
            case "Integer" -> value.isIntegralNumber() && value.canConvertToInt();
            case "Long" -> value.isIntegralNumber() && value.canConvertToLong();
            case "String" -> value.isTextual();
            case "List" -> value.isArray();
            case "Map" -> value.isObject();
            default -> false;
        };
    }
}
