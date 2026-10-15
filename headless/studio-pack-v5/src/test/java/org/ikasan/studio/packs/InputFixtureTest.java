package org.ikasan.studio.packs;

import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class InputFixtureTest {
    @TempDir Path temporary;
    @Test void exportsScheduledAndLocalFileInputs() throws Exception {
        Path root = Path.of(System.getProperty("studio.v5.inputs", temporary.toString()));
        for (String scenario : List.of("scheduled", "local-file")) {
            boolean scheduled = scenario.equals("scheduled");
            var consumer = DevelopmentFixtureTest.element(scheduled ? "Scheduled Consumer" : "Local File Consumer", "Input");
            if (!scheduled) consumer.setPropertyValue("filenames", "/tmp/unused-v5-fixture/.*\\.txt");
            var flow = Flow.flowBuilder().metapackVersion(DevelopmentFixtureTest.PACK).name("Inputs").consumer(consumer).build();
            var debug = DevelopmentFixtureTest.element("Debug Transition", "Debug");
            debug.setPropertyValue("userImplementedClassName", "DebugStep");
            flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(List.of(
                    debug,
                    DevelopmentFixtureTest.element("Dev Null Producer", "Output")))).build());
            var module = Module.moduleBuilder().version(DevelopmentFixtureTest.PACK).name("InputProbe")
                    .applicationPackageName("org.example.inputs").port("8080").h2PortNumber("8092")
                    .h2WebPortNumber("8093").flows(List.of(flow)).build();
            module.setPropertyValue("flowStartupType", "MANUAL");
            var files = DevelopmentFixtureTest.render(module);
            files.put("pom.xml", files.get("pom.xml").replace("<artifactId>ikasan5-review</artifactId>", "<artifactId>input-" + scenario + "</artifactId>"));
            DevelopmentFixtureTest.putJava(files, DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE,
                    DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME, DebugTransitionComponentTemplate.create(module));
            DevelopmentFixtureTest.putJava(files, DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE,
                    DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME, DeepCopyUtilTemplate.create(module));
            String userPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
            DevelopmentFixtureTest.putJava(files, userPackage, "DebugStep", FlowsUserImplementedComponentTemplate.create(userPackage, module, flow, debug));
            files.keySet().removeIf(p -> p.startsWith("src/test/java/org/ikasan/studio/flowtests/") && !p.contains("/support/"));
            files.put("src/test/java/org/ikasan/studio/flowtests/InputTest.java", resource(scenario + ".java.txt"));
            for (String value : List.of("first", "later", "third")) files.put("src/test/resources/input/" + value + ".txt", value);
            for (var file : files.entrySet()) {
                Path target = root.resolve(scenario).resolve(file.getKey());
                Files.createDirectories(target.getParent()); Files.writeString(target, file.getValue());
            }
        }
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion><groupId>org.ikasan.studio.probe</groupId>"
                + "<artifactId>inputs</artifactId><version>1</version><packaging>pom</packaging><modules>"
                + "<module>scheduled</module><module>local-file</module></modules></project>");
    }
    private String resource(String name) throws Exception {
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/processing/" + name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
