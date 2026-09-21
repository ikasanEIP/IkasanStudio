package org.ikasan.studio.integration.ikasan;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.assertj.core.api.Assertions.*;

class FileDuplicateHistoryClientTest {
    @Test void preservesSearchPatternsWithoutQueryInjection() {
        URI uri = FileDuplicateHistoryClient.query(URI.create("http://localhost:12450/demo/rest/filefilter/search"),
                1, "client & one", "%order_1.csv");
        assertThat(uri.getRawQuery()).isEqualTo("pageNumber=1&pageSize=20&clientId=client+%26+one&criteria=%25order_1.csv");
    }
    @Test void readsNumericAndFormattedDatesAndRetainsIdentity() throws Exception {
        var page = FileDuplicateHistoryClient.parse("""
                {"resultSize":1,"lastPage":true,"pagedResults":[{"id":7,"clientId":"FTP Consumer",
                "criteria":"/orders/a.csv","size":12,"lastModified":0,
                "lastAccessed":"2026-09-21T12:00:00Z","createdDateTime":1000}]}
                """);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.last()).isTrue();
        assertThat(page.entries().get(0)).isEqualTo(new FileDuplicateHistoryClient.Entry("7","FTP Consumer",
                "/orders/a.csv",12,"1970-01-01T00:00:00Z","2026-09-21T12:00:00Z","1970-01-01T00:00:01Z"));
    }
    @Test void rejectsBrokenPagesInsteadOfShowingEmptyHistory() {
        for (String json : new String[]{"null","{}","{\"resultSize\":0,\"lastPage\":true,\"pagedResults\":[null]}"})
            assertThatThrownBy(() -> FileDuplicateHistoryClient.parse(json)).isInstanceOf(java.io.IOException.class);
    }
}
