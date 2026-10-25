package org.ikasan.studio.integration.ikasan;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeImportClientTest {
    private static final String MODULE = "{\"name\":\"Demo module\",\"flows\":[{\"name\":\"Flow 1\"},{\"name\":\"Flow/2\"}]}";
    private static final String RECORD = "{\"configurationId\":\"shared\",\"implementingClass\":\"Example\",\"parameters\":[]}";

    @Test void fetchesScopedConfigurationEncodesNamesAndDeduplicatesSharedRecords() throws Exception {
        var paths = new CopyOnWriteArrayList<String>();
        withServer(exchange -> {
            paths.add(exchange.getRequestURI().getRawPath());
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8)), exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, paths.size() == 1 ? MODULE : "[" + RECORD + "]");
        }, base -> {
            var result = RuntimeImportClient.fetch(base, "Demo module", "user", "secret".toCharArray(), true, () -> {});
            assertEquals(MODULE, result.moduleJson());
            assertEquals("[" + RECORD + "]", result.configurationJson());
            assertEquals(List.of("/ctx/rest/metadata/module/Demo%20module",
                    "/ctx/rest/configuration/Demo%20module/Flow%201/components",
                    "/ctx/rest/configuration/Demo%20module/Flow%2F2/components"), paths);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void fetchedDocumentsUseTheExistingImporterAndPreserveManualStartup(String pack) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var topology = json.createObjectNode().put("name", "Demo module").put("ikasanVersion", pack.substring(1));
        var flow = topology.putArray("flows").addObject().put("name", "Orders");
        var consumerMeta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer");
        var producerMeta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Logging Producer");
        var consumer = json.createObjectNode().put("componentName", "Poll").put("componentType", consumerMeta.getComponentType())
                .put("implementingClass", consumerMeta.getImplementingClass()).put("configurationId", "poll-config");
        flow.set("consumer", consumer);
        flow.putArray("flowElements").add(consumer.deepCopy()).addObject().put("componentName", "Log")
                .put("componentType", producerMeta.getComponentType()).put("implementingClass", producerMeta.getImplementingClass());
        flow.putArray("transitions").addObject().put("from", "Poll").put("to", "Log").put("name", "default");
        String config = "[{\"configurationId\":\"poll-config\",\"implementingClass\":\"org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumerConfiguration\","
                + "\"parameters\":[{\"name\":\"cronExpression\",\"value\":\"0/10 * * * * ?\"}]}]";
        withServer(exchange -> respond(exchange, 200, exchange.getRequestURI().getPath().contains("/metadata/") ? topology.toString() : config), base -> {
            var documents = RuntimeImportClient.fetch(base, "Demo module", "", new char[0], true, () -> {});
            var converted = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(documents.moduleJson(), documents.configurationJson(), pack,
                    "org.example.imported", Map.of(new org.ikasan.studio.core.importer.IkasanRuntimeImport.ComponentRef("Orders", "Poll"), "Scheduled Consumer"));
            var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(converted.studioJson(), "REST test", false);
            assertEquals("MANUAL", module.getFlows().get(0).getPropertyValue("flowStartupType"));
            assertEquals("0/10 * * * * ?", module.getFlows().get(0).getConsumer().getPropertyValue("cronExpression"));
        });
    }

    @Test void explicitTopologyOnlyFetchMakesNoConfigurationRequestsOrAuthenticationAssumptions() throws Exception {
        var count = new AtomicInteger();
        withServer(exchange -> {
            count.incrementAndGet(); assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, MODULE);
        }, base -> {
            assertEquals("[]", RuntimeImportClient.fetch(base, "Demo module", "", new char[0], false, () -> {}).configurationJson());
            assertEquals(1, count.get());
        });
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 500})
    void errorsExplainStatusWithoutLeakingResponseBody(int status) throws Exception {
        withServer(exchange -> respond(exchange, status, "sensitive password=secret"), base -> {
            var failure = assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], true, () -> {}));
            assertTrue(failure.getMessage().contains("HTTP " + status));
            assertFalse(failure.getMessage().contains("secret"));
        });
    }

    @Test void doesNotFollowRedirectsOrSendCredentialsToRedirectTarget() throws Exception {
        var count = new AtomicInteger();
        withServer(exchange -> {
            count.incrementAndGet(); exchange.getResponseHeaders().add("Location", "/unexpected"); respond(exchange, 302, "login");
        }, base -> {
            var failure = assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "u", "p".toCharArray(), true, () -> {}));
            assertTrue(failure.getMessage().contains("redirect")); assertEquals(1, count.get());
        });
    }

    @ParameterizedTest @ValueSource(strings = {"<html>secret</html>", "null", "{\"name\":\"other\",\"flows\":[]}", "{\"name\":\"Demo module\",\"flows\":[],\"flows\":[]}"})
    void rejectsInvalidOrWrongModuleMetadata(String document) throws Exception {
        withServer(exchange -> respond(exchange, 200, document), base ->
                assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], false, () -> {})));
    }

    @Test void rejectsConflictingConfigurationFromDifferentFlows() throws Exception {
        var count = new AtomicInteger();
        withServer(exchange -> {
            int index = count.incrementAndGet();
            respond(exchange, 200, index == 1 ? MODULE : "[" + (index == 2 ? RECORD : RECORD.replace("Example", "Different")) + "]");
        }, base -> assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], true, () -> {})));
    }

    @Test void configurationFailureDoesNotReturnPartialDocuments() throws Exception {
        var count = new AtomicInteger();
        withServer(exchange -> {
            int index = count.incrementAndGet(); respond(exchange, index == 3 ? 403 : 200, index == 1 ? MODULE : "[]");
        }, base -> assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], true, () -> {})));
    }

    @Test void rejectsOversizedResponseDuringReception() throws Exception {
        withServer(exchange -> {
            exchange.sendResponseHeaders(200, RuntimeImportClient.MAX_BYTES + 1L);
            try (var output = exchange.getResponseBody()) {
                byte[] block = new byte[64 * 1024];
                for (int i = 0; i < 128; i++) output.write(block);
                output.write(1);
            } catch (IOException cancelled) { /* Client stops receiving at its bound. */ }
        }, base -> assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], false, () -> {})));
    }

    @Test void cancellationInterruptsARequestWaitingForTheServer() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var cancel = new AtomicBoolean(); var executor = Executors.newSingleThreadExecutor();
        try {
            withServer(exchange -> {
                entered.countDown();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                try { respond(exchange, 200, MODULE); } catch (IOException cancelled) { }
            }, base -> {
                var task = executor.submit(() -> RuntimeImportClient.fetch(base, "Demo module", "", new char[0], false,
                        () -> { if (cancel.get()) throw new CancellationException(); }));
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS)); cancel.set(true);
                    var failure = assertThrows(ExecutionException.class, () -> task.get(3, TimeUnit.SECONDS));
                    assertInstanceOf(CancellationException.class, failure.getCause());
                } finally { release.countDown(); task.cancel(true); }
            });
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @ParameterizedTest @ValueSource(strings = {"file:///tmp/config", "http://user:secret@localhost/app", "http://localhost/app?token=secret", "http://localhost/app#fragment", "not a URL"})
    void rejectsAmbiguousOrCredentialBearingUrls(String url) {
        var failure = assertThrows(IllegalArgumentException.class, () -> RuntimeImportClient.baseUri(url));
        assertFalse(failure.getMessage().contains("secret"));
    }

    private interface Check { void run(URI base) throws Exception; }
    private static void withServer(com.sun.net.httpserver.HttpHandler handler, Check check) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler); server.start();
        try { check.run(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/ctx/")); }
        finally { server.stop(0); }
    }
    private static void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
