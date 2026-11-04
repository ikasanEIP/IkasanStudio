package org.ikasan.studio.boot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Studio-owned local development server. H2 is loaded from this project's runtime dependencies. */
public final class IndependentH2 {
    private IndependentH2() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected TCP port and web console port.");
        int tcpPort = port(args[0]), webPort = port(args[1]);
        if (tcpPort == webPort) throw new IllegalArgumentException("H2 TCP and web console ports must differ.");
        // Keep both services local; do not inherit a wider bind address from the parent environment.
        System.setProperty("h2.bindAddress", "127.0.0.1");
        Class<?> server;
        try {
            server = Class.forName("org.h2.tools.Server");
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException("H2 is missing from the project runtime classpath. Reload Maven and check the selected Ikasan dependencies.", missing);
        }
        Object tcp = server.getMethod("createTcpServer", String[].class).invoke(null,
                (Object) new String[]{"-tcpPort", Integer.toString(tcpPort), "-ifNotExists",
                        "-tcpPassword", java.util.UUID.randomUUID().toString()});
        Object web = server.getMethod("createWebServer", String[].class).invoke(null,
                (Object) new String[]{"-webPort", Integer.toString(webPort)});
        AtomicBoolean stopping = new AtomicBoolean();
        CountDownLatch stopped = new CountDownLatch(1);
        Runnable stop = () -> {
            if (!stopping.compareAndSet(false, true)) return;
            try {
                stop(server, web);
                stop(server, tcp);
            } finally {
                stopped.countDown();
                System.out.println("Independent H2 stopped.");
            }
        };
        Thread hook = new Thread(stop, "studio-h2-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            server.getMethod("start").invoke(tcp);
            server.getMethod("start").invoke(web);
            if (!running(server, tcp) || !running(server, web)) throw new IllegalStateException("H2 could not bind its configured ports.");
            System.out.println("IKASAN_STUDIO_H2_READY tcp=" + tcpPort + " web=" + webPort);
            System.out.println("Console: http://127.0.0.1:" + webPort + "/");
            System.out.println("Using H2 " + server.getPackage().getImplementationVersion() + " from the project runtime classpath.");
            Thread control = new Thread(() -> {
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
                    String line;
                    while ((line = input.readLine()) != null) if ("STOP".equals(line)) break;
                } catch (java.io.IOException failure) {
                    System.err.println("H2 control input closed: " + failure.getMessage());
                } finally {
                    stop.run();
                }
            }, "studio-h2-control");
            control.setDaemon(true);
            control.start();
            while (!stopped.await(1, TimeUnit.SECONDS)) {
                if (!running(server, tcp) || !running(server, web)) {
                    stop.run();
                    throw new IllegalStateException("An H2 service stopped unexpectedly. Review the Run console.");
                }
            }
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("H2 could not start. Check that TCP port " + tcpPort
                    + " and web port " + webPort + " are available: " + failure.getCause().getMessage(), failure.getCause());
        } finally {
            stop.run();
            try { Runtime.getRuntime().removeShutdownHook(hook); }
            catch (IllegalStateException shuttingDown) { /* The JVM is already running the hook. */ }
        }
    }

    private static int port(String value) {
        int port = Integer.parseInt(value);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("H2 ports must be between 1 and 65535.");
        return port;
    }
    private static boolean running(Class<?> server, Object instance) throws Exception {
        return Boolean.TRUE.equals(server.getMethod("isRunning", boolean.class).invoke(instance, false));
    }
    private static void stop(Class<?> server, Object instance) {
        try { server.getMethod("stop").invoke(instance); }
        catch (ReflectiveOperationException failure) { System.err.println("H2 shutdown failed: " + failure.getMessage()); }
    }
}
