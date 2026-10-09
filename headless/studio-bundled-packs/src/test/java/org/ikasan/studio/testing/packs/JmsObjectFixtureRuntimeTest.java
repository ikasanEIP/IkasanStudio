package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;

class JmsObjectFixtureRuntimeTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(strings = {"v3", "v4"})
    void sendsObjectMessageAndClosesProducerOnSuccessAndFailure(String variant) throws Exception {
        String pack = variant.equals("v3") ? "V3.3.9" : "V4.1.6";
        String namespace = variant.equals("v3") ? "javax.jms" : "jakarta.jms";
        String classpath = Objects.requireNonNull(System.getProperty("studio.activemq." + variant + ".classpath"));
        List<Path> sources = new ArrayList<>();
        sources.add(write("JmsFlowTestSupport", org.ikasan.studio.core.generator.FreemarkerUtils.generateFromTemplate(pack,
                "jmsFlowTestSupportTemplate_en.ftl", new HashMap<>())));
        sources.add(write("ModuleFlowTestSupport", """
                package org.ikasan.studio.flowtests.support;
                public class ModuleFlowTestSupport { public static int deliveryTimeoutSeconds(String s) { return Integer.parseInt(s); } }
                """));
        sources.add(write("ConfigurableApplicationContext", """
                package org.springframework.context;
                public interface ConfigurableApplicationContext {
                    <T> T getBean(String name, Class<T> type); Environment getEnvironment();
                    interface Environment { String getProperty(String key, String fallback); }
                }
                """));
        // Only sendObject is exercised; these JUnit methods are needed to compile the other helper methods.
        sources.add(write("Assert", """
                package org.junit;
                public class Assert {
                    public static void assertEquals(Object a, Object b) { throw new AssertionError("Unexpected assertion"); }
                    public static void assertTrue(String s, boolean b) { throw new AssertionError("Unexpected assertion"); }
                    public static void assertNull(String s, Object o) { throw new AssertionError("Unexpected assertion"); }
                }
                """));
        sources.add(write("Probe", """
                import JMS.*;
                import java.lang.reflect.Proxy;
                import org.apache.activemq.command.ActiveMQObjectMessage;
                import org.apache.activemq.command.ActiveMQQueue;
                import org.ikasan.studio.flowtests.support.utils.JmsFlowTestSupport;
                public class Probe {
                    interface Handler { Object call(String name, Object[] args) throws Throwable; }
                    static <T> T proxy(Class<T> type, Handler handler) {
                        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                                (p,m,a) -> handler.call(m.getName(),a)));
                    }
                    public static void run() throws Exception {
                        int[] closed = {0,0}; Object[] sent = {null}; boolean[] fail = {false};
                        MessageProducer producer = proxy(MessageProducer.class, (name,args) -> {
                            if (name.equals("close")) closed[0]++;
                            if (name.equals("send")) { if (fail[0]) throw new JMSException("send failed"); sent[0]=args[0]; }
                            return null;
                        });
                        Session session = proxy(Session.class, (name,args) -> {
                            switch(name) {
                                case "createQueue": return new ActiveMQQueue((String)args[0]);
                                case "createProducer": return producer;
                                case "createObjectMessage":
                                    var message = new ActiveMQObjectMessage(); message.setObject((java.io.Serializable)args[0]); return message;
                                default: throw new AssertionError("Unexpected session operation: " + name);
                            }
                        });
                        Connection connection = proxy(Connection.class, (name,args) -> {
                            if(name.equals("createSession")) return session;
                            if(name.equals("close")) closed[1]++;
                            return null;
                        });
                        ConnectionFactory factory = proxy(ConnectionFactory.class, (name,args) -> connection);
                        try (var helper = new JmsFlowTestSupport(factory)) {
                            var payload = new java.util.ArrayList<String>(); payload.add("order-1");
                            helper.sendObject("test.orders",payload);
                            if(!(sent[0] instanceof ObjectMessage) || !payload.equals(((ObjectMessage)sent[0]).getObject()))
                                throw new AssertionError("Not an object message containing the fixture");
                            if(closed[0]!=1) throw new AssertionError("Producer leaked");
                            try { helper.sendObject("test.orders",null); throw new AssertionError("Null accepted"); }
                            catch(IllegalArgumentException expected) { }
                            fail[0]=true;
                            try { helper.sendObject("test.orders",payload); throw new AssertionError("Send failure swallowed"); }
                            catch(JMSException expected) { }
                            if(closed[0]!=2) throw new AssertionError("Failed producer leaked");
                        }
                        if(closed[1]!=1) throw new AssertionError("Connection leaked");
                    }
                }
                """.replace("import JMS.*;", "import " + namespace + ".*;")));
        List<String> args = new ArrayList<>(List.of("--release", variant.equals("v3") ? "11" : "17", "-cp", classpath, "-d", root.toString()));
        sources.forEach(p -> args.add(p.toString()));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)));
        List<URL> urls = new ArrayList<>(); urls.add(root.toUri().toURL());
        for (String entry : classpath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) urls.add(Path.of(entry).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("Probe").getMethod("run").invoke(null);
        }
    }
    private Path write(String name, String code) throws Exception { return Files.writeString(root.resolve(name + ".java"), code); }
}
