package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestBatchTemplateTest {
    @TempDir Path root;
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void verifiesPerProducerCountsOrderingSilenceAndIndependentBatchSizes(String version) throws Exception {
        Path source = root.resolve("FlowTestBatch.java");
        Files.writeString(source, Files.readString(Path.of("src/main/resources/studio/metapack", version,
                "templates/org/ikasan/studio/generator/flowTestBatchTemplate_en.ftl")));
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
                        var objects = new FlowTestBatch<>(List.of(), Map.of("Orders", List.of(new Order(7))));
                        var matching = objects.observe(); matching.accept("Orders", new Order(7));
                        matching.verify(WAIT, QUIET, () -> {});
                        var different = objects.observe(); different.accept("Orders", new Order(8));
                        fails(() -> different.verify(WAIT, QUIET, () -> {}), "Producer 'Orders', output 1");
                        if (!objects.expectedOutputs("Orders").equals(List.of(new Order(7)))) throw new AssertionError("Expected values");
                        var inputs = new ArrayList<>(List.of("one input"));
                        var batch = new FlowTestBatch<>(inputs, Map.of("A",List.of(equal("a1"),equal("a2")),
                                "B",List.of(equal("b")), "Silent",List.of()));
                        inputs.clear();
                        if(batch.inputs().size()!=1 || batch.expectedCount("A")!=2) throw new AssertionError("Counts changed");
                        var observation = batch.observe();
                        observation.accept("B","b"); observation.accept("ignored","anything");
                        observation.accept("A","a1"); observation.accept("A","a2");
                        observation.verify(WAIT,QUIET,()->{}); // One input, three outputs, no global ordering.
                        var wrongOrder = batch.observe();
                        wrongOrder.accept("A","a2"); wrongOrder.accept("A","a1"); wrongOrder.accept("B","b");
                        fails(()->wrongOrder.verify(WAIT,QUIET,()->{}), "Producer 'A', output 1");
                        var missing = batch.observe();
                        fails(()->missing.verify(WAIT,QUIET,()->{}), "Missing producer outputs");
                        var unexpected = batch.observe(); unexpected.accept("Silent","unexpected");
                        fails(()->unexpected.verify(WAIT,QUIET,()->{}), "Unexpected output at producer 'Silent'");
                        var zero = new FlowTestBatch<>(List.of("filtered"),Map.of("A",List.of()));
                        zero.observe().verify(WAIT,QUIET,()->{});
                        var duplicate = new FlowTestBatch<>(List.of(),Map.of("A",List.of(equal("a")))).observe();
                        duplicate.accept("A","a"); duplicate.accept("A","a");
                        fails(()->duplicate.verify(WAIT,QUIET,()->{}), "Unexpected extra output");
                        var flood = zero.observe(); flood.accept("A","a"); flood.accept("A","a");
                        fails(()->flood.verify(WAIT,QUIET,()->{}), "Too many outputs");
                        var late = zero.observe(); late.verify(WAIT,QUIET,()->{}); late.accept("A","late");
                        fails(late::assertNoPending, "Unexpected extra output");
                        fails(()->zero.observe().verify(WAIT,QUIET,()->{throw new AssertionError("processing failed");}), "processing failed");
                        // A fresh observation must not inherit deliveries or counts from an earlier batch.
                        var fresh = batch.observe(); fresh.accept("A","a1"); fresh.accept("B","b"); fresh.accept("A","a2");
                        fresh.verify(WAIT,QUIET,()->{});
                    }
                }
                """);
        Path diagnostics = root.resolve("javac.log");
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-d", root.toString(), source.toString(), probe.toString())
                .redirectErrorStream(true).redirectOutput(diagnostics.toFile()).start();
        try {
            assertTrue(compiler.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, compiler.exitValue(), Files.readString(diagnostics));
        } finally { if (compiler.isAlive()) compiler.destroyForcibly().waitFor(); }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("Probe").getMethod("run").invoke(null);
        }
    }
}
