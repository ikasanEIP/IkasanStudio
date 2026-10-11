package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestPathListenerTemplateTest {
    @TempDir Path temporary;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void ignoresOnlyDeclaredDebugComponentsAndCleansUpListener(String pack) throws Exception {
        String support = Files.readString(Path.of("src/main/resources/studio/metapack", pack,
                "templates/org/ikasan/studio/generator/moduleFlowTestSupportTemplate_en.ftl"));
        String method = support.substring(support.indexOf("    private IkasanFlowTestRule testHarness("),
                support.indexOf("    private int batchInputCount"))
                .replace("org.ikasan.testharness.flow.listener.FlowEventListenerSubject", "Subject")
                .replace("org.ikasan.flow.event.DefaultReplicationFactory.getInstance()", "null")
                .replace("org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer", "ScheduledConsumer");
        String probe = """
                import java.util.*;
                import java.io.*;
                public class Probe {
                    static class JsonNode implements Iterable<JsonNode> {
                        public JsonNode path(String key) { return this; }
                        public String asText() { return "trace"; }
                        public Iterator<JsonNode> iterator() { return List.of(this).iterator(); }
                    }
                    JsonNode testWiring() { return new JsonNode(); }
                    String getFlowName() { return "flow"; }
                    static class FlowElement {
                        String name; FlowElement(String name) { this.name = name; }
                        String getComponentName() { return name; }
                    }
                    static class FlowEvent { }
                    static class Subject {
                        List<String> calls = new ArrayList<>(); Subject(Object factory) { }
                        public void beforeFlowElement(String m, String f, FlowElement e, FlowEvent event) { calls.add(e.name); }
                        public void afterFlowElement(String m, String f, FlowElement e, FlowEvent event) { calls.add(e.name); }
                    }
                    static class Flow {
                        List<Subject> listeners = new ArrayList<>();
                        void addFlowListener(Subject s) { listeners.add(s); }
                        void removeFlowListener(Subject s) { listeners.remove(s); }
                    }
                    static class ScheduledConsumer {
                        String cron; boolean eager = true;
                        ScheduledConsumer getConfiguration() { return this; }
                        void setCronExpression(String cron) { this.cron = cron; }
                        void setEager(boolean eager) { this.eager = eager; }
                    }
                    static class IkasanFlowTestRule {
                        final boolean allowSchedule;
                        final ScheduledConsumer target = new ScheduledConsumer();
                        IkasanFlowTestRule(boolean allowSchedule) { this.allowSchedule = allowSchedule; }
                        public Object getComponent(String name) { return target; }
                        public IkasanFlowTestRule consumer(String name) { return this; }
                        public IkasanFlowTestRule scheduledConsumer(String name) { return this; }
                        List<String> filters = new ArrayList<>(); boolean failStart, failStop;
                        public IkasanFlowTestRule withFlow(Flow f) { return this; }
                        public IkasanFlowTestRule filter(String name) { filters.add(name); return this; }
                        public void startFlow() { }
                        public void startFlow(Subject s) { if (!allowSchedule) throw new ClassCastException("raw proxy"); if (failStart) throw new IllegalStateException("start"); }
                        public void stopFlow() { if (failStop) throw new IllegalStateException("stop"); }
                    }
                """ + method + """
                    public static void main(String[] args) throws Exception {
                        Flow flow = new Flow();
                        var harness = new Probe().testHarness(flow);
                        harness.filter("trace").filter("businessDebug");
                        if (!harness.filters.equals(List.of("businessDebug"))) throw new AssertionError("Business filter lost");
                        harness.scheduledConsumer("timer");
                        harness.startFlow();
                        if (!"0/5 * * * * ? 2099".equals(harness.target.cron) || harness.target.eager)
                            throw new AssertionError("Timer must remain suppressed before startup");
                        if (flow.listeners.size() != 1) throw new AssertionError("Listener missing");
                        Subject listener = flow.listeners.get(0);
                        for (String name : List.of("trace", "businessDebug", "producer")) {
                            listener.beforeFlowElement("m", "f", new FlowElement(name), new FlowEvent());
                            listener.afterFlowElement("m", "f", new FlowElement(name), new FlowEvent());
                        }
                        if (!listener.calls.equals(List.of("businessDebug", "businessDebug", "producer", "producer")))
                            throw new AssertionError(listener.calls);
                        harness.stopFlow();
                        if (!flow.listeners.isEmpty()) throw new AssertionError("Listener leaked");
                        harness.failStart = true;
                        try { harness.startFlow(); throw new AssertionError("Start failure masked"); }
                        catch (IllegalStateException expected) { }
                        if (!flow.listeners.isEmpty()) throw new AssertionError("Start failure leaked listener");
                        harness.failStart = false; harness.startFlow(); harness.failStop = true;
                        try { harness.stopFlow(); throw new AssertionError("Stop failure masked"); }
                        catch (IllegalStateException expected) { }
                        if (!flow.listeners.isEmpty()) throw new AssertionError("Stop failure leaked listener");
                    }
                }
                """;
        Path source = temporary.resolve("Probe.java");
        Files.writeString(source, probe);
        Path log = temporary.resolve("probe.log");
        String bin = Path.of(System.getProperty("java.home"), "bin").toString();
        var compile = new ProcessBuilder(bin + "/javac", "--release", "11", source.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(compile.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, compile.exitValue(), Files.readString(log));
        } finally { if (compile.isAlive()) compile.destroyForcibly(); }
        var run = new ProcessBuilder(bin + "/java", "-cp", temporary.toString(), "Probe")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(run.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, run.exitValue(), Files.readString(log));
        } finally { if (run.isAlive()) run.destroyForcibly(); }
    }
}
