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
        Path probe = root.resolve("Probe.java");
        Files.writeString(probe, """
                import org.ikasan.studio.flowtests.support.utils.FlowTestFailureDiagnostics;
                import org.junit.Assert;
                import org.junit.ComparisonFailure;
                public class Probe {
                    static class BusinessTest {
                        void assertExpectedOutput(Object expected, Object actual) { Assert.assertEquals(expected, actual); }
                        void verifyReceivedOutput() { receiverHelper(); }
                        void receiverHelper() { Assert.assertEquals("SMTP mailbox delivery count", 1, 2); }
                    }
                    static class InheritedTest extends BusinessTest { }
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
                        var timeout = new AssertionError("Missing output");
                        var fallback = FlowTestFailureDiagnostics.contextualise("Batch timed out", timeout, BusinessTest.class);
                        Assert.assertArrayEquals(timeout.getStackTrace(), fallback.getStackTrace());
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
                "--release", "11", "-cp", junit, "-d", root.toString(), source.toString(), probe.toString())
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
