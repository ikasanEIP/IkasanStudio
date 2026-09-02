package org.ikasan.studio.ui.actions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleControlClientTest {

    @Test
    void parsesEachFlowsStateFromAModuleControlResponse() throws Exception {
        String json = "{\"name\":\"untitled104\",\"flows\":["
                + "{\"name\":\"f1\",\"state\":\"running\"},"
                + "{\"name\":\"f2\",\"state\":\"stoppedInError\"}"
                + "]}";

        Map<String, String> states = ModuleControlClient.parseFlowStates(json);

        assertThat(states).containsExactly(Map.entry("f1", "running"), Map.entry("f2", "stoppedInError"));
    }

    @Test
    void parsesAnEmptyFlowsListToAnEmptyMap() throws Exception {
        String json = "{\"name\":\"untitled104\",\"flows\":[]}";

        Map<String, String> states = ModuleControlClient.parseFlowStates(json);

        assertThat(states).isEmpty();
    }

    @Test
    void toleratesUnknownFieldsOnTheModuleControlResponse() throws Exception {
        String json = "{\"name\":\"untitled104\",\"someFutureField\":42,\"flows\":["
                + "{\"name\":\"f1\",\"state\":\"running\",\"someFutureFlowField\":\"x\"}"
                + "]}";

        Map<String, String> states = ModuleControlClient.parseFlowStates(json);

        assertThat(states).containsExactly(Map.entry("f1", "running"));
    }

    @Test
    void buildsAHumanReadableSummaryFromTheMostRecentErrorOccurrence() throws Exception {
        String json = "{\"pagedResults\":[{"
                + "\"flowName\":\"f4\","
                + "\"flowElementName\":\"bob\","
                + "\"errorMessage\":\"Expected MIME type, got null\","
                + "\"exceptionClass\":\"javax.mail.internet.ParseException\","
                + "\"timestamp\":1700000000000}]}";

        String summary = ModuleControlClient.parseLatestErrorSummary(json);

        assertThat(summary).isEqualTo("ParseException: Expected MIME type, got null at bob");
    }

    @Test
    void buildsACopyableReportIncludingMetadataAndFullStackTrace() throws Exception {
        String json = "{\"pagedResults\":[{"
                + "\"moduleName\":\"orders-module\","
                + "\"flowName\":\"processOrders\","
                + "\"flowElementName\":\"validateOrder\","
                + "\"errorMessage\":\"Customer identifier is missing\","
                + "\"exceptionClass\":\"org.example.ValidationException\","
                + "\"errorDetail\":\"org.example.ValidationException: Customer identifier is missing\\n\\tat org.example.ValidateOrder.convert(ValidateOrder.java:42)\","
                + "\"uri\":\"error-123\","
                + "\"eventLifeIdentifier\":\"event-456\","
                + "\"timestamp\":1700000000000}]}";

        ModuleControlClient.ErrorDetails details = ModuleControlClient.parseLatestErrorDetails(json);

        assertThat(details).isNotNull();
        assertThat(details.summary()).isEqualTo(
                "ValidationException: Customer identifier is missing at validateOrder");
        assertThat(details.report()).contains(
                "Module: orders-module",
                "Flow: processOrders",
                "Component: validateOrder",
                "Exception: org.example.ValidationException",
                "Error URI: error-123",
                "Event identifier: event-456",
                "Stack trace:\norg.example.ValidationException: Customer identifier is missing",
                "at org.example.ValidateOrder.convert(ValidateOrder.java:42)");
    }

    @Test
    void returnsNullWhenNoErrorOccurrencesAreLoggedYet() throws Exception {
        String json = "{\"pagedResults\":[]}";

        String summary = ModuleControlClient.parseLatestErrorSummary(json);

        assertThat(summary).isNull();
    }

    @Test
    void buildsAChangeFlowStateRequestBodyMatchingIkasansChangeFlowStateDto() throws Exception {
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowTransportAction.START_PAUSE, "stopped");

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"startPause\"}");
    }

    @Test
    void sendsResumeRatherThanStartWhenTheStartButtonIsClickedOnAPausedFlow() throws Exception {
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowTransportAction.START, "paused");

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"resume\"}");
    }

    @Test
    void sendsStartAsIsWhenTheFlowIsNotCurrentlyPaused() throws Exception {
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowTransportAction.START, "stopped");

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"start\"}");
    }
}
