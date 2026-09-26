package org.ikasan.studio.core.generator;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Explicitly generated, frozen verification; ordinary application generation never calls this renderer. */
public final class GeneratedVerification {
    public static final String DIRECTORY = "generated-verification";
    public record Bundle(String rootPom, Map<String, String> files) { }
    private GeneratedVerification() { }

    public static Bundle render(Module module, String modelJson, String parentXml, String applicationXml) throws Exception {
        if (module.getFlows().isEmpty()) throw new IllegalArgumentException("Add a flow before generating verification");
        var reader = new MavenXpp3Reader();
        var parent = reader.read(new StringReader(parentXml));
        var application = reader.read(new StringReader(applicationXml));
        if (!"pom".equals(parent.getPackaging()) || parent.getGroupId() == null || parent.getVersion() == null)
            throw new IllegalArgumentException("Expected a Studio parent Maven POM");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("groupId", parent.getGroupId());
        values.put("artifactId", parent.getArtifactId());
        values.put("version", parent.getVersion());
        values.put("applicationGroupId", application.getGroupId() == null ? parent.getGroupId() : application.getGroupId());
        values.put("applicationArtifactId", application.getArtifactId());
        values.put("applicationVersion", application.getVersion() == null ? parent.getVersion() : application.getVersion());
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(modelJson.getBytes(StandardCharsets.UTF_8)));
        values.put("fingerprint", fingerprint);
        Map<String, String> files = new LinkedHashMap<>();
        String javaRoot = "src/test/java/org/ikasan/studio/verification/";
        for (var flow : module.getFlows()) {
            if (flow.getConsumer() == null) throw new IllegalArgumentException("Add a consumer to " + flow.getIdentity() + " before generating verification");
            values.put("flow", flow);
            List<Map<String, String>> components = new ArrayList<>();
            for (var component : flow.getFlowElementsNoExternalEndPoints()) {
                String contract = component.getComponentMeta().getComponentType();
                if (contract == null || contract.isBlank()) continue;
                String implementation = component.getPropertyValueAsString("userImplementedClassName");
                if (implementation == null) implementation = "";
                if (!implementation.isBlank() && !implementation.contains("."))
                    implementation = GeneratorUtils.getUserImplementedClassesPackageName(module, flow) + "." + implementation;
                String implementationContract = component.getComponentMeta().getVerificationImplementationContract();
                components.add(Map.of("name", component.getIdentity(), "javaName", component.getJavaClassName(),
                        "contract", contract, "implementation", implementation,
                        "factory", Boolean.toString(component.getComponentMeta().isUsesBuilderInFactory()),
                        "implementationContract", implementationContract == null ? contract : implementationContract));
            }
            values.put("components", components);
            String name = flow.getJavaClassName() + "VerificationTest.java";
            if (files.put(javaRoot + name, FreemarkerUtils.generateFromTemplate(module.getMetaVersion(),
                    "generatedVerificationTest_en.ftl", values)) != null)
                throw new IllegalArgumentException("Duplicate verification class name: " + name);
        }
        files.put(javaRoot + "GeneratedVerificationSupport.java", FreemarkerUtils.generateFromTemplate(module.getMetaVersion(),
                "generatedVerificationSupport_en.ftl", values));
        files.put("pom.xml", FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "generatedVerificationPom_en.ftl", values));
        files.put("baseline-model.json", modelJson);
        files.put("baseline.properties", "format=1\nmodel.sha256=" + fingerprint + "\nmeta.pack=" + module.getMetaVersion() + "\n");
        files.put("README.md", """
                # Generated verification baseline

                Studio owns this directory. Generate/refresh explicitly; running tests never regenerates them.
                Commit these files. Refresh archives the previous directory, including temporary developer corrections.
                There are no TODOs or developer enablement flags.

                This first version verifies flow/component factory signatures and declared user implementation interfaces.
                Classes are loaded without initialisation; Spring and external services are not started.
                Runtime delivery, routing decisions, exclusions and custom business behaviour are NOT VERIFIED.
                Each flow reports a skipped runtime test with that reason. A passed structural test is not runtime success.
                Maven Surefire reports passed/failed/skipped checks in target/surefire-reports.

                Run from the project root: mvn -pl generated-verification -am test
                BASELINE_MODEL_SHA256 in GeneratedVerificationSupport identifies the saved model.json at generation.
                It hashes UTF-8 model bytes, not Java sources or compiled classes; baseline.properties stores the same hash.
                Formatting-only model changes also change the hash. A mismatch warns; assertions still run unchanged.
                Before migration run and save target/surefire-reports outside target. Upgrade the application, then run
                these same tests again. Save the after reports separately and compare failures and skipped checks.
                Only after comparison, optionally use Generate/Refresh Verification Tests to establish a new baseline.
                A model-hash warning is expected after migration and is not itself a failed test.
                If an API change prevents the old tests running, retain that failure evidence before adapting tests.
                Interface incompatibilities are failures to review, never silently rewritten expectations.
                Ordinary generation and migration preserve this directory. Business tests remain in user-flow-tests.
                """);
        String root = parentXml;
        if (!parent.getModules().contains(DIRECTORY)) {
            parent.addModule(DIRECTORY);
            StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write(writer, parent);
            root = writer.toString();
        }
        return new Bundle(root, Collections.unmodifiableMap(files));
    }
}
