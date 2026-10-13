package org.ikasan.studio.core.ai;

import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;

class FlowTestPathValidationTest {
    @TempDir Path root;
    @Test void reportsLiteralAndConstantTyposAndWrongRolesAtTheirSourceLines() throws Exception {
        Path test = root.resolve("user-flow-tests/src/test/java/Example.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                class Example {
                    private static final String FLOW_NAME = "Router flow";
                    private static final String OUTPUT = "output2";
                    protected String getFlowName() { return FLOW_NAME; }
                    protected void defineExpectedPath(IkasanFlowTestRule harness) {
                        harness.producer("output1");
                        harness.producer(OUTPUT);
                        harness.converter("output 2");
                        harness.producer("output 2").producer("output 2");
                        // harness.producer("comment is not code");
                        String example = "harness.producer(\\"not code\\")";
                        harness.producer(dynamicName());
                        other.producer("not the harness");
                    }
                }
                """);
        var model = StudioJson.newObjectMapper().readTree("""
                {"flows":[{"name":"Router flow","flowElements":[
                  {"componentName":"output 1","componentType":"org.ikasan.spec.component.endpoint.Producer"},
                  {"componentName":"output 2","componentType":"org.ikasan.spec.component.endpoint.Producer"}]}]}
                """);
        var findings = ImplementationReadiness.scan(root, model).findings();
        assertThat(findings).hasSize(3);
        assertThat(findings).extracting(ImplementationReadiness.Finding::line).containsExactly(6, 7, 8);
        assertThat(findings.get(0).message()).contains("Did you mean 'output 1'?", "defineExpectedPath");
        assertThat(findings.get(1).message()).contains("Did you mean 'output 2'?");
        assertThat(findings.get(2).message()).contains("is a Producer", "uses converter");
        assertThat(findings).allSatisfy(f -> {
            assertThat(f.flow()).isEqualTo("Router flow");
            assertThat(f.file()).isEqualTo("user-flow-tests/src/test/java/Example.java");
        });
    }
    @Test void skipsShadowedConstantsAndChecksOnlyTheSelectedFlow() throws Exception {
        Path test = root.resolve("user-flow-tests/src/test/java/Example.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                class Example {
                    final String NAME = "mistyped";
                    protected String getFlowName() { return "First"; }
                    protected void defineExpectedPath(IkasanFlowTestRule path) {
                        String NAME = dynamicName();
                        path.producer(NAME);
                        path.producer("belongs to second");
                    }
                    void unrelated(IkasanFlowTestRule path) { path.producer("ignore helper"); }
                }
                """);
        var model = StudioJson.newObjectMapper().readTree("""
                {"flows":[{"name":"First","flowElements":[]},
                  {"name":"Second","flowElements":[{"componentName":"belongs to second","componentType":"Producer"}]}]}
                """);
        var findings = FlowTestPathValidation.scan(root, model);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).component()).isEqualTo("belongs to second");
        assertThat(findings.get(0).line()).isEqualTo(7);
    }
}
