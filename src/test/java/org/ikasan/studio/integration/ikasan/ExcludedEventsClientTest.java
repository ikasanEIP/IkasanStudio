package org.ikasan.studio.integration.ikasan;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class ExcludedEventsClientTest {
    @Test void encodesFlowAsOneQueryValueAndValidatesDates() {
        URI endpoint = URI.create("http://localhost:12450/demo/rest/exclusion/");
        String query = ExcludedEventsClient.query(endpoint, 2, "Orders & Audit+1", "2026-09-21T12:00:00", "").getRawQuery();
        assertThat(query).contains("pageNumber=2", "flow=Orders+%26+Audit%2B1", "fromDateTime=2026-09-21T12%3A00%3A00");
        assertThatThrownBy(() -> ExcludedEventsClient.query(endpoint, 0, "", "2026-02-30T12:00:00", ""))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
        assertThatThrownBy(() -> ExcludedEventsClient.query(endpoint, 0, "", "2026-09-22T12:00:00", "2026-09-21T12:00:00"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void fetchUsesReadOnlyAuthenticatedRequestAndReportsAccessFailure() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var method = new java.util.concurrent.atomic.AtomicReference<String>();
        var auth = new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/rest/exclusion/", exchange -> {
            method.set(exchange.getRequestMethod());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(403, -1); exchange.close();
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/rest/exclusion/");
            assertThatThrownBy(() -> ExcludedEventsClient.fetch(uri))
                    .isInstanceOf(ExcludedEventsClient.HttpFailure.class).hasMessage("HTTP 403");
            assertThat(method.get()).isEqualTo("GET");
            assertThat(auth.get()).isEqualTo("Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } finally { server.stop(0); }
    }

    @Test void refusesOversizedHttpResponses() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rest/exclusion/", exchange -> {
            byte[] huge = new byte[4 * 1024 * 1024 + 1];
            exchange.sendResponseHeaders(200, huge.length);
            try (var out = exchange.getResponseBody()) { out.write(huge); }
            catch (java.io.IOException ignored) { /* Expected when the client cancels. */ }
            finally { exchange.close(); }
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/rest/exclusion/");
            assertThatThrownBy(() -> ExcludedEventsClient.fetch(uri)).isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("4 MiB");
        } finally { server.stop(0); }
    }

    private String response(String payload) {
        return "{\"resultSize\":21,\"lastPage\":false,\"pagedResults\":[{\"moduleName\":\"demo\",\"flowName\":\"Orders\","
                + "\"identifier\":\"order-2\",\"timestamp\":1700000000000,\"harvested\":false,\"errorUri\":\"err-2\",\"event\":\""
                + payload + "\"}]}";
    }

    @Test void readsPagingAndUtf8PayloadWithoutDroppingErrorIdentity() throws Exception {
        var page = ExcludedEventsClient.parse(response(Base64.getEncoder().encodeToString("<order>2</order>".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertThat(page.total()).isEqualTo(21);
        assertThat(page.last()).isFalse();
        var event = page.events().get(0);
        assertThat(event.errorUri()).isEqualTo("err-2");
        assertThat(event.payload()).isEqualTo("<order>2</order>");
        assertThat(event.binary()).isFalse();
        assertThat(event.harvested()).isFalse();
    }

    @Test void keepsSerializedObjectsAsBase64AndRejectsMalformedResponses() throws Exception {
        String binary = Base64.getEncoder().encodeToString(new byte[]{(byte)0xac,(byte)0xed,0,5});
        assertThat(ExcludedEventsClient.parse(response(binary)).events().get(0).binary()).isTrue();
        assertThat(ExcludedEventsClient.parse(response(binary)).events().get(0).payload()).isEqualTo(binary);
        for (String json : new String[]{"null", "{}", "{\"pagedResults\":[]}", response("not base64!"), response("")+"{}"})
            assertThatThrownBy(() -> ExcludedEventsClient.parse(json)).isInstanceOf(java.io.IOException.class);
    }
}
