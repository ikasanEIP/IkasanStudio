package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.FreemarkerUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ActiveMqTrustedPackagesRuntimeTest {
    @TempDir Path temporary;

    @ParameterizedTest
    @ValueSource(strings = {"v3", "v4"})
    void generatedFactoryPreservesJndiAndXaAndRestrictsDeserialization(String variant) throws Exception {
        String pack = variant.equals("v3") ? "V3.3.9" : "V4.1.6";
        String classpath = Objects.requireNonNull(System.getProperty("studio.activemq." + variant + ".classpath"));
        var component = TrustedObjectPackagesTemplateTest.jms(pack, "Consumer");
        String helper = FreemarkerUtils.generateFromTemplate(pack, "trustedObjectPackages_en.ftl",
                new HashMap<>(Map.of("flowElement", component, "trustedPackages", "example.orders, example.shared")));
        Path generated = temporary.resolve("GeneratedTrust.java");
        Files.writeString(generated, "public class GeneratedTrust {\n" + helper + "\n}");
        Path orderSource = temporary.resolve("example/orders/Order.java");
        Files.createDirectories(orderSource.getParent());
        Files.writeString(orderSource, "package example.orders; public class Order implements java.io.Serializable { public String reference = \"ABC123\"; }");
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler.run(null, null, null, "--release", variant.equals("v3") ? "11" : "17",
                "-classpath", classpath, "-d", temporary.toString(), generated.toString(), orderSource.toString())).isZero();
        List<URL> urls = new ArrayList<>();
        urls.add(temporary.toUri().toURL());
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            urls.add(Path.of(entry).toUri().toURL());
        }
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);
            try {
                Class<?> helperClass = loader.loadClass("GeneratedTrust$TrustedOrdersContextFactory");
                assertThat(helperClass.getMethod("initialFactoryName", String.class).invoke(null,
                        "org.apache.activemq.jndi.ActiveMQInitialContextFactory")).isEqualTo(helperClass.getName());
                assertThatThrownBy(() -> helperClass.getMethod("initialFactoryName", String.class).invoke(null,
                        "example.OtherProvider")).isInstanceOf(InvocationTargetException.class)
                        .hasCauseInstanceOf(IllegalArgumentException.class);
                var initial = (javax.naming.spi.InitialContextFactory) helperClass.getConstructor().newInstance();
                var environment = new Hashtable<String, Object>();
                environment.put(javax.naming.Context.PROVIDER_URL, "tcp://localhost:61616");
                environment.put("connectionFactoryNames", "ConnectionFactory,XAConnectionFactory");
                var context = initial.getInitialContext(environment);
                try {
                    Object factory = context.lookup("ConnectionFactory");
                    Class<?> activeMq = loader.loadClass("org.apache.activemq.ActiveMQConnectionFactory");
                    Object defaults = activeMq.getConstructor().newInstance();
                    @SuppressWarnings("unchecked")
                    List<String> defaultPackages = (List<String>) activeMq.getMethod("getTrustedPackages").invoke(defaults);
                    @SuppressWarnings("unchecked")
                    List<String> trusted = (List<String>) activeMq.getMethod("getTrustedPackages").invoke(factory);
                    assertThat(trusted).containsAll(defaultPackages).contains("example.orders", "example.shared").doesNotContain("*");
                    assertThat(activeMq.getMethod("isTrustAllPackages").invoke(factory)).isEqualTo(false);
                    assertThat(activeMq.getMethod("getBrokerURL").invoke(factory)).isEqualTo("tcp://localhost:61616");
                    assertThat(context.lookup("XAConnectionFactory").getClass().getName()).isEqualTo("org.apache.activemq.ActiveMQXAConnectionFactory");

                    Serializable order = (Serializable) loader.loadClass("example.orders.Order").getConstructor().newInstance();
                    Class<?> messageType = loader.loadClass("org.apache.activemq.command.ActiveMQObjectMessage");
                    Object outgoing = messageType.getConstructor().newInstance();
                    messageType.getMethod("setObject", Serializable.class).invoke(outgoing, order);
                    messageType.getMethod("storeContent").invoke(outgoing);
                    Object content = messageType.getMethod("getContent").invoke(outgoing);
                    Object incoming = messageType.getConstructor().newInstance();
                    messageType.getMethod("setContent", content.getClass()).invoke(incoming, content);
                    messageType.getMethod("setTrustedPackages", List.class).invoke(incoming, defaultPackages);
                    assertThatThrownBy(() -> messageType.getMethod("getObject").invoke(incoming))
                            .hasStackTraceContaining("Forbidden class example.orders.Order");
                    messageType.getMethod("setTrustedPackages", List.class).invoke(incoming, trusted);
                    Object received = messageType.getMethod("getObject").invoke(incoming);
                    assertThat(received.getClass().getField("reference").get(received)).isEqualTo("ABC123");
                } finally {
                    context.close();
                }
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        }
    }
}
