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

    @Test void reportsTypedHttpStageAndStatusWithoutPassingUrlsOrResponseBodies() throws Exception {
        var events = new ArrayList<String>();
        withServer(exchange -> respond(exchange, 500, "PRIVATE_RESPONSE"), base -> {
            assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base, "PRIVATE_MODULE", "", new char[0], false,
                    () -> {}, ignored -> {}, (stage, status) -> events.add(stage + ":" + status)));
            assertEquals(List.of("METADATA:0", "METADATA:500"), events);
        });
    }

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
    void fetchedDocumentsUseTheExistingImporterAndPreserveDeclaredStartup(String pack) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var topology = json.createObjectNode().put("name", "Demo module").put("ikasanVersion", pack.substring(1));
        var flow = topology.putArray("flows").addObject().put("name", "Orders").put("flowStartupType", "AUTOMATIC");
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
            assertEquals("AUTOMATIC", module.getFlows().get(0).getPropertyValue("flowStartupType"));
            assertEquals("0/10 * * * * ?", module.getFlows().get(0).getConsumer().getPropertyValue("cronExpression"));
        });
    }

    /** REST-only fixture: deliberately different from every Studio broker default. */
    private String[] jmsDocuments(String pack) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var module = json.createObjectNode().put("name", "Demo module").put("ikasanVersion", pack.substring(1));
        var flow = module.putArray("flows").addObject().put("name", "REST JMS").put("flowStartupType", "AUTOMATIC");
        var elements = flow.putArray("flowElements");
        var configs = json.createArrayNode();
        for (String variant : List.of("Spring JMS Consumer", "Spring JMS Producer")) {
            boolean consumer = variant.endsWith("Consumer");
            String name = consumer ? "Receive" : "Send";
            var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, variant);
            var element = elements.addObject().put("componentName", name).put("componentType", meta.getComponentType())
                    .put("implementingClass", meta.getImplementingClass()).put("configurationId", name + "-config");
            if (consumer) flow.set("consumer", element.deepCopy());
            String type = meta.getRuntimeConfigurationClass();
            if (type == null) type = meta.getAllowableProperties().get("configuration").getUsageDataType();
            var config = configs.addObject().put("configurationId", name + "-config").put("implementingClass", type);
            var parameters = config.putArray("parameters");
            parameters.addObject().put("name", "destinationJndiName").put("value", "queue/rest-import");
            var factory = parameters.addObject().put("name", "connectionFactoryJndiProperties").putObject("value");
            factory.put("java.naming.factory.initial", "example.CustomInitialContextFactory")
                    .put("java.naming.provider.url", "tcp://remote-broker.example:61618")
                    .put("java.naming.security.principal", "rest-user")
                    .put("java.naming.security.credentials", "private-jndi-password")
                    .put("custom.provider.option", "keep-me");
            var destination = parameters.addObject().put("name", "destinationJndiProperties").putObject("value");
            if (consumer) destination.put("java.naming.provider.url", "tcp://destination.example:61619");
        }
        flow.putArray("transitions").addObject().put("from", "Receive").put("to", "Send").put("name", "default");
        return new String[]{module.toString(), configs.toString()};
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void restOnlyJndiMapsSurviveImportReloadAndGenerationWithoutDefaultOverrides(String pack) throws Exception {
        var fixture = jmsDocuments(pack);
        withServer(exchange -> respond(exchange, 200,
                exchange.getRequestURI().getPath().contains("/metadata/") ? fixture[0] : fixture[1]), base -> {
            var documents = RuntimeImportClient.fetch(base, "Demo module", "", new char[0], true, () -> {});
            var converted = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(
                    documents.moduleJson(), documents.configurationJson(), pack, "org.example.imported");
            assertFalse(converted.review().toString().contains("configuration fields not applied"));
            assertFalse(converted.review().toString().contains("private-jndi-password"));
            var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(converted.studioJson(), "REST JMS", false);
            module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(
                    org.ikasan.studio.core.generator.ModelTemplate.create(module), "REST JMS reload", false);
            var flow = module.getFlows().get(0);
            var endpoints = List.of(flow.getConsumer(), flow.getFlowRoute().getFlowElements().get(0));
            var properties = new Properties();
            properties.load(new java.io.StringReader(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)));
            var json = new com.fasterxml.jackson.databind.ObjectMapper();
            for (var endpoint : endpoints) {
                var factory = (Map<?, ?>) endpoint.getPropertyValue("connectionFactoryJndiProperties");
                assertEquals("tcp://remote-broker.example:61618", factory.get("java.naming.provider.url"));
                assertEquals("private-jndi-password", factory.get("java.naming.security.credentials"));
                assertEquals("keep-me", factory.get("custom.provider.option"));
                for (String field : List.of("connectionFactoryJndiProperties", "destinationJndiProperties")) {
                    String key = org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(
                            module, flow, endpoint, endpoint.getProperty(field).getMeta().getPropertyConfigFileLabel());
                    assertEquals(endpoint.getPropertyValue(field), json.readValue(properties.getProperty(key), Map.class));
                }
            }
            assertEquals(Map.of(), endpoints.get(1).getPropertyValue("destinationJndiProperties"));
            String generated = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create("org.example.imported", module, flow);
            assertTrue(generated.contains(".setConnectionFactoryJndiProperties(studioStringMap("));
            assertTrue(generated.contains(".setDestinationJndiProperties(studioStringMap("));
            for (String prefix : List.of("ConnectionFactoryJndiProperty", "DestinationJndiProperty"))
                for (String suffix : List.of("ProviderUrl", "FactoryInitial", "SecurityPrincipal", "SecurityCredentials", "UrlPkgPrefixes"))
                    assertFalse(generated.contains(".set" + prefix + suffix + "("), "Individual field must not overwrite the map");
        });
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void invalidJndiMapsAndMismatchedClassesRemainUnappliedAndFlagged(String pack) throws Exception {
        var fixture = jmsDocuments(pack);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var configs = json.readTree(fixture[1]);
        var producer = (com.fasterxml.jackson.databind.node.ObjectNode) configs.get(1);
        ((com.fasterxml.jackson.databind.node.ObjectNode) producer.path("parameters").get(1).path("value"))
                .putObject("unsupported.nested.option").put("value", "private-value");
        var invalid = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(fixture[0], configs.toString(), pack, "org.example");
        assertTrue(invalid.review().toString().contains("Send: review configuration fields not applied: connectionFactoryJndiProperties"));
        assertFalse(invalid.review().toString().contains("private-value"));
        var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(invalid.studioJson(), "invalid map", false);
        assertNull(module.getFlows().get(0).getFlowRoute().getFlowElements().get(0).getPropertyValue("connectionFactoryJndiProperties"));
        producer.put("implementingClass", "example.DifferentConfiguration");
        var mismatch = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(fixture[0], configs.toString(), pack, "org.example");
        assertTrue(mismatch.review().toString().contains("destinationJndiProperties"));
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

    @ParameterizedTest @ValueSource(strings = {
            "/swagger-ui/index.html", "/swagger-ui/", "/swagger-ui", "/swagger-ui.html",
            "/swagger-ui/index.html?configUrl=/api-docs#/module"})
    void normalizesSwaggerLandingPagesWithoutChangingTheContext(String suffix) {
        assertEquals("http://localhost:8080/My%20Module",
                RuntimeImportClient.baseUri("http://localhost:8080/My%20Module" + suffix).toString());
        assertEquals("http://localhost:8080",
                RuntimeImportClient.baseUri("http://localhost:8080/swagger-ui/index.html").toString());
    }

    @Test void swaggerUrlFetchUsesApplicationRestPath() throws Exception {
        withServer(exchange -> {
            assertEquals("/ctx/rest/metadata/module/Demo%20module", exchange.getRequestURI().getRawPath());
            respond(exchange, 200, MODULE);
        }, base -> assertEquals(MODULE, RuntimeImportClient.fetch(
                URI.create(base + "swagger-ui/index.html"), "Demo module", "", new char[0], false, () -> {}).moduleJson()));
    }

    @ParameterizedTest @ValueSource(ints = {404, 500})
    void moduleLookupFailuresExplainCaseSensitiveRegisteredNames(int status) throws Exception {
        withServer(exchange -> respond(exchange, status, "private response"), base -> {
            var failure = assertThrows(IOException.class, () -> RuntimeImportClient.fetch(base,
                    "wrongcase", "", new char[0], false, () -> {}));
            assertTrue(failure.getMessage().contains("case-sensitive registered module name (module.name)"));
            assertFalse(failure.getMessage().contains("private response"));
        });
    }

    @Test void diagnosticsShowTheActualRestUrlAndStatusWithoutCredentialsOrResponseBodies() throws Exception {
        var logs = new ArrayList<String>();
        withServer(exchange -> respond(exchange, 500, "private response password=private-body"), base -> {
            assertThrows(IOException.class, () -> RuntimeImportClient.fetch(
                    URI.create(base + "swagger-ui/index.html?token=private-query#/module"),
                    "Demo module", "private-user", "private-password".toCharArray(), false, () -> {}, logs::add));
            assertEquals(2, logs.size());
            assertTrue(logs.get(0).contains("module metadata via GET " + base.toString().replaceAll("/+$", "")
                    + "/rest/metadata/module/Demo%20module"));
            assertTrue(logs.get(1).contains("returned HTTP 500"));
            String combined = String.join("\n", logs);
            for (String forbidden : List.of("private-user", "private-password", "private-query", "private-body", "Authorization", "swagger-ui"))
                assertFalse(combined.contains(forbidden), forbidden);
        });
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
