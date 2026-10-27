package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.diagnostics.RuntimeImportReport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeImportReportTest {
    private static final String MODULE = """
        {"name":"PRIVATE_MODULE", "ikasanVersion":"4.1.6", "flows":[{
          "name":"PRIVATE_FLOW", "consumer":{"componentName":"PRIVATE_SOURCE","implementingClass":"private.COMPONENT"},
          "flowElements":[{"componentName":"PRIVATE_SOURCE","implementingClass":"private.COMPONENT"},
                          {"componentName":"PRIVATE_TARGET","implementingClass":"private.TARGET"}],
          "transitions":[{"from":"PRIVATE_SOURCE","to":"PRIVATE_TARGET","name":"PRIVATE_ROUTE"}]}]}
        """;

    @Test void countsRepeatedConsumerOnceAndNeverExportsNamesValuesOrUnknownClasses() {
        var report = new RuntimeImportReport();
        report.inspect(MODULE, """
            [{"configurationId":"PRIVATE_CONFIG", "parameters":[
               {"name":"PASSWORD", "value":"PRIVATE_PASSWORD"},
               {"name":"unset"}, {"name":"null", "value":null},
               {"name":"false", "value":false}, {"name":"zero", "value":0}]}]
            """);
        String text = report.render("1.0.0-beta.1", "262.123.4");
        assertTrue(text.contains("components: 2"));
        assertTrue(text.contains("unset parameters: 2"));
        assertTrue(text.contains("Issues: 0"));
        assertTrue(text.contains("1.0.0-beta.1"));
        assertFalse(text.contains("PRIVATE")); assertFalse(text.contains("PASSWORD"));
    }

    @Test void detectsCyclesMissingEndpointsAndDisconnectedNodesWithoutHanging() {
        var report = new RuntimeImportReport();
        report.inspect(MODULE.replace("\"to\":\"PRIVATE_TARGET\"", "\"to\":\"PRIVATE_SOURCE\""), "");
        String text = report.render(null, null);
        assertTrue(text.contains("CYCLIC_TOPOLOGY"));
        assertTrue(text.contains("DISCONNECTED_COMPONENTS"));
        assertTrue(text.contains("CONSUMER_HAS_INCOMING_TRANSITION"));
        var missing = new RuntimeImportReport();
        missing.inspect(MODULE.replace("\"to\":\"PRIVATE_TARGET\"", "\"to\":\"MISSING\""), "");
        assertTrue(missing.render(null, null).contains("TRANSITION_ENDPOINT_MISSING"));
    }

    @Test void malformedJsonIncludesCoordinatesWithoutParserExcerpts() {
        var report = new RuntimeImportReport();
        report.inspect("{\"PRIVATE_SECRET\": BAD}", "");
        String text = report.render("PRIVATE_VERSION", "PRIVATE_BUILD");
        assertTrue(text.contains("JSON_PARSE_FAILED")); assertTrue(text.contains("JSON line 1, column"));
        assertFalse(text.contains("PRIVATE")); assertFalse(text.contains("BAD"));
    }

    @Test void boundedIssuesAndNumberedPagesPreserveSourceLocationsButExcludeMessagesAndPaths() {
        var report = new RuntimeImportReport();
        for (int i = 0; i < 205; i++) report.issue("F1/C1", "CONFIGURATION_NOT_SUPPLIED");
        var failure = new IllegalArgumentException("PRIVATE_PASSWORD https://private/", new java.io.IOException("PRIVATE_BODY"));
        failure.setStackTrace(new StackTraceElement[]{new StackTraceElement("org.ikasan.studio.core.Example", "generate", "/PRIVATE_PATH", 42)});
        report.stage(RuntimeImportReport.Stage.GENERATION); report.failure(failure);
        String text = report.render("1.0.0-beta.1", "262.123.4");
        assertTrue(text.contains("omitted: 5")); assertTrue(text.contains("Occurrences: 200")); assertTrue(text.contains("IMPORT-200")); assertFalse(text.contains("IMPORT-201"));
        assertTrue(text.contains("Example.generate:42")); assertTrue(text.contains("java.io.IOException"));
        assertTrue(text.contains("PAGE 2 — DETAILS")); assertTrue(text.contains("Stage: GENERATION; outcome: Failed"));
        assertFalse(text.contains("PRIVATE"));
    }

    static class PrivateBusinessException extends RuntimeException { }

    @Test void applicationClassesSharingTheStudioNamespaceAreExcluded() {
        var report = new RuntimeImportReport();
        var failure = new PrivateBusinessException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("org.ikasan.studio.demo.PrivateComponent", "convert", "PrivateComponent.java", 12),
                new StackTraceElement("org.ikasan.studio.core.importer.IkasanRuntimeImport", "convert", "Ignored.java", 20)});
        report.failure(failure);
        String text = report.render(null, null);
        assertFalse(text.contains("PrivateBusinessException")); assertFalse(text.contains("PrivateComponent"));
        assertTrue(text.contains("External exception (type omitted)"));
        assertTrue(text.contains("IkasanRuntimeImport.convert:20"));
    }

    @Test void unsafeCodesAndNonstandardPackContextsAreNotExported() {
        var report = new RuntimeImportReport(); report.pack("PrivatePack");
        report.publicContext("F1/C1", "org.ikasan.component.Private", "secret");
        report.issue("F1/C1", "COMPONENT_MAPPING_UNAVAILABLE");
        assertFalse(report.render(null, null).contains("Private"));
        assertThrows(IllegalArgumentException.class, () -> report.issue("PRIVATE", "INVALID"));
        assertThrows(IllegalArgumentException.class, () -> report.issue("DOCUMENT", "password=secret"));
        report.request(RuntimeImportReport.Endpoint.METADATA, 500);
        assertTrue(report.render(null, null).contains("METADATA; HTTP: 500"));
        report.cancelled(); assertTrue(report.render(null, null).contains("outcome: Cancelled"));
    }
}
