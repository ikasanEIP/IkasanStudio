package org.ikasan.studio.integration.ikasan;

import com.sun.net.httpserver.HttpServer;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.OutputStream;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class ModuleControlFailureInjectionTest {
    @Test
    void unavailableMalformedAndHttpFailureRecoverOnNextPoll() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicInteger mode = new AtomicInteger(503);
        server.createContext("/", exchange -> {
            String body = mode.get() == 200 ? "{\"flows\":[{\"name\":\"flow\",\"state\":\"running\"}]}" : "{";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(mode.get() == 201 ? 200 : mode.get(), bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        });
        Module module = module(server.getAddress().getPort());
        server.start();
        try {
            assertThrows(java.io.IOException.class, () -> ModuleControlClient.fetchFlowStates(module));
            mode.set(201);
            assertThrows(java.io.IOException.class, () -> ModuleControlClient.fetchFlowStates(module));
            mode.set(200);
            assertEquals("running", ModuleControlClient.fetchFlowStates(module).get("flow"));
        } finally { server.stop(0); }
        assertThrows(java.io.IOException.class, () -> ModuleControlClient.fetchFlowStates(module));
    }

    @Test
    void slowEndpointTimesOutWhileSwingRemainsResponsive() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            accepted.countDown();
            // Return value intentionally unused - exchange.close() below runs either way, regardless of
            // whether release counted down or this safety timeout elapsed first.
            try {
                @SuppressWarnings("unused") boolean ignored = release.await(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        Module module = module(server.getAddress().getPort());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        server.start();
        try {
            Future<HttpTimeoutException> call = pool.submit(() -> assertThrows(java.net.http.HttpTimeoutException.class,
                    () -> ModuleControlClient.fetchFlowStates(module)));
            assertTrue(accepted.await(5, TimeUnit.SECONDS));
            CompletableFuture<Boolean> heartbeat = new CompletableFuture<>();
            javax.swing.SwingUtilities.invokeLater(() -> heartbeat.complete(true));
            assertTrue(heartbeat.get(2, TimeUnit.SECONDS));
            call.get(8, TimeUnit.SECONDS);
        } finally { release.countDown(); server.stop(0); pool.shutdownNow(); }
    }

    private static Module module(int port) {
        Module module = mock(Module.class);
        when(module.getIdentity()).thenReturn("test");
        when(module.getPort()).thenReturn(Integer.toString(port));
        return module;
    }
}
