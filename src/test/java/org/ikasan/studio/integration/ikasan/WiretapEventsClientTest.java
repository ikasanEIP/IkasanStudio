package org.ikasan.studio.integration.ikasan;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;

class WiretapEventsClientTest {
    private static final String ROW = """
            {"identifier":42,"moduleName":"Demo","flowName":"Orders","componentName":"Audit",
             "eventId":"order-1","relatedEventId":"batch-1","timestamp":1234,"harvested":true,
             "event":"<order>hello & world</order>"}
            """;
    private static String page(String rows) {
        return "{\"pagedResults\":[" + rows + "],\"resultSize\":1,\"lastPage\":true}";
    }
    @Test void preservesStoredTextAndEventIdentity() throws Exception {
        var page = WiretapEventsClient.parse(page(ROW));
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.last()).isTrue();
        var event = page.events().get(0);
        assertThat(event.identifier()).isEqualTo(42);
        assertThat(event.component()).isEqualTo("Audit");
        assertThat(event.eventId()).isEqualTo("order-1");
        assertThat(event.relatedEventId()).isEqualTo("batch-1");
        assertThat(event.harvested()).isTrue();
        assertThat(event.payload()).isEqualTo("<order>hello & world</order>");
    }
    @Test void encodesContextFiltersAndValidatesDates() {
        URI endpoint = URI.create("http://localhost:8080/demo/rest/wiretap/");
        assertThat(WiretapEventsClient.query(endpoint, 2, "Orders & Returns", "Audit + Log", "", "").toString())
                .contains("pageNumber=2", "orderBy=timestamp", "orderAscending=false",
                        "flow=Orders+%26+Returns", "componentName=Audit+%2B+Log");
        assertThatThrownBy(() -> WiretapEventsClient.query(endpoint, 0, "", "", "2026-02-30T00:00:00", ""))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }
    @Test void rejectsMalformedAndOversizedPagesAndBoundsPreviews() throws Exception {
        assertThatThrownBy(() -> WiretapEventsClient.parse("{}" )).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> WiretapEventsClient.parse(page(ROW.replace("\"identifier\":42", "\"identifier\":{}"))))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> WiretapEventsClient.parse(page(String.join(",", java.util.Collections.nCopies(21, ROW)))))
                .isInstanceOf(IOException.class);
        assertThat(WiretapEventsClient.parse(page(ROW.replace("<order>hello & world</order>", "x".repeat(40000))))
                .events().get(0).payload()).hasSize(32770).endsWith("\n…");
    }
}
