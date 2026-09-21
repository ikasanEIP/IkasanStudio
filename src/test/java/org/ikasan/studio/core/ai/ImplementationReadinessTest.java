package org.ikasan.studio.core.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import static org.assertj.core.api.Assertions.*;

class ImplementationReadinessTest {
    @TempDir Path root;
    private com.fasterxml.jackson.databind.JsonNode model() throws Exception {
        return StudioJson.newObjectMapper().readTree("""
                {"applicationPackageName":"org.example","flows":[{"name":"Demo02 Order Audit and Fulfilment",
                "flowElements":[{"componentName":"Create Accepted Order","userImplementedClassName":"CreateAcceptedOrder"}]}]}
                """);
    }
    private void source(String text) throws Exception {
        Path file = root.resolve("user/src/main/java/org/example/demo02orderauditandfulfilment/CreateAcceptedOrder.java");
        Files.createDirectories(file.getParent()); Files.writeString(file, text);
    }
    @Test void identifiesReportedConverterAndLocation() throws Exception {
        source("class CreateAcceptedOrder {\n Object convert(Object payload) {\nthrow new UnsupportedOperationException(\"Conversion has not been implemented\");\n}}\n");
        var report = ImplementationReadiness.scan(root, model());
        assertThat(report.runtimeVerified()).isFalse();
        assertThat(report.findings()).hasSize(1);
        var finding = report.findings().get(0);
        assertThat(finding.code()).isEqualTo("THROWING_STUB");
        assertThat(finding.line()).isEqualTo(3);
        assertThat(finding.component()).isEqualTo("Create Accepted Order");
    }
    @Test void ignoresCommentedThrowAndDoesNotClaimRuntimeSuccess() throws Exception {
        source("// throw new UnsupportedOperationException(\"Conversion has not been implemented\");\nclass CreateAcceptedOrder {}\n");
        var report = ImplementationReadiness.scan(root, model());
        assertThat(report.findings()).isEmpty();
        assertThat(report.runtimeVerified()).isFalse();
        assertThat(report.scope()).contains("not proof");
    }
    @Test void reportsMissingSourceAndMarksTodoAsReviewRatherThanDefiniteFailure() throws Exception {
        assertThat(ImplementationReadiness.scan(root, model()).findings().get(0).code()).isEqualTo("MISSING_SOURCE");
        source("//@TODO implement your producer logic here\n");
        assertThat(ImplementationReadiness.scan(root, model()).findings().get(0).code()).isEqualTo("REVIEW_SCAFFOLD");
    }
}
