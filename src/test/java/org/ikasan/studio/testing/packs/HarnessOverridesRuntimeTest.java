package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.HarnessOverridesTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the exact generated adapter, rather than comparing its source to itself. */
class HarnessOverridesRuntimeTest {
    @TempDir Path root;
    @Test void mixedConnectionsRetainConfiguredValuesAndUseSeparateRuntimeIdentities() throws Exception {
        Path source = root.resolve("StudioHarnessOverrides.java");
        Files.writeString(source, HarnessOverridesTemplate.javaSource());
        Path env = root.resolve("Environment.java");
        Files.writeString(env, """
                package org.springframework.core.env;
                public interface Environment {
                    String getRequiredProperty(String key);
                    <T> T getRequiredProperty(String key, Class<T> type);
                }
                """);
        Path endpoint = root.resolve("Endpoint.java");
        Files.writeString(endpoint, """
                public class Endpoint {
                    public String id = "development";
                    public final Configuration configuration = new Configuration();
                    public Configuration getConfiguration() { return configuration; }
                    public void setConfiguredResourceId(String id) { this.id = id; }
                    public static class Configuration {
                        public String host = "dev.example.com", destination = "dev.queue", directory = "/dev";
                        public int port = 22;
                        public String password = "dev-secret", user = "dev-user", protocol = "smtps";
                        public Object factory = new Object();
                        public java.util.Map<String, String> session = java.util.Map.of("mail.smtp.auth", "true");
                        public void setRemoteHost(String v) { host = v; }
                        public void setRemotePort(Integer v) { port = v; }
                        public void setUsername(String v) { user = v; }
                        public void setPassword(String v) { password = v; }
                        public void setPasswordFilePath(String v) {}
                        public void setPrivateKeyFilename(String v) {}
                        public void setPrivateKeyPassphrase(String v) {}
                        public void setKnownHostsFilename(String v) {}
                        public void setPreferredKeyExchangeAlgorithm(String v) {}
                        public void setSourceDirectory(String v) { directory = v; }
                        public void setOutputDirectory(String v) { directory = v; }
                        public void setSourceDirectoryURLFactory(Object v) { factory = v; }
                        public void setMinAge(Long v) {}
                        public void setMailHost(String v) { host = v; }
                        public void setMailSmtpHost(String v) { host = v; }
                        public void setMailSmtpPort(int v) { port = v; }
                        public void setMailSmtpUser(String v) { user = v; }
                        public void setUser(String v) { user = v; }
                        public void setMailTransportProtocol(String v) { protocol = v; }
                        public void setExtendedMailSessionProperties(java.util.Map<String,String> v) { session = v; }
                        public void setDestinationJndiName(String v) { destination = v; }
                    }
                }
                """);
        var compile = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), source.toString(), env.toString(), endpoint.toString())
                .redirectErrorStream(true).redirectOutput(root.resolve("compiler.log").toFile()).start();
        try {
            assertTrue(compile.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0, compile.exitValue(), Files.readString(root.resolve("compiler.log")));
        } finally { if (compile.isAlive()) compile.destroyForcibly(); }
        Map<String, String> saved = new HashMap<>();
        for (String key : List.of("studio.harness.components", "studio.harness.session", "studio.harness.ftp.home")) saved.put(key, System.getProperty(key));
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> environment = loader.loadClass("org.springframework.core.env.Environment");
            Class<?> endpointClass = loader.loadClass("Endpoint");
            Class<?> adapter = loader.loadClass("org.ikasan.studio.boot.StudioHarnessOverrides");
            Map<String, String> settings = new HashMap<>();
            settings.put("studio.harness.ftp.host", "127.0.0.1"); settings.put("studio.harness.ftp.port", "2121");
            settings.put("studio.harness.ftp.username", "ikasan"); settings.put("studio.harness.ftp.password", "ikasan");
            settings.put("studio.harness.email.host", "127.0.0.1"); settings.put("studio.harness.email.port", "1025");
            settings.put("studio.harness.flow.jms.destination", "dynamicQueues/private-test");
            Object properties = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{environment}, (p,m,args) -> {
                if (m.getDeclaringClass() == Object.class) {
                    return switch (m.getName()) {
                        case "equals" -> p == args[0];
                        case "hashCode" -> System.identityHashCode(p);
                        case "toString" -> "Harness test environment";
                        default -> throw new UnsupportedOperationException(m.toString());
                    };
                }
                if (!"getRequiredProperty".equals(m.getName()) || args == null || !(args[0] instanceof String key))
                    throw new UnsupportedOperationException(m.toString());
                String value = Objects.requireNonNull(settings.get(key));
                return args.length == 2 && args[1] == Integer.class ? Integer.valueOf(value) : value;
            });
            Method apply = adapter.getMethod("apply", environment, Object.class, String.class, String.class, boolean.class);
            System.setProperty("studio.harness.components", "flow.ftp,flow.mail,flow.jms");
            System.setProperty("studio.harness.session", "launch-one");
            System.setProperty("studio.harness.ftp.home", root.toString());
            Object configured = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, configured, "flow.remote", "SFTP", true);
            assertEquals("dev.example.com", value(configured, "host"));
            assertEquals("development", endpointClass.getField("id").get(configured));
            Object ftp = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, ftp, "flow.ftp", "FTP", true);
            assertEquals("127.0.0.1", value(ftp, "host")); assertEquals(2121, value(ftp, "port"));
            assertEquals("/component-flow.ftp", value(ftp, "directory")); assertNull(value(ftp, "factory"));
            assertTrue(endpointClass.getField("id").get(ftp).toString().startsWith("studio-harness-launch-one-"));
            Object mail = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, mail, "flow.mail", "Email", false);
            assertEquals(1025, value(mail, "port")); assertEquals("smtp", value(mail, "protocol"));
            assertNull(value(mail, "password")); assertEquals(Map.of(), value(mail, "session"));
            Object jms = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, jms, "flow.jms", "JMS", false);
            assertEquals("dynamicQueues/private-test", value(jms, "destination"));
            Object reader = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, reader, "flow.jms", "JMS", true);
            assertNotEquals(endpointClass.getField("id").get(jms), endpointClass.getField("id").get(reader));
            System.setProperty("studio.harness.components", "");
            Object nextLaunch = endpointClass.getConstructor().newInstance();
            apply.invoke(null, properties, nextLaunch, "flow.ftp", "FTP", true);
            assertEquals("dev.example.com", value(nextLaunch, "host"));
            assertEquals("127.0.0.1", value(ftp, "host")); // The first launch remains unchanged.
        } finally {
            saved.forEach((key,value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key,value); });
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void inventoryAndFactoryUseTheSameComponentIdentity(String pack) throws Exception {
        var producer = org.ikasan.studio.core.TestFixtures.getEmailProducer(pack);
        producer.setComponentName("Local email");
        var flow = new org.ikasan.studio.core.model.ikasan.instance.Flow(pack);
        flow.setName("Orders / 日本語");
        producer.setContainingFlow(flow);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(producer);
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(pack,
                new ArrayList<>(List.of(flow)));
        var inventory = HarnessOverridesTemplate.launchTargets(HarnessOverridesTemplate.create(module));
        assertEquals(1, inventory.size());
        String id = org.ikasan.studio.core.model.analysis.HarnessOverrides.id(producer);
        assertEquals(id, inventory.get(0).id());
        assertTrue(inventory.get(0).label().contains("日本語"));
        String factory = org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.create(
                "org.ikasan.studio.boot.flow." + flow.getJavaPackageName(), module, flow);
        assertTrue(factory.contains("StudioHarnessOverrides.apply(studioHarnessEnvironment,"));
        assertTrue(factory.contains("\"" + id + "\""));
        assertTrue(org.ikasan.studio.core.generator.PropertiesTemplate.create(module)
                .contains("spring.config.import=optional:classpath:studio-harness.properties"));
    }

    private Object value(Object endpoint, String field) throws Exception {
        Object config = endpoint.getClass().getField("configuration").get(endpoint);
        return config.getClass().getField(field).get(config);
    }
}
