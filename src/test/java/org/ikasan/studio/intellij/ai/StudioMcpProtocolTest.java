package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class StudioMcpProtocolTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void negotiationDiscoveryAndNotification() throws Exception {
        var protocol = new StudioMcpProtocol((name, arguments) -> "live");
        var initialized = json.valueToTree(protocol.handle(json.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}")));
        assertThat(initialized.path("result").path("protocolVersion").asText()).isEqualTo("2025-03-26");
        var tools = json.valueToTree(protocol.handle(json.readTree("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")));
        assertThat(tools.path("result").path("tools")).hasSize(4);
        assertThat(protocol.handle(json.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))).isNull();
    }
    @Test void catalogueFilterIsAdvertisedAndForwarded() throws Exception {
        var protocol = new StudioMcpProtocol((name, arguments) -> {
            assertThat(name).isEqualTo("studio_catalogue");
            assertThat(arguments.path("componentKeys").get(0).asText()).isEqualTo("Converter");
            return "selected";
        });
        var tools = json.valueToTree(protocol.handle(json.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}")));
        var catalogue = tools.path("result").path("tools").get(1);
        assertThat(catalogue.path("inputSchema").path("properties").path("componentKeys").path("type").asText()).isEqualTo("array");
        var result = json.valueToTree(protocol.handle(json.readTree("""
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"studio_catalogue","arguments":{"componentKeys":["Converter"]}}}
                """)));
        assertThat(result.path("result").path("isError").asBoolean()).isFalse();
    }
    @Test void toolFailuresAreReturnedWithoutProtocolFailure() throws Exception {
        var protocol = new StudioMcpProtocol((name, arguments) -> { throw new IllegalStateException("Stale revision"); });
        var response = json.valueToTree(protocol.handle(json.readTree("""
                {"jsonrpc":"2.0","id":"request","method":"tools/call","params":{"name":"studio_propose","arguments":{}}}
                """)));
        assertThat(response.path("id").asText()).isEqualTo("request");
        assertThat(response.path("result").path("isError").asBoolean()).isTrue();
        assertThat(response.path("result").path("content").get(0).path("text").asText()).isEqualTo("Stale revision");
    }
    @Test void unknownToolsDoNotInvokeModelService() throws Exception {
        var protocol = new StudioMcpProtocol((name, arguments) -> { throw new AssertionError("must not run"); });
        var response = json.valueToTree(protocol.handle(json.readTree("""
                {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"apply_without_review"}}
                """)));
        assertThat(response.path("error").path("code").asInt()).isEqualTo(-32602);
    }
    @Test void redactsNestedCredentialFields() throws Exception {
        var data = json.readTree("{\"flows\":[{\"password\":\"secret-value\",\"remoteHost\":\"example\",\"accessToken\":\"abc\"}]}");
        StudioAiService.redact(data);
        assertThat(data.toString()).doesNotContain("secret-value", "abc").contains("example", "<redacted>");
    }
}
