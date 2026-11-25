package org.ikasan.studio.intellij.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.testing.FlowTestPropertyWarnings.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FlowTestPropertyWarningsTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void detectsSupportRenameAndRefreshIndependentlyOfProperties(String version,
            @TempDir Path root) throws Exception {
        Flow flow = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        Module module = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
        Path model = root.resolve("generated/src/main/model/model.json");
        Path support = root.resolve(FlowTestScaffold.SUPPORT_PATH);
        Files.createDirectories(model.getParent());
        Files.createDirectories(support.getParent());
        Files.writeString(model, ModelTemplate.create(module));
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse(); // No tests yet.
        writeSupport(support, module);
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse();
        flow.setName("Renamed flow");
        Files.writeString(model, ModelTemplate.create(module));
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse(); // Model edits no longer stale generic support.
        Files.writeString(support, "legacy support");
        Status stale = FlowTestPropertyWarnings.inspect(root);
        assertThat(stale.staleSupport()).isTrue();
        assertThat(stale.brokenReferences()).isEmpty();
        Path props = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Path app = root.resolve("generated/src/main/resources/application.properties");
        Files.createDirectories(props.getParent());
        Files.createDirectories(app.getParent());
        Files.writeString(props, "test.jms.broker-url=${jms.old.url}\n");
        Files.writeString(app, "jms.new.url=local\n");
        assertThat(FlowTestPropertyWarnings.inspect(root).brokenReferences()).hasSize(1);
        writeSupport(support, module);
        assertThat(FlowTestPropertyWarnings.inspect(root).staleSupport()).isFalse();
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isTrue();
        Files.writeString(props, "test.jms.broker-url=${jms.new.url}\n");
        assertThat(FlowTestPropertyWarnings.inspect(root).needsRefresh()).isFalse();
    }

    private static void writeSupport(Path path,
            Module module) throws Exception {
        Files.writeString(path, "FlowTestFailureDiagnostics.contextualise(\nprotected void assertExpectedOutput(\nprotected final <I> void runBatches(\nFlowTestFailureCapture\nprotected final void runTest(TestScenario scenario) {}\nnew FileDeliveryBatchAssertions();\nprotected String formatOutputText(Object payload) {}\nWIRING_SCHEMA_VERSION = 1;\nSUPPORT_META_PACK = \""
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
