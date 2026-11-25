package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URLClassLoader;

class FlowTestBatchTemplateTest {
    @TempDir Path root;
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void verifiesPerProducerCountsOrderingSilenceAndIndependentBatchSizes(String version) throws Exception {
        Path templates = version.equals("V5.1.0")
                ? Path.of("headless/studio-pack-v5-release/src/main/resources/studio/metapack", version, "templates/org/ikasan/studio/generator")
                : Path.of("src/main/resources/studio/metapack", version, "templates/org/ikasan/studio/generator");
        Path source = root.resolve("FlowTestBatch.java");
        Files.writeString(source, Files.readString(templates.resolve("flowTestBatchTemplate_en.ftl")));
        Path textSource = root.resolve("OutputTextSupport.java");
        Files.writeString(textSource, Files.readString(templates.resolve("outputTextSupportTemplate_en.ftl")));
        Path probe = root.resolve("Probe.java");
        Files.writeString(probe, """
                import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
                import java.util.*;
                import java.time.Duration;
                public class Probe {
                    static final Duration WAIT = Duration.ofMillis(100), QUIET = Duration.ofMillis(40);
                    static String equal(String expected) { return expected; }
                    static final class Order {
                        final int number;
                        Order(int number) { this.number = number; }
                        public boolean equals(Object other) { return other instanceof Order && ((Order) other).number == number; }
                        public int hashCode() { return number; }
                    }
                    interface Check { void run() throws Exception; }
                    static void fails(Check check, String diagnostic) throws Exception {
                        try { check.run(); } catch (AssertionError expected) {
                            if (!expected.getMessage().contains(diagnostic)) throw expected;
                            return;
                        }
                        throw new AssertionError("Missing failure: " + diagnostic);
                    }
                    public static void run() throws Exception {
                        FlowTestBatch<Object> trigger = FlowTestBatch.triggered(Map.of("P", List.of("value")));
                        if (!trigger.isTriggered() || trigger.invocationCount() != 1 || !trigger.inputs().isEmpty())
                            throw new AssertionError("Trigger has no business inputs but invokes once");
                        FlowTestBatch<Object> observationOnly = new FlowTestBatch<>(List.of(), Map.of());
                        if (observationOnly.isTriggered() || observationOnly.invocationCount() != 0)
                            throw new AssertionError("Empty batches must not trigger");
                        if (new FlowTestBatch<>(List.of("a", "b", "c"), Map.of()).invocationCount() != 3)
                            throw new AssertionError("Input counts must stay dynamic");
                        FlowTestBatch<String> emptyOutput = new FlowTestBatch<>(List.of("input"), Map.of("P", List.of()));
                        if (!emptyOutput.expectedTexts("P").isEmpty()) throw new AssertionError("Expected silence");
                        FlowTestBatch<String> many = new FlowTestBatch<>(List.of("input"), Map.of("P", List.of("a", "b")));
                        if (!many.expectedTexts("P").equals(List.of("a", "b"))) throw new AssertionError("Lost outputs");
                        try { emptyOutput.expectedText("P", 0); throw new AssertionError("Expected guidance"); }
                        catch (IllegalArgumentException expected) {
                            if (!expected.getMessage().contains("0 expected outputs")
                                    || !expected.getMessage().contains("verifyReceivedOutput")) throw new AssertionError(expected);
                        }
                        FlowTestBatch<Object> objects = new FlowTestBatch<>(List.of(), Map.of("Orders", List.of(new Order(7))));
                        FlowTestBatch<Object>.Observation matching = objects.observe(); matching.accept("Orders", new Order(7));
                        matching.verify(WAIT, QUIET, () -> {});
                        FlowTestBatch<Object>.Observation different = objects.observe(); different.accept("Orders", new Order(8));
                        fails(() -> different.verify(WAIT, QUIET, () -> {}), "Producer 'Orders', output 1");
                        if (!objects.expectedOutputs("Orders").equals(List.of(new Order(7)))) throw new AssertionError("Expected values");
                        FlowTestBatch<Object> diagnosticBatch = new FlowTestBatch<>(List.of(), Map.of("Orders", List.of("expected")));
                        AssertionError original = new AssertionError("reference expected:<null> but was:<mr-ref 2000-01-01T00:00:00Z>");
                        FlowTestBatch<Object>.Observation diagnostic = diagnosticBatch.observe((expected, actual) -> { throw original; });
                        diagnostic.accept("Orders", "actual");
                        try {
                            diagnostic.verify(WAIT, QUIET, () -> {});
                            throw new AssertionError("Expected mismatch");
                        } catch (AssertionError failure) {
                            if (!failure.getMessage().contains("Producer 'Orders', output 1 failed: " + original.getMessage()))
                                throw new AssertionError("Missing comparison details", failure);
                            if (failure.getCause() != original) throw new AssertionError("Original cause lost");
                        }
                        FlowTestBatch<Object>.Observation unnamed = diagnosticBatch.observe((expected, actual) -> { throw new IllegalStateException(); });
                        unnamed.accept("Orders", "actual");
                        fails(() -> unnamed.verify(WAIT, QUIET, () -> {}), "java.lang.IllegalStateException");
                        List<String> inputs = new ArrayList<>(List.of("one input"));
                        FlowTestBatch<String> batch = new FlowTestBatch<>(inputs, Map.of("A",List.of(equal("a1"),equal("a2")),
                                "B",List.of(equal("b")), "Silent",List.of()));
                        inputs.clear();
                        if(batch.inputs().size()!=1 || batch.expectedCount("A")!=2) throw new AssertionError("Counts changed");
                        FlowTestBatch<String>.Observation observation = batch.observe();
                        observation.accept("B","b"); observation.accept("ignored","anything");
                        observation.accept("A","a1"); observation.accept("A","a2");
                        observation.verify(WAIT,QUIET,()->{}); // One input, three outputs, no global ordering.
                        FlowTestBatch<String>.Observation wrongOrder = batch.observe();
                        wrongOrder.accept("A","a2"); wrongOrder.accept("A","a1"); wrongOrder.accept("B","b");
                        fails(()->wrongOrder.verify(WAIT,QUIET,()->{}), "Producer 'A', output 1");
                        FlowTestBatch<String>.Observation missing = batch.observe();
                        fails(()->missing.verify(WAIT,QUIET,()->{}), "Missing producer outputs");
                        FlowTestBatch<String>.Observation unexpected = batch.observe(); unexpected.accept("Silent","unexpected");
                        fails(()->unexpected.verify(WAIT,QUIET,()->{}), "Too many outputs at producer 'Silent'");
                        FlowTestBatch<String> zero = new FlowTestBatch<>(List.of("filtered"),Map.of("A",List.of()));
                        zero.observe().verify(WAIT,QUIET,()->{});
                        FlowTestBatch<Object>.Observation duplicate = new FlowTestBatch<>(List.of(),Map.of("A",List.of(equal("a")))).observe();
                        duplicate.accept("A","a"); duplicate.accept("A","a");
                        fails(()->duplicate.verify(WAIT,QUIET,()->{}), "Too many outputs");
                        FlowTestBatch<Object>.Observation independent = new FlowTestBatch<>(List.of(), Map.of("A", List.of("a"), "B", List.of("b", "c", "d"))).observe();
                        independent.accept("A", "a"); independent.accept("A", "extra");
                        fails(() -> independent.verify(WAIT, QUIET, () -> {}), "Expected: 1; observed: at least 2");
                        FlowTestBatch<String>.Observation flood = zero.observe(); flood.accept("A","a"); flood.accept("A","a");
                        fails(()->flood.verify(WAIT,QUIET,()->{}), "Expected: 0; observed: at least 1");
                        for (int i = 0; i < 10000; i++) flood.accept("A", "more");
                        fails(flood::assertNoPending, "Expected: 0; observed: at least 1");
                        FlowTestBatch<String>.Observation late = zero.observe(); late.verify(WAIT,QUIET,()->{}); late.accept("A","late");
                        fails(late::assertNoPending, "Too many outputs");
                        fails(()->zero.observe().verify(WAIT,QUIET,()->{throw new AssertionError("processing failed");}), "processing failed");
                        // A fresh observation must not inherit deliveries or counts from an earlier batch.
                        FlowTestBatch<String>.Observation fresh = batch.observe(); fresh.accept("A","a1"); fresh.accept("B","b"); fresh.accept("A","a2");
                        fresh.verify(WAIT,QUIET,()->{});
                    }
                }
                """);
        Path diagnostics = root.resolve("javac.log");
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), source.toString(), textSource.toString(), probe.toString())
                .redirectErrorStream(true).redirectOutput(diagnostics.toFile()).start();
        try {
            assertTrue(compiler.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, compiler.exitValue(), Files.readString(diagnostics));
        } finally { if (compiler.isAlive()) compiler.destroyForcibly().waitFor(); }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("Probe").getMethod("run").invoke(null);
        }
    }
}
