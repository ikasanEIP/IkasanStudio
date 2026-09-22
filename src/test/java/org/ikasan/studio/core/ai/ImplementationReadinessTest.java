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
    /** The generated stubs use several TODO wordings; recognising only two let the Translator and Generic Consumer through. */
    @Test void anyCommentOnlyTodoIsAReviewScaffold() throws Exception {
        for (String todo : new String[]{"// TODO: Update the mutable payload in place.", "//@TODO replace this scheduling with your real logic",
                "// TODO review the default body above", "    //   TODO something"}) {
            source("class CreateAcceptedOrder {\n " + todo + "\n}\n");
            var findings = ImplementationReadiness.scan(root, model()).findings();
            assertThat(findings).as(todo).hasSize(1);
            assertThat(findings.get(0).code()).isEqualTo("REVIEW_SCAFFOLD");
            assertThat(findings.get(0).line()).isEqualTo(2);
        }
        // Javadoc and ordinary comments that merely mention the word are not markers.
        source("/** Handles the TODO list. */\nclass CreateAcceptedOrder { // not a marker\n}\n");
        assertThat(ImplementationReadiness.scan(root, model()).findings()).isEmpty();
    }

    /** The converter stub has a TODO directly above its throw, and should be reported once, as the stronger finding. */
    @Test void aTodoDirectlyAboveAThrowingStubIsReportedOnce() throws Exception {
        source("class CreateAcceptedOrder {\n Object convert(Object payload) {\n"
                + "// TODO Implement the conversion. The target may be an interface.\n"
                + "throw new UnsupportedOperationException(\"Conversion has not been implemented\");\n}}\n");
        var findings = ImplementationReadiness.scan(root, model()).findings();
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).code()).isEqualTo("THROWING_STUB");
        assertThat(findings.get(0).line()).isEqualTo(4);
    }
}
