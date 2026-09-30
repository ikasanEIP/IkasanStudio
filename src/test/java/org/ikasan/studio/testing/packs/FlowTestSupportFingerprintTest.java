package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.FlowTestSupportFingerprint;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestSupportFingerprintTest {
    private static final String MODEL = """
            {"name":"module","version":"V3.3.9","flows":[{"name":"flow",
            "consumer":{"componentName":"input","implementingClass":"Consumer","url":"vm://test"},
            "flowElements":[{"componentName":"mail","implementingClass":"Producer","port":25}]}]}
            """;
    private String fingerprint(String json) throws Exception {
        return FlowTestSupportFingerprint.fingerprint(json, Set.of("url", "port"));
    }

    @Test void ignoresFormattingFieldOrderVersionAndPresentation() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(MODEL);
        tree.put("version", "V4.1.6");
        tree.put("canvasX", 200);
        String formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        assertEquals(fingerprint(MODEL), fingerprint(formatted));
        assertEquals(fingerprint(MODEL), fingerprint(MODEL.replace(
                "\"componentName\":\"input\",\"implementingClass\":\"Consumer\"",
                "\"implementingClass\":\"Consumer\",\"componentName\":\"input\"")));
    }

    @Test void detectsRenamesEndpointChangesAndAddedComponents() throws Exception {
        for (String changed : new String[]{MODEL.replace("mail", "warehouse"), MODEL.replace("vm://test", "vm://other"),
                MODEL.replace("25", "26"), MODEL.replace("Producer", "OtherProducer"),
                MODEL.replace("\"flowElements\":[", "\"flowElements\":[{\"componentName\":\"extra\"},")}) {
            assertNotEquals(fingerprint(MODEL), fingerprint(changed));
        }
        assertThrows(java.io.IOException.class, () -> fingerprint("{}"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void emittedBaselineMatchesAndLegacySupportRequiresRefresh(String version) throws Exception {
        var flow = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        String support = scaffold.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertFalse(FlowTestScaffold.supportNeedsRefresh(support, module));
        assertTrue(FlowTestScaffold.supportNeedsRefresh("old support", module));
        assertTrue(support.indexOf("FlowTestSupportFingerprint.verify(") < support.indexOf("application.run(arguments)"));
        assertTrue(support.contains("Review module-test.properties or scenario overrides"));
        // The runtime uses the same projection/hash implementation as Studio.
        String helper = scaffold.files().get("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/FlowTestSupportFingerprint.java");
        String core = Files.readString(Path.of("headless/studio-generator/src/main/java/org/ikasan/studio/core/generator/FlowTestSupportFingerprint.java"));
        assertTrue(helper.startsWith(core.replace("package org.ikasan.studio.core.generator;",
                "package org.ikasan.studio.flowtests.support;").stripTrailing().replaceAll("}\\z", "")));
    }
}
