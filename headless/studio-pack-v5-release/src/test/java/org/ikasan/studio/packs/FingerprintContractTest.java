package org.ikasan.studio.packs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintContractTest {
    @TempDir Path temporary;

    @Test void releasedEmailTestsAcceptNativeModelsButStillDetectChangedConfiguration() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String scenario : List.of("smtp", "smtp-generated", "smtp-configured")) {
            Map<String,String> files = RuntimeTransportFixtureTest.render(scenario);
            Path output = Files.createDirectories(temporary.resolve(scenario));
            Path source = output.resolve("FlowTestSupportFingerprint.java");
            Files.writeString(source, files.get("src/test/java/org/ikasan/studio/flowtests/support/FlowTestSupportFingerprint.java"));
            List<String> dependencies = Stream.of(ObjectMapper.class, JsonParser.class,
                    JsonProperty.class).map(type -> {
                try { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }).toList();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-cp", String.join(File.pathSeparator, dependencies), "-d", output.toString(), source.toString()));
            try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toUri().toURL()}, getClass().getClassLoader())) {
                Method method = loader.loadClass("org.ikasan.studio.flowtests.support.FlowTestSupportFingerprint")
                        .getMethod("fingerprint", String.class, Set.class);
                JsonNode wiring = mapper.readTree(files.get("src/main/resources/studio-flow-test-wiring.json"));
                Set<String> fields = new HashSet<>();
                wiring.path("modelFields").forEach(value -> fields.add(value.asText()));
                String saved = files.get("generated/src/main/model/model.json");
                String expected = wiring.path("modelFingerprint").asText();
                assertEquals(expected, method.invoke(null, saved, fields), scenario);
                JsonNode changed = mapper.readTree(saved);
                boolean modified = false;
                for (JsonNode configuration : changed.path("configuration")) for (JsonNode parameter : configuration.path("parameters")) {
                    if (parameter.path("name").asText().equals("mailFrom")) {
                        ((ObjectNode) parameter).put("value", "different@example.test"); modified = true;
                    }
                }
                assertTrue(modified, "Fixture must include the REST mailFrom mapping");
                assertNotEquals(expected, method.invoke(null, changed.toString(), fields), scenario);
            }
        }
    }
}
