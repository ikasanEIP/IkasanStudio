package org.ikasan.studio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone MCP stdio adapter. Only protocol responses are written to stdout. */
public final class StudioMcpAdapter {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int TIMEOUT_MILLIS = 120_000;

    private StudioMcpAdapter() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"--connection".equals(args[0])) {
            System.err.println("Usage: java -jar studio-mcp-adapter.jar --connection <connection.json>");
            System.exit(2);
            return;
        }
        Path connectionFile = Path.of(args[1]);
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        String line;
        while ((line = input.readLine()) != null) {
            JsonNode request = null;
            try {
                request = JSON.readTree(line);
                if (request == null || !request.isObject()) {
                    throw new IllegalArgumentException("Expected a JSON-RPC object");
                }
                // Re-read for each request so removal of the session file invalidates the adapter.
                JsonNode connection = JSON.readTree(Files.readString(connectionFile, StandardCharsets.UTF_8));
                URI endpoint = URI.create(connection.path("url").asText());
                if (!"http".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost())
                        || !"/rpc".equals(endpoint.getRawPath()) || endpoint.getRawUserInfo() != null
                        || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null) {
                    throw new IllegalArgumentException("Studio connection must be a loopback /rpc endpoint");
                }
                // Explicitly bypass system proxies; never redirect a request carrying the token.
                var http = (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);
                try {
                    http.setInstanceFollowRedirects(false);
                    http.setConnectTimeout(TIMEOUT_MILLIS);
                    http.setReadTimeout(TIMEOUT_MILLIS);
                    http.setRequestMethod("POST");
                    http.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    http.setRequestProperty("Authorization", "Bearer " + connection.path("token").asText());
                    http.setDoOutput(true);
                    byte[] body = line.getBytes(StandardCharsets.UTF_8);
                    http.setFixedLengthStreamingMode(body.length);
                    try (var stream = http.getOutputStream()) { stream.write(body); }
                    int status = http.getResponseCode();
                    if (status < 200 || status >= 300) {
                        throw new IllegalStateException("Studio bridge returned HTTP " + status);
                    }
                    try (var stream = http.getInputStream()) {
                        byte[] response = stream.readAllBytes();
                        if (response.length > 0) {
                            output.println(JSON.writeValueAsString(JSON.readTree(response)));
                        }
                    }
                } finally {
                    http.disconnect();
                }
            } catch (Exception failure) {
                // Do not echo connection contents, tokens or response bodies into diagnostics.
                String message = "Studio bridge unavailable (" + failure.getClass().getSimpleName() + ")";
                if (request != null && request.isObject() && request.has("id")) {
                    var response = JSON.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    response.set("id", request.get("id"));
                    response.putObject("error").put("code", -32000).put("message", message);
                    output.println(JSON.writeValueAsString(response));
                } else {
                    System.err.println(message);
                }
            }
        }
    }
}
