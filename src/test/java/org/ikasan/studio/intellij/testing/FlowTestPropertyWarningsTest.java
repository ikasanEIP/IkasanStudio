package org.ikasan.studio.intellij.testing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FlowTestPropertyWarningsTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void detectsSupportRenameAndRefreshIndependentlyOfProperties(String version,
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path root) throws Exception {
        var flow = org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        var model = root.resolve("generated/src/main/model/model.json");
        var support = root.resolve(org.ikasan.studio.core.generator.FlowTestScaffold.SUPPORT_PATH);
        java.nio.file.Files.createDirectories(model.getParent());
        java.nio.file.Files.createDirectories(support.getParent());
        java.nio.file.Files.writeString(model, org.ikasan.studio.core.generator.ModelTemplate.create(module));
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse(); // No tests yet.
        writeSupport(support, module);
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse();
        flow.setName("Renamed flow");
        java.nio.file.Files.writeString(model, org.ikasan.studio.core.generator.ModelTemplate.create(module));
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse(); // Model edits no longer stale generic support.
        java.nio.file.Files.writeString(support, "legacy support");
        var stale = FlowTestPropertyWarnings.inspect(root);
        assertThat(stale.staleSupport()).isTrue();
        assertThat(stale.brokenReferences()).isEmpty();
        var props = root.resolve(org.ikasan.studio.core.generator.FlowTestScaffold.TEST_PROPERTIES_PATH);
        var app = root.resolve("generated/src/main/resources/application.properties");
        java.nio.file.Files.createDirectories(props.getParent());
        java.nio.file.Files.createDirectories(app.getParent());
        java.nio.file.Files.writeString(props, "test.jms.broker-url=${jms.old.url}\n");
        java.nio.file.Files.writeString(app, "jms.new.url=local\n");
        assertThat(FlowTestPropertyWarnings.inspect(root).brokenReferences()).hasSize(1);
        writeSupport(support, module);
        assertThat(FlowTestPropertyWarnings.inspect(root).staleSupport()).isFalse();
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isTrue();
        java.nio.file.Files.writeString(props, "test.jms.broker-url=${jms.new.url}\n");
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse();
    }

    private static void writeSupport(java.nio.file.Path path,
            org.ikasan.studio.core.model.ikasan.instance.Module module) throws Exception {
        java.nio.file.Files.writeString(path, "FlowTestFailureDiagnostics.contextualise(\nprotected void assertExpectedOutput(\nprotected final <I> void runBatches(\nFlowTestFailureCapture\nprotected final void runTest(TestScenario scenario) {}\nnew FileDeliveryBatchAssertions();\nprotected String formatOutputText(Object payload) {}\nWIRING_SCHEMA_VERSION = 1;\nSUPPORT_META_PACK = \""
                + module.getMetaVersion() + "\";");
    }

    @Test void reportsEveryStaleComponentReferenceWithoutExposingValues() throws Exception {
        assertThat(FlowTestPropertyWarnings.brokenReferences(
                "test.jms.broker-url=${jms.old.consumer.url}\ntest.password=${sftp.old.password}\n",
                "jms.new.consumer.url=secret\nsftp.new.password=secret\n"))
                .containsExactly("test.jms.broker-url → jms.old.consumer.url", "test.password → sftp.old.password");
    }

    @Test void ignoresCommentsDefaultsEnvironmentAndResolvedReferences() throws Exception {
        assertThat(FlowTestPropertyWarnings.brokenReferences(
                """
                # unused=${jms.old.url}
                a=${jms.current.url}
                b=${sftp.override}
                sftp.override=local
                c=${jms.optional.url:localhost}
                d=${BROKER_URL}
                """,
                "jms.current.url=local\n")).isEmpty();
    }

    @Test void acceptsContinuedPropertiesAndReportsMissingReferenceOnly() throws Exception {
        assertThat(FlowTestPropertyWarnings.brokenReferences(
                "test.jms.broker-url=\\\n  ${jms.renamed.url}\n", ""))
                .containsExactly("test.jms.broker-url → jms.renamed.url");
    }
}
