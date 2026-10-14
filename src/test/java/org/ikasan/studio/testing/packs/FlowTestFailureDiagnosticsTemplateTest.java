package org.ikasan.studio.testing.packs;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestFailureDiagnosticsTemplateTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void preservesComparisonValuesCauseAndRealAssertionFrame(String version) throws Exception {
        Path source = root.resolve("FlowTestFailureDiagnostics.java");
        Files.writeString(source, Files.readString(Path.of("src/main/resources/studio/metapack", version,
                "templates/org/ikasan/studio/generator/flowTestFailureDiagnosticsTemplate_en.ftl")));
        Path batchSource = root.resolve("FlowTestBatch.java");
        Files.writeString(batchSource, Files.readString(Path.of("src/main/resources/studio/metapack", version,
                "templates/org/ikasan/studio/generator/flowTestBatchTemplate_en.ftl")));
        Path probe = root.resolve("Probe.java");
        Files.writeString(probe, """
                import org.ikasan.studio.flowtests.support.utils.FlowTestFailureDiagnostics;
                import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
                import java.util.*;
                import java.time.Duration;
                import org.junit.Assert;
                import org.junit.ComparisonFailure;
                public class Probe {
                    static class BusinessTest {
                        void assertExpectedOutput(Object expected, Object actual) { Assert.assertEquals(expected, actual); }
                        void verifyReceivedOutput() { receiverHelper(); }
                        List<FlowTestBatch<Object>> createInputOutputBatches() {
                            return List.of(
                                new FlowTestBatch<>(List.of(), Map.of("P", List.of("a"))),
                                FlowTestBatch.triggered(Map.of("P", List.of())));
                        }
                        void defineExpectedPath() { pathHelper(); }
                        void pathHelper() { throw new AssertionError("Unknown producer"); }
                        void receiverHelper() { Assert.assertEquals("SMTP mailbox delivery count", 1, 2); }
                    }
                    static class InheritedTest extends BusinessTest { }
                    static class DecodeTest extends BusinessTest {
                        @Override void assertExpectedOutput(Object expected, Object actual) {
                            throw new IllegalArgumentException("Cannot read output text from directory; review flowTestProperties()");
                        }
                    }
                    public static void run() {
                        AssertionError original;
                        try {
                            new InheritedTest().assertExpectedOutput("expected", "actual");
                            throw new IllegalStateException("Expected failure");
                        } catch (AssertionError failure) { original = failure; }
                        var producer = new AssertionError("Producer 'Email', output 1 failed: " + original.getMessage(), original);
                        var reported = FlowTestFailureDiagnostics.contextualise("Flow 'Example', batch 1: " + producer.getMessage(),
                                producer, InheritedTest.class);
                        if (!(reported instanceof ComparisonFailure)) throw new AssertionError("Comparison type lost");
                        var comparison = (ComparisonFailure) reported;
                        Assert.assertEquals("expected", comparison.getExpected());
                        Assert.assertEquals("actual", comparison.getActual());
                        Assert.assertSame(producer, reported.getCause());
                        Assert.assertSame(original, reported.getCause().getCause());
                        Assert.assertTrue(reported.getMessage().contains("batch 1"));
                        Assert.assertTrue(reported.getMessage().contains("Producer 'Email'"));
                        var first = reported.getStackTrace()[0];
                        Assert.assertEquals(BusinessTest.class.getName(), first.getClassName());
                        Assert.assertEquals("assertExpectedOutput", first.getMethodName());
                        Assert.assertTrue(first.getLineNumber() > 0);
                        Assert.assertTrue(java.util.Arrays.asList(original.getStackTrace()).contains(first));
                        try { new BusinessTest().assertExpectedOutput(1, 2); }
                        catch (AssertionError business) {
                            var result = FlowTestFailureDiagnostics.contextualise("Business mismatch", business, BusinessTest.class);
                            Assert.assertEquals("assertExpectedOutput", result.getStackTrace()[0].getMethodName());
                            Assert.assertSame(business, result.getCause());
                        }
                        try {
                            new InheritedTest().verifyReceivedOutput();
                            throw new IllegalStateException("Expected receiver failure");
                        } catch (AssertionError receiver) {
                            var result = FlowTestFailureDiagnostics.contextualise("Flow 'Example', batch 1: " + receiver.getMessage(),
                                    receiver, InheritedTest.class);
                            Assert.assertEquals("verifyReceivedOutput", result.getStackTrace()[0].getMethodName());
                            Assert.assertEquals(BusinessTest.class.getName(), result.getStackTrace()[0].getClassName());
                            Assert.assertTrue(result.getMessage().contains("expected:<1> but was:<2>"));
                            Assert.assertSame(receiver, result.getCause());
                            Assert.assertTrue(java.util.Arrays.asList(receiver.getStackTrace()).contains(result.getStackTrace()[0]));
                        }
                        try { new DecodeTest().assertExpectedOutput("text", null); }
                        catch (IllegalArgumentException decoding) {
                            var wrapped = new AssertionError("Producer output failed", decoding);
                            var result = FlowTestFailureDiagnostics.contextualise(wrapped.getMessage(), wrapped, DecodeTest.class);
                            Assert.assertEquals("assertExpectedOutput", result.getStackTrace()[0].getMethodName());
                            Assert.assertEquals(DecodeTest.class.getName(), result.getStackTrace()[0].getClassName());
                            Assert.assertSame(wrapped, result.getCause());
                        }
                        var timeout = new AssertionError("Missing output");
                        var fallback = FlowTestFailureDiagnostics.contextualise("Batch timed out", timeout, BusinessTest.class);
                        Assert.assertArrayEquals(timeout.getStackTrace(), fallback.getStackTrace());
                        Assert.assertTrue(fallback.getMessage().contains(BusinessTest.class.getName()));
                        try { new BusinessTest().defineExpectedPath(); }
                        catch (AssertionError path) {
                            var result = FlowTestFailureDiagnostics.contextualise(path.getMessage(), path, BusinessTest.class);
                            Assert.assertEquals("defineExpectedPath", result.getStackTrace()[0].getMethodName());
                        }
                        var batches = new BusinessTest().createInputOutputBatches();
                        int previousLine = -1;
                        for (var batch : batches) {
                            var observation = batch.observe();
                            // Arrivals on another thread must still navigate to this batch's definition.
                            Thread worker = new Thread(() -> {
                                observation.accept("P", "a"); observation.accept("P", "excess");
                            });
                            worker.start();
                            try { worker.join(); } catch (InterruptedException e) { throw new AssertionError(e); }
                            try { observation.assertNoPending(); throw new IllegalStateException("Expected excess"); }
                            catch (AssertionError excess) {
                                var result = FlowTestFailureDiagnostics.contextualise("Flow 'Example', batch: " + excess.getMessage(), excess, BusinessTest.class);
                                Assert.assertTrue(result.getMessage().contains(BusinessTest.class.getName()));
                                Assert.assertTrue(result.getMessage().contains("createInputOutputBatches"));
                                Assert.assertTrue(result.getMessage().contains("observed: at least"));
                                Assert.assertEquals("createInputOutputBatches", result.getStackTrace()[0].getMethodName());
                                Assert.assertTrue(result.getStackTrace()[0].getLineNumber() > previousLine);
                                previousLine = result.getStackTrace()[0].getLineNumber();
                                Assert.assertSame(excess, result.getCause());
                                Assert.assertEquals("countFailure", excess.getStackTrace()[0].getMethodName());
                                var outer = FlowTestFailureDiagnostics.contextualise(result.getMessage(), result, BusinessTest.class);
                                Assert.assertEquals(result.getMessage(), outer.getMessage());
                                Assert.assertArrayEquals(result.getStackTrace(), outer.getStackTrace());
                            }
                        }
                        var missing = batches.get(0).observe();
                        try { missing.verify(Duration.ofMillis(25), Duration.ofMillis(25), () -> {}); throw new IllegalStateException("Expected missing output"); }
                        catch (AssertionError failure) {
                            var result = FlowTestFailureDiagnostics.contextualise(failure.getMessage(), failure, BusinessTest.class);
                            Assert.assertEquals("createInputOutputBatches", result.getStackTrace()[0].getMethodName());
                            Assert.assertTrue(result.getMessage().contains("Missing producer outputs"));
                        } catch (Exception failure) { throw new AssertionError(failure); }
                        // Unrelated assertion classes must not masquerade as this test's assertion.
                        var unrelated = FlowTestFailureDiagnostics.contextualise("Other failure", original, Probe.class);
                        Assert.assertArrayEquals(original.getStackTrace(), unrelated.getStackTrace());
                    }
                }
                """);
        URL junitResource = org.junit.Assert.class.getResource("/org/junit/Assert.class");
        assertNotNull(junitResource, "JUnit Assert.class must be available to compile the diagnostics probe");
        String resource = junitResource.toExternalForm();
        String junit = Path.of(URI.create(resource.substring(4, resource.indexOf("!/")))).toString();
        Path log = root.resolve("compile.log");
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "11", "-cp", junit, "-d", root.toString(), source.toString(), batchSource.toString(), probe.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(compiler.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, compiler.exitValue(), Files.readString(log));
        } finally { if (compiler.isAlive()) compiler.destroyForcibly().waitFor(); }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, getClass().getClassLoader())) {
            loader.loadClass("Probe").getMethod("run").invoke(null);
        }
    }
}
