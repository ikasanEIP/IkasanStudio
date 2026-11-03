package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Exercise the patched broker and client together, isolated from Studio's own dependencies. */
class ActiveMqBrokerRuntimeTest {
    @TempDir Path directory;

    @ParameterizedTest @ValueSource(strings = {"v3", "v4", "v5"})
    void brokerRemainsReadyForLaterDeliveryAndSupportsTransactionRollback(String variant) throws Exception {
        String classpath = Objects.requireNonNull(System.getProperty("studio.activemq." + variant + ".classpath"));
        String namespace = variant.equals("v3") ? "javax.jms" : "jakarta.jms";
        Path probe = directory.resolve("BrokerProbe.java");
        Files.writeString(probe, """
                import JMS.*;
                import org.apache.activemq.ActiveMQConnectionFactory;
                import org.apache.activemq.broker.BrokerService;
                public class BrokerProbe {
                    public static void run() throws Exception {
                        BrokerService broker = new BrokerService();
                        broker.setBrokerName("security-" + java.util.UUID.randomUUID());
                        broker.setPersistent(false);
                        broker.setUseJmx(false);
                        broker.setUseShutdownHook(false);
                        var connector = broker.addConnector("tcp://127.0.0.1:0");
                        try {
                            broker.start();
                            if (!broker.waitUntilStarted(10000)) throw new AssertionError("Broker did not start");
                            var factory = new ActiveMQConnectionFactory(connector.getPublishableConnectString());
                            Connection connection = factory.createConnection();
                            try {
                                Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
                                Queue queue = session.createQueue("security.verification");
                                MessageProducer producer = session.createProducer(queue);
                                MessageConsumer consumer = session.createConsumer(queue);
                                connection.start();
                                producer.send(session.createTextMessage("first"));
                                session.commit();
                                expect(consumer.receive(5000), "first");
                                session.commit();
                                if (consumer.receive(250) != null || !broker.isStarted())
                                    throw new AssertionError("Broker was not idle and ready");
                                producer.send(session.createTextMessage("later"));
                                session.commit();
                                expect(consumer.receive(5000), "later");
                                session.rollback();
                                Message redelivered = consumer.receive(5000);
                                expect(redelivered, "later");
                                if (!redelivered.getJMSRedelivered()) throw new AssertionError("Rollback did not redeliver");
                                session.commit();
                                if (!broker.isStarted()) throw new AssertionError("Broker stopped after delivery");
                                consumer.close(); producer.close(); session.close();
                            } finally { connection.close(); }
                        } finally { broker.stop(); broker.waitUntilStopped(); }
                    }
                    private static void expect(Message message, String expected) throws Exception {
                        if (!(message instanceof TextMessage) || !expected.equals(((TextMessage) message).getText()))
                            throw new AssertionError("Missing or incorrect delivery: " + expected);
                    }
                }
                """.replace("import JMS.*;", "import " + namespace + ".*;"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", variant.equals("v3") ? "11" : "17", "-classpath", classpath,
                "-d", directory.toString(), probe.toString()));
        List<URL> urls = new ArrayList<>(); urls.add(directory.toUri().toURL());
        for (String file : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) urls.add(Path.of(file).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            var thread = Thread.currentThread(); ClassLoader previous = thread.getContextClassLoader();
            try { thread.setContextClassLoader(loader); loader.loadClass("BrokerProbe").getMethod("run").invoke(null); }
            finally { thread.setContextClassLoader(previous); }
        }
    }
}
