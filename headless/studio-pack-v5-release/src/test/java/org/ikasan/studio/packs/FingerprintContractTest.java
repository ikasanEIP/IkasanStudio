package org.ikasan.studio.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FingerprintContractTest {
    @TempDir Path temporary;

    @Test void releasedEmailTestsAcceptNativeModelsButStillDetectChangedConfiguration() throws Exception {
        var mapper = new ObjectMapper();
        for (String scenario : List.of("smtp", "smtp-generated", "smtp-configured")) {
            var files = RuntimeTransportFixtureTest.render(scenario);
            Path output = Files.createDirectories(temporary.resolve(scenario));
            Path source = output.resolve("FlowTestSupportFingerprint.java");
            Files.writeString(source, files.get("src/test/java/org/ikasan/studio/flowtests/support/FlowTestSupportFingerprint.java"));
            var dependencies = List.of(ObjectMapper.class, com.fasterxml.jackson.core.JsonParser.class,
                    com.fasterxml.jackson.annotation.JsonProperty.class).stream().map(type -> {
                try { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }).toList();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-cp", String.join(java.io.File.pathSeparator, dependencies), "-d", output.toString(), source.toString()));
            try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, getClass().getClassLoader())) {
                var method = loader.loadClass("org.ikasan.studio.flowtests.support.FlowTestSupportFingerprint")
                        .getMethod("fingerprint", String.class, Set.class);
                var wiring = mapper.readTree(files.get("src/main/resources/studio-flow-test-wiring.json"));
                Set<String> fields = new HashSet<>();
                wiring.path("modelFields").forEach(value -> fields.add(value.asText()));
                String saved = files.get("generated/src/main/model/model.json");
                String expected = wiring.path("modelFingerprint").asText();
                assertEquals(expected, method.invoke(null, saved, fields), scenario);
                var changed = mapper.readTree(saved);
                boolean modified = false;
                for (var configuration : changed.path("configuration")) for (var parameter : configuration.path("parameters")) {
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
