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

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT", "V5.1.0"})
    void preservesComparisonValuesCauseAndRealAssertionFrame(String version) throws Exception {
        Path templates = version.equals("V5.1.0")
                ? Path.of("headless/studio-pack-v5-release/src/main/resources/studio/metapack", version, "templates/org/ikasan/studio/generator")
                : version.equals("V5.0.1-SNAPSHOT")
                ? Path.of("headless/studio-pack-v5/src/main/resources/studio/metapack", version, "templates/org/ikasan/studio/generator")
                : Path.of("src/main/resources/studio/metapack", version, "templates/org/ikasan/studio/generator");
        Path source = root.resolve("FlowTestFailureDiagnostics.java");
        Files.writeString(source, Files.readString(templates.resolve("flowTestFailureDiagnosticsTemplate_en.ftl")));
        Path batchSource = root.resolve("FlowTestBatch.java");
        Files.writeString(batchSource, Files.readString(templates.resolve("flowTestBatchTemplate_en.ftl")));
        Path textSource = root.resolve("OutputTextSupport.java");
        Files.writeString(textSource, Files.readString(templates.resolve("outputTextSupportTemplate_en.ftl")));
        Path probe = root.resolve("Probe.java");
        Files.writeString(probe, """
                import org.ikasan.studio.flowtests.support.utils.FlowTestFailureDiagnostics;
                import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
                import org.ikasan.studio.flowtests.support.utils.OutputTextSupport;
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
                    static final class Order {
                        @Override public String toString() { throw new AssertionError("Do not stringify business objects for diagnostics"); }
                    }
                    static class TextTest extends BusinessTest {
                        @Override void assertExpectedOutput(Object expected, Object actual) {
                            Assert.assertEquals(expected, OutputTextSupport.stringify(actual));
                        }
                    }
                    static class MappedTextTest extends BusinessTest {
                        @Override void assertExpectedOutput(Object expected, Object actual) {
                            Assert.assertEquals(expected, actual instanceof Order ? "mapped order" : OutputTextSupport.stringify(actual));
                        }
                    }
                    static void verifyMappingDiagnostics() {
                        var test = new TextTest();
                        var batch = new FlowTestBatch<>(List.of(), Map.of("Send for Processing", List.of("expected order")));
                        var observation = batch.observe(test::assertExpectedOutput);
                        observation.accept("Send for Processing", new Order());
                        try {
                            observation.verify(Duration.ofMillis(100), Duration.ofMillis(20), () -> {});
                            throw new IllegalStateException("Expected unsupported mapping");
                        } catch (AssertionError failure) {
                            var report = FlowTestFailureDiagnostics.contextualise(
                                    "Flow 'Create New Order', batch 1 of 2 failed: " + failure.getMessage(), failure, TextTest.class);
                            Assert.assertTrue(report.getMessage().contains("Expected: String (java.lang.String). Actual: Order (" + Order.class.getName() + ")"));
                            Assert.assertTrue(report.getMessage().contains("No text mapping is defined"));
                            Assert.assertTrue(report.getMessage().contains("formatOutputText(Object)"));
                            Assert.assertTrue(report.getMessage().contains("assertExpectedOutput(Object, Object)"));
                            Assert.assertTrue(report.getMessage().contains("Send for Processing"));
                            Assert.assertTrue(report.getMessage().contains("output 1"));
                            Assert.assertTrue(report.getMessage().contains("batch 1 of 2"));
                            Assert.assertSame(failure, report.getCause());
                            Assert.assertTrue(failure.getCause() instanceof OutputTextSupport.UnsupportedOutputTypeException);
                            Assert.assertEquals(TextTest.class.getName(), report.getStackTrace()[0].getClassName());
                            Assert.assertEquals("assertExpectedOutput", report.getStackTrace()[0].getMethodName());
                            Assert.assertTrue(Arrays.asList(failure.getCause().getStackTrace()).contains(report.getStackTrace()[0]));
                        } catch (Exception failure) { throw new AssertionError(failure); }
                        // Different Java types are valid when the developer supplies a text mapping.
                        var mapped = new FlowTestBatch<>(List.of(), Map.of("P", List.of("mapped order")))
                                .observe(new MappedTextTest()::assertExpectedOutput);
                        mapped.accept("P", new Order());
                        try { mapped.verify(Duration.ofMillis(100), Duration.ofMillis(20), () -> {}); }
                        catch (Exception failure) { throw new AssertionError(failure); }
                        // Malformed text is a decoding problem, not an unsupported business-object mapping.
                        var malformed = batch.observe(test::assertExpectedOutput);
                        malformed.accept("Send for Processing", new byte[]{(byte) 0xc3, (byte) 0x28});
                        try {
                            malformed.verify(Duration.ofMillis(100), Duration.ofMillis(20), () -> {});
                            throw new IllegalStateException("Expected malformed text failure");
                        } catch (AssertionError failure) {
                            Assert.assertTrue(failure.getMessage().contains("Cannot decode output content"));
                            Assert.assertFalse(failure.getMessage().contains("No text mapping is defined"));
                            Assert.assertTrue(failure.getCause().getCause() instanceof java.nio.charset.CharacterCodingException);
                        } catch (Exception failure) { throw new AssertionError(failure); }
                    }
                    public static void run() {
                        verifyMappingDiagnostics();
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
                "--release", "11", "-cp", junit, "-d", root.toString(), source.toString(), batchSource.toString(), textSource.toString(), probe.toString())
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
