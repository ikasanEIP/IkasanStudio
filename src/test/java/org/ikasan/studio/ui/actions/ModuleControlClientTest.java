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
    void returnsNullWhenNoErrorOccurrencesAreLoggedYet() throws Exception {
        String json = "{\"pagedResults\":[]}";

        String summary = ModuleControlClient.parseLatestErrorSummary(json);

        assertThat(summary).isNull();
    }
}
