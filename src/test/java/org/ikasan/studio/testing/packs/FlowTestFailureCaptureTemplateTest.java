package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestFailureCaptureTemplateTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void preservesInvocationAndConfigurationAndSurfacesOriginalFailure(String version) throws Exception {
        Path helper = root.resolve("FlowTestFailureCapture.java");
        Files.writeString(helper, Files.readString(Path.of("src/main/resources/studio/metapack", version,
                "templates/org/ikasan/studio/generator/flowTestFailureCaptureTemplate_en.ftl")));
        Path flow = source("Flow", "public interface Flow { java.util.List<FlowElement<?>> getFlowElements(); }");
        Path element = source("FlowElement", """
                public interface FlowElement<T> {
                    FlowElementInvoker<T> getFlowElementInvoker(); void setFlowElementInvoker(FlowElementInvoker<T> invoker);
                    T getFlowComponent(); String getComponentName();
                }
                """);
        Path event = source("FlowEvent", "public interface FlowEvent { Object getPayload(); }");
        Path invoker = source("FlowElementInvoker", """
                public interface FlowElementInvoker<T> {
                    FlowElement invoke(java.util.List<?> listeners, String module, String flow, Object context,
                        FlowEvent event, FlowElement<T> element);
                }
                """);
        Path probe = root.resolve("Probe.java");
        Files.writeString(probe, """
                import org.ikasan.spec.flow.*;
                import org.ikasan.studio.flowtests.support.utils.FlowTestFailureCapture;
                import java.util.*;
                import java.util.concurrent.LinkedBlockingQueue;
                import java.util.concurrent.TimeUnit;
                public class Probe {
                    public interface Configuration { void setValue(String value); String getValue(); }
                    public static class Invoker implements FlowElementInvoker<Object>, Configuration {
                        RuntimeException failure;
                        String value;
                        public void setValue(String value) { this.value = value; }
                        public String getValue() { return value; }
                        public FlowElement invoke(List<?> listeners, String module, String flow, Object context,
                                FlowEvent event, FlowElement<Object> element) {
                            if (failure != null) throw failure;
                            return element;
                        }
                    }
                    public static class Element implements FlowElement<Object> {
                        FlowElementInvoker<Object> invoker = new Invoker();
                        public FlowElementInvoker<Object> getFlowElementInvoker() { return invoker; }
                        public void setFlowElementInvoker(FlowElementInvoker<Object> invoker) { this.invoker = invoker; }
                        public Object getFlowComponent() { return this; }
                        public String getComponentName() { return "Unwrap order"; }
                    }
                    public static void run() throws Exception {
                        Element element = new Element();
                        Invoker original = (Invoker) element.invoker;
                        Flow flow = () -> List.of(element);
                        try (FlowTestFailureCapture capture = new FlowTestFailureCapture(flow, "Orders")) {
                            Configuration config = (Configuration) element.invoker;
                            config.setValue("configured");
                            if (!"configured".equals(original.value) || !"configured".equals(config.getValue()))
                                throw new AssertionError("Configuration was not delegated");
                            if (element.invoker.invoke(List.of(), "module", "Orders", null, () -> "text", element) != element)
                                throw new AssertionError("Successful result changed");
                            capture.check();
                            var queue = new LinkedBlockingQueue<String>(); queue.add("ok");
                            if (!"ok".equals(capture.poll(queue, 1))) throw new AssertionError("Output lost");
                            original.failure = new ClassCastException("Expected ObjectMessage");
                            try {
                                element.invoker.invoke(List.of(), "module", "Orders", null, () -> "text", element);
                                throw new AssertionError("Exception swallowed");
                            } catch (ClassCastException expected) {
                                if (expected != original.failure) throw new AssertionError("Original cause replaced");
                            }
                            long start = System.nanoTime();
                            try {
                                capture.poll(queue, 10);
                                throw new AssertionError("Failure not reported");
                            } catch (AssertionError expected) {
                                if (expected.getCause() != original.failure || !expected.getMessage().contains("Unwrap order")
                                        || !expected.getMessage().contains("java.lang.String") || !expected.getMessage().contains("Orders"))
                                    throw new AssertionError("Missing context/cause", expected);
                            }
                            if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(1)) throw new AssertionError("Waited for timeout");
                        }
                        if (element.invoker != original) throw new AssertionError("Invoker was not restored");
                        try (FlowTestFailureCapture fresh = new FlowTestFailureCapture(flow, "Orders")) { fresh.check(); }
                    }
                }
                """);
        Path diagnostics = root.resolve("javac.log");
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), helper.toString(), flow.toString(), element.toString(),
                event.toString(), invoker.toString(), probe.toString())
                .redirectErrorStream(true).redirectOutput(diagnostics.toFile()).start();
        try {
            assertTrue(compiler.waitFor(20, TimeUnit.SECONDS), "javac timed out");
            assertEquals(0, compiler.exitValue(), Files.readString(diagnostics));
        } finally { if (compiler.isAlive()) compiler.destroyForcibly().waitFor(); }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("Probe").getMethod("run").invoke(null);
        }
    }

    private Path source(String name, String code) throws Exception {
        return Files.writeString(root.resolve(name + ".java"), "package org.ikasan.spec.flow; " + code);
    }
}
