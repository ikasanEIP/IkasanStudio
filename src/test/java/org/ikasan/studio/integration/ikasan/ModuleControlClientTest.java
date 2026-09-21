package org.ikasan.studio.integration.ikasan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleControlClientTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "{", "null", "{}", "{\"flows\":[null]}", "{\"flows\":[{\"name\":\"f\"}]}",
            "{\"flows\":[{\"name\":\"f\",\"state\":\"running\"},{\"name\":\"f\",\"state\":\"stopped\"}]}",
            "{\"flows\":[]} {}", "{\"flows\":[],\"flows\":[]}"
    })
    void rejectsMalformedRestDataAsRecoverableIoFailure(String json) {
        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> ModuleControlClient.parseFlowStates(json));
    }

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
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowControlOperation.START_PAUSE);

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"startPause\"}");
    }

    @Test
    void serialisesTheResumeOperation() throws Exception {
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowControlOperation.RESUME);

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"resume\"}");
    }

    @Test
    void serialisesTheStartOperation() throws Exception {
        String body = ModuleControlClient.buildChangeFlowStateRequestBody("untitled104", "f4", FlowControlOperation.START);

        assertThat(body).isEqualTo("{\"moduleName\":\"untitled104\",\"flowName\":\"f4\",\"action\":\"start\"}");
    }

    private static org.ikasan.studio.core.model.ikasan.instance.Module moduleNamed(String name) throws Exception {
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(
                org.ikasan.studio.core.TestFixtures.BASE_META_PACK, new java.util.ArrayList<>());
        module.setName(name);
        module.setPort("8391");
        return module;
    }

    /**
     * The flow name in the error lookup is a query value. The multi-argument URI constructor leaves & + = ; unencoded
     * because they are legal in a query, so "Orders & Invoices" split into flow=Orders plus a stray parameter and
     * "C++ Import" arrived with its plus signs read as spaces. The lookup is best-effort, so the failure was silent:
     * the flow showed as in error but without its error detail.
     */
    @Test
    void flowNamesAreEncodedAsQueryValuesInTheErrorLookup() throws Exception {
        var module = moduleNamed("My Module");
        String prefix = "http://localhost:8391/my-module/rest/error/?pageNumber=0&pageSize=1&orderBy=timestamp&orderAscending=false&flow=";
        String[][] cases = {
                {"Orders & Invoices", "Orders+%26+Invoices"},
                {"C++ Import", "C%2B%2B+Import"},
                {"a=b;c", "a%3Db%3Bc"},
                {"100% Flow", "100%25+Flow"},
                {"Caf\u00e9 Orders", "Caf%C3%A9+Orders"},
                {"plain", "plain"}};
        for (String[] c : cases) {
            var uri = ModuleControlClient.endpointUri(module, "/rest/error/", ModuleControlClient.latestErrorQuery(c[0]));
            assertThat(uri.toASCIIString()).as(c[0]).isEqualTo(prefix + c[1]);
            // Whatever the name, the value must decode back to exactly the flow name and stay a single parameter.
            String value = java.util.Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(p -> p.startsWith("flow=")).map(p -> p.substring(5)).findFirst().orElseThrow();
            assertThat(java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(c[0]);
            assertThat(uri.getRawQuery().split("&")).hasSize(5);
        }
    }

    @Test
    void thePathPartOfModuleUrlsIsStillEncodedByTheUriConstructor() throws Exception {
        var module = moduleNamed("A to B convert");
        assertThat(ModuleControlClient.endpointUri(module, "/rest/moduleControl/" + module.getIdentity(), null).toASCIIString())
                .isEqualTo("http://localhost:8391/a-to-b-convert/rest/moduleControl/A%20to%20B%20convert");
    }
}
