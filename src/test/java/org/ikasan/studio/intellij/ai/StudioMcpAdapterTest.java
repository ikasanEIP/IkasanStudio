package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the exact executable JAR bundled in the plugin, without the IDE classpath. */
class StudioMcpAdapterTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path directory;

    @Test void forwardsUtf8AndAuthenticationAndRecoversAfterMalformedInput() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var authorization = new AtomicReference<String>();
        var received = new AtomicReference<String>();
        server.createContext("/rpc", exchange -> {
            try (exchange) {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] reply = received.get().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, reply.length);
                exchange.getResponseBody().write(reply);
            }
        });
        server.start();
        try {
            Path connection = connection("http://127.0.0.1:" + server.getAddress().getPort() + "/rpc");
            String request = "{\"jsonrpc\":\"2.0\",\"id\":\"日本語\",\"method\":\"tools/list\"}";
            Result result = run(connection, "invalid json\n" + request + "\n");
            assertThat(result.lines()).hasSize(1);
            assertThat(JSON.readTree(result.lines().get(0))).isEqualTo(JSON.readTree(request));
            assertThat(received.get()).isEqualTo(request);
            assertThat(authorization.get()).isEqualTo("Bearer test-secret");
            assertThat(result.errors()).contains("Studio bridge unavailable").doesNotContain("test-secret");
        } finally { server.stop(0); }
    }

    @Test void missingConnectionReturnsErrorsOnlyForRequests() throws Exception {
        Result result = run(directory.resolve("missing.json"), """
                {"jsonrpc":"2.0","id":7,"method":"initialize"}
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                {"jsonrpc":"2.0","id":"next","method":"tools/list"}
                """);
        assertThat(result.lines()).hasSize(2);
        assertThat(JSON.readTree(result.lines().get(0)).path("id").asInt()).isEqualTo(7);
        assertThat(JSON.readTree(result.lines().get(1)).path("id").asText()).isEqualTo("next");
        assertThat(JSON.readTree(result.lines().get(0)).path("error").path("code").asInt()).isEqualTo(-32000);
        assertThat(result.errors()).contains("Studio bridge unavailable");
    }

    @Test void rejectsWrongEndpointAndNeverFollowsRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var redirected = new AtomicInteger();
        server.createContext("/rpc", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Location", "/elsewhere");
                exchange.sendResponseHeaders(302, -1);
            }
        });
        server.createContext("/elsewhere", exchange -> {
            redirected.incrementAndGet();
            try (exchange) { exchange.sendResponseHeaders(204, -1); }
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}\n";
            for (String endpoint : List.of(base + "/elsewhere", base + "/rpc", base + "/rpc?secret=test-secret")) {
                Result result = run(connection(endpoint), request);
                assertThat(result.lines()).hasSize(1);
                assertThat(JSON.readTree(result.lines().get(0)).has("error")).isTrue();
                assertThat(result.lines().get(0)).doesNotContain("test-secret");
            }
            assertThat(redirected.get()).isZero();
        } finally { server.stop(0); }
    }

    private Path connection(String url) throws Exception {
        Path file = directory.resolve("connection with spaces.json");
        Files.writeString(file, JSON.writeValueAsString(Map.of("url", url, "token", "test-secret")));
        return file;
    }

    private Result run(Path connection, String input) throws Exception {
        Path jar = directory.resolve("adapter with spaces.jar");
        try (var resource = getClass().getResourceAsStream("/studio/ai/studio-mcp-adapter.jar")) {
            assertThat(resource).isNotNull();
            Files.copy(resource, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dhttp.proxyHost=127.0.0.1", "-Dhttp.proxyPort=1", "-Dhttp.nonProxyHosts=",
                "-jar", jar.toString(), "--connection", connection.toString()).start();
        try {
            try (var stdin = process.outputWriter(StandardCharsets.UTF_8)) { stdin.write(input); }
            assertThat(process.waitFor(15, TimeUnit.SECONDS)).as("adapter exits on stdin EOF").isTrue();
            String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as(errors).isZero();
            return new Result(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).lines().toList(), errors);
        } finally { process.destroyForcibly(); }
    }

    private record Result(List<String> lines, String errors) { }
}
