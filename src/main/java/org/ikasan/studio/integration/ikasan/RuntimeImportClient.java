package org.ikasan.studio.integration.ikasan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.persistence.json.StudioJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Read-only acquisition of the documents accepted by the existing runtime importer. */
public final class RuntimeImportClient {
    static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final ObjectMapper JSON = StudioJson.newObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private RuntimeImportClient() { }

    /** Payloads may contain application credentials. Never log or persist this acquisition result automatically. */
    public static final class Documents {
        private final String moduleJson;
        private final String configurationJson;
        public Documents(String moduleJson, String configurationJson) {
            this.moduleJson = moduleJson; this.configurationJson = configurationJson;
        }
        public String moduleJson() { return moduleJson; }
        public String configurationJson() { return configurationJson; }
    }

    public static URI baseUri(String value) {
        URI uri;
        try { uri = URI.create(value.trim()); }
        catch (IllegalArgumentException failure) { throw new IllegalArgumentException("Enter a valid module application URL."); }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getPort() > 65535 || uri.getPort() == 0 || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Use an HTTP(S) application URL including its context path, without credentials, query or fragment.");
        }
        return URI.create(uri.toASCIIString().replaceAll("/+$", ""));
    }

    public static Documents fetch(URI base, String module, String username, char[] password,
                                  boolean includeConfiguration, Runnable cancelled) throws IOException, InterruptedException {
        base = baseUri(base.toString());
        if (module == null || module.isBlank()) throw new IllegalArgumentException("Enter the running module name.");
        if (username == null) username = "";
        if (username.contains(":")) throw new IllegalArgumentException("A Basic authentication username cannot contain a colon.");
        if (username.isBlank() && password.length != 0) throw new IllegalArgumentException("Enter a username or clear the password for anonymous access.");
        String authorization = username.isBlank() ? null : "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + new String(password)).getBytes(StandardCharsets.UTF_8));
        String metadata = get(base, "/rest/metadata/module/" + segment(module), authorization, cancelled);
        JsonNode topology = parse(metadata, "Module metadata");
        if (!topology.isObject() || !topology.path("flows").isArray()
                || !topology.path("name").isTextual() || !module.equals(topology.path("name").asText())) {
            throw new IOException("Module metadata must describe the requested module and contain a flows array.");
        }
        if (topology.path("flows").size() > 1000) throw new IOException("Import supports at most 1,000 flows.");
        var names = new LinkedHashSet<String>();
        for (var flow : topology.path("flows")) {
            if (!flow.path("name").isTextual() || flow.path("name").asText().isBlank()
                    || !names.add(flow.path("name").asText())) throw new IOException("Module metadata has invalid or duplicate flow names.");
        }
        var records = new LinkedHashMap<String, JsonNode>();
        int total = 0;
        if (includeConfiguration) for (String flow : names) {
            // The unqualified /configuration/components endpoint selects the first module. Always scope it.
            String text = get(base, "/rest/configuration/" + segment(module) + "/" + segment(flow) + "/components", authorization, cancelled);
            total += text.getBytes(StandardCharsets.UTF_8).length;
            if (total > MAX_BYTES) throw new IOException("Combined component configuration exceeds the 8 MiB import limit.");
            JsonNode array = parse(text, "Component configuration");
            if (!array.isArray()) throw new IOException("Component configuration must be a JSON array.");
            for (var record : array) {
                if (!record.isObject() || !record.path("configurationId").isTextual()
                        || record.path("configurationId").asText().isBlank()) throw new IOException("A component configuration record has no configurationId.");
                String id = record.path("configurationId").asText();
                var previous = records.putIfAbsent(id, record);
                if (previous != null && !previous.equals(record)) throw new IOException("Conflicting records share a configurationId; fetch again when configuration is stable.");
            }
        }
        cancelled.run();
        var configuration = JSON.createArrayNode();
        records.values().forEach(configuration::add);
        String combined = JSON.writeValueAsString(configuration);
        if (combined.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IOException("Component configuration exceeds the 8 MiB import limit.");
        return new Documents(metadata, combined);
    }

    private static String segment(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20"); }

    private static JsonNode parse(String text, String document) throws IOException {
        try {
            var node = JSON.readTree(text);
            if (node == null || node.isNull()) throw new IOException();
            return node;
        } catch (IOException failure) {
            // Server responses may contain secrets. Do not expose parser excerpts or login HTML.
            throw new IOException(document + " is not valid JSON. Check the application URL and authentication.");
        }
    }

    private static String get(URI base, String path, String authorization, Runnable cancelled) throws IOException, InterruptedException {
        cancelled.run();
        var request = HttpRequest.newBuilder(URI.create(base.toASCIIString() + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json").GET();
        if (authorization != null) request.header("Authorization", authorization);
        var future = HTTP.sendAsync(request.build(), ignored -> new LimitedBody());
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        try {
            HttpResponse<byte[]> response;
            while (true) {
                cancelled.run();
                if (System.nanoTime() - deadline >= 0) throw new IOException("The module REST request timed out.");
                try { response = future.get(100, TimeUnit.MILLISECONDS); break; }
                catch (TimeoutException waiting) { /* Check platform cancellation while the HTTP operation is pending. */ }
                catch (ExecutionException failure) {
                    if (failure.getCause() instanceof HttpTimeoutException) throw new IOException("The module REST request timed out.");
                    throw new IOException("Could not read the module REST response. Check connectivity, TLS trust and response size.");
                }
            }
            int status = response.statusCode();
            if (status == 401 || status == 403) throw new IOException("REST access was denied (HTTP " + status + "). Check credentials and ALL or WebServiceAdmin authority.");
            if (status >= 300 && status < 400) throw new IOException("The module redirected the REST request. Enter its final application URL; redirects are not followed.");
            if (status != 200) throw new IOException("Module REST request returned HTTP " + status + ". Check the context path, module name and REST endpoint availability.");
            return new String(response.body(), StandardCharsets.UTF_8);
        } finally { future.cancel(true); }
    }

    /** Enforce the input limit while receiving, rather than after buffering an unbounded response. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private int size;
        @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription value) { subscription = value; delegate.onSubscribe(value); }
        @Override public void onNext(List<ByteBuffer> buffers) {
            long incoming = buffers.stream().mapToLong(ByteBuffer::remaining).sum();
            if (incoming > MAX_BYTES - size) {
                subscription.cancel(); delegate.onError(new IOException("REST response exceeds 8 MiB")); return;
            }
            size += (int) incoming; delegate.onNext(buffers);
        }
        @Override public void onError(Throwable failure) { delegate.onError(failure); }
        @Override public void onComplete() { delegate.onComplete(); }
    }
}
