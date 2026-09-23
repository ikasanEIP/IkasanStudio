package org.ikasan.studio.core.generator;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders developer-owned tests once; normal generation never owns these files. */
public final class FlowTestScaffold {
    private FlowTestScaffold() { }
    public record Scaffold(String rootPom, String testPath, Map<String, String> files) { }

    public static Scaffold render(Module module, Flow flow, String rootPom, String applicationPom) throws Exception {
        if (flow.getConsumer() == null) throw new IllegalArgumentException("Add a consumer before generating a flow test.");
        Model parent = new MavenXpp3Reader().read(new StringReader(rootPom));
        Model application = new MavenXpp3Reader().read(new StringReader(applicationPom));
        if (!"pom".equals(parent.getPackaging()) || parent.getGroupId() == null || parent.getVersion() == null
                || parent.getProperties().getProperty("version.ikasan") == null) {
            throw new IllegalArgumentException("Expected a Studio parent POM with groupId, version and version.ikasan.");
        }
        String className = flow.getJavaClassName() + "FlowTest";
        if (!className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) throw new IllegalArgumentException("Invalid flow Java class name.");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("className", className);
        values.put("flowName", flow.getIdentity());
        values.put("flowNames", module.getFlows().stream().map(Flow::getIdentity).toList());
        values.put("consumerName", flow.getConsumer().getIdentity());
        values.put("localFile", flow.getConsumer().getComponentMeta().isLocalFileConsumer());
        var filenameProperty = flow.getConsumer().getProperty("filenames");
        boolean isolatedFiles = flow.getConsumer().getComponentMeta().isLocalFileConsumer()
                && filenameProperty != null && !filenameProperty.valueNotSet()
                && filenameProperty.getMeta().getPropertyConfigFileLabel() != null;
        values.put("isolatedFiles", isolatedFiles);
        values.put("filenameKey", isolatedFiles ? org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(
                module, flow, flow.getConsumer(), filenameProperty.getMeta().getPropertyConfigFileLabel()) : "");
        Map<String, String> methods = Map.of("Converter", "converter", "Translator", "translator", "Broker", "broker", "Producer", "producer");
        java.util.List<Map<String, String>> executionPath = new java.util.ArrayList<>();
        boolean automaticPath = flow.getFlowRoute().getChildRoutes().isEmpty() && flow.getExceptionResolver() == null;
        for (var element : flow.getFlowRoute().getFlowElements()) {
            String type = element.getComponentMeta().getComponentType();
            String method = type == null ? null : methods.get(type.substring(type.lastIndexOf('.') + 1));
            if (method == null) automaticPath = false;
            else executionPath.add(Map.of("method", method, "name", element.getIdentity()));
        }
        automaticPath &= !executionPath.isEmpty() && "producer".equals(executionPath.get(executionPath.size() - 1).get("method"));
        values.put("automaticPath", automaticPath);
        values.put("expectedPath", executionPath);
        values.put("componentNames", flow.getFlowElementsNoExternalEndPoints().stream().map(FlowElement::getIdentity).toList());
        values.put("scheduled", flow.getConsumer().getComponentMeta().isTimeEventConsumer());
        values.put("producers", flow.getFlowElementsNoExternalEndPoints().stream()
                .filter(e -> e.getComponentMeta().isProducer()).map(FlowElement::getIdentity).toList());
        values.put("groupId", parent.getGroupId());
        values.put("artifactId", parent.getArtifactId());
        values.put("version", parent.getVersion());
        values.put("applicationGroupId", application.getGroupId() == null ? parent.getGroupId() : application.getGroupId());
        values.put("applicationArtifactId", application.getArtifactId());
        values.put("applicationVersion", application.getVersion() == null ? parent.getVersion() : application.getVersion());
        String path = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/" + className + ".java";
        Map<String, String> files = new LinkedHashMap<>();
        files.put(path, FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "flowTestTemplate_en.ftl", values));
        files.put("user-flow-tests/pom.xml", FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "flowTestPomTemplate_en.ftl", values));
        files.put("user-flow-tests/README.md", """
                # Flow tests

                These are developer-owned scaffolds using the actual generated application and IkasanFlowTestRule.
                They deliberately FAIL before starting services until you complete the test scenario.
                Follow TODO 1–5 in the Java test: isolate settings, supply two input batches, select output and expected results,
                review component-path expectations, then enable and run. Set CONFIGURED only after completing the first four tasks.
                Consult ../LOCAL_TEST_ENVIRONMENT.md for test services. Never point tests at production systems.
                Test-only MANUAL startup does not change the model or normal application startup settings.

                Run from the project root: `mvn -pl user-flow-tests -am test`.
                For one test: `mvn -pl user-flow-tests -am -Dtest=YourFlowTest -Dsurefire.failIfNoSpecifiedTests=false test`.
                Assert actual delivery at external receivers as well as observed flow payloads. For routers, assert
                every expected branch and absence of unwanted deliveries. For exclusions, assert stored exclusions
                and delivery of subsequent valid input. Component invocation alone proves none of these.
                Scheduled tests explicitly trigger the consumer; separately verify real scheduling and pacing.
                Isolated test flows stop during teardown; production/demo flows must stay running between batches.

                Migration updates the inherited version.ikasan dependency. Studio never regenerates these tests.
                Run before and after migration; review framework API changes if a future major version needs them.
                Keep assertions unchanged when comparing behaviour. Add application-specific timeouts and reliable
                cleanup for any local services you start. A stuck JVM is a test/shutdown failure, not a passing run.
                """);
        String updated = rootPom;
        if (!parent.getModules().contains("user-flow-tests")) {
            parent.addModule("user-flow-tests");
            StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write(writer, parent);
            updated = writer.toString();
        }
        return new Scaffold(updated, path, java.util.Collections.unmodifiableMap(files));
    }
}
