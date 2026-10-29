package org.ikasan.studio.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Deliberate commit failures use real generated flows and real local endpoints. */
class RollbackFixtureTest {
    @TempDir Path temporary;

    @Test void exportsRollbackScenarios() throws Exception {
        Path root = Path.of(System.getProperty("studio.v5.rollback", temporary.toString()));
        var scenarios = List.of("jms", "ftp-output", "sftp-output");
        for (String scenario : scenarios) {
            var files = RuntimeTransportFixtureTest.render(scenario, flow -> {
                if (scenario.equals("jms")) {
                    // This failure reaches the consumer error handler after the transaction has rolled back.
                    // Only this test fault is ignored: let the broker retry without stopping the flow.
                    var resolution = org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution
                            .exceptionResolutionBuilder().metapackVersion(DevelopmentFixtureTest.PACK)
                            .exceptionsCaught("org.example.transport.transport.CommitFailure.class").theAction("ignoreException")
                            .componentProperties(new LinkedHashMap<>()).build();
                    flow.setExceptionResolver(org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver
                            .exceptionResolverBuilder().metapackVersion(DevelopmentFixtureTest.PACK)
                            .ikasanExceptionResolutionMap(Map.of("org.example.transport.transport.CommitFailure.class", resolution)).build());
                }
            });
            files.put("src/main/java/org/example/transport/transport/CommitFailure.java",
                    "package org.example.transport.transport; public class CommitFailure extends RuntimeException {"
                    + "public CommitFailure() { super(\"Injected commit failure after producer delivery\"); }}");
            if (scenario.equals("jms")) {
                files.put("src/test/java/org/ikasan/studio/flowtests/JmsXaLifecycleTest.java", resource("jms-xa-lifecycle.java.txt"));
                // ActiveMQ's ordinary ConnectionFactory does not enlist sends in the JTA transaction.
                files.compute("src/test/resources/module-test.properties", (key, value) -> value
                        + "\njms.transportprobe.transport.input.connection.factory.name=XAConnectionFactory\n"
                        + "jms.transportprobe.transport.output.connection.factory.name=XAConnectionFactory\n");
            }
            String converter = "src/main/java/org/example/transport/transport/TransportConverter.java";
            String source = files.get(converter).replace(" value) {", " value) { injectCommitFailure(value);");
            source = source.substring(0, source.lastIndexOf('}')) + resource("probe.java.txt") + "}\n";
            files.put(converter, source);
            String test = "src/test/java/org/ikasan/studio/flowtests/TransportTest.java";
            String testSource = files.get(test);
            files.put("src/test/java/org/ikasan/studio/flowtests/RollbackDiagnostics.java", resource("diagnostics.java.txt"));
            String directory = scenario.equals("sftp-output")
                    ? "localSftpDirectory(context, \"Transport\", \"Output\")" : "localFtpDirectory(context)";
            files.put(test, testSource.substring(0, testSource.indexOf("    @Test"))
                    + resource(scenario.equals("jms") ? "jms.java.txt" : "file.java.txt")
                    .replace("@DIRECTORY@", directory));
            for (var file : files.entrySet()) {
                Path target = root.resolve(scenario).resolve(file.getKey());
                Files.createDirectories(target.getParent()); Files.writeString(target, file.getValue());
            }
        }
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>org.ikasan.studio.probe</groupId><artifactId>rollback</artifactId>"
                + "<version>1</version><packaging>pom</packaging><modules>"
                + scenarios.stream().map(s -> "<module>" + s + "</module>").reduce("", String::concat)
                + "</modules></project>");
    }

    private String resource(String name) throws Exception {
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/rollback/" + name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
