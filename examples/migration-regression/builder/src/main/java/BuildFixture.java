import java.nio.file.*;
import java.util.*;
import org.ikasan.studio.core.ai.ModelProposal;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.*;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.persistence.json.StudioJson;

public class BuildFixture {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        String version = args.length > 1 ? args[1] : "V3.3.9";
        var json = StudioJson.newObjectMapper();
        String source = Files.readString(root.resolve("baseline.json"));
        if (Files.exists(root.resolve("operations.json"))) {
            Map<String,Object> snapshot = json.readValue(source, Map.class);
            for (var batch : json.readTree(Files.readString(root.resolve("operations.json")))) {
                source = ComponentIO.toJson(ModelProposal.prepare(snapshot, batch).draft());
                snapshot = json.readValue(source, Map.class);
            }
        }
        var plan = version.equals("V3.3.9")
                ? new ModelMigration.Plan(version, version, source, source, List.of())
                : ModelMigration.analyse(source, version);
        if (!plan.canApply()) throw new IllegalStateException(plan.report());
        Path output = root.resolve("build").resolve(args.length > 2 ? args[2] : version);
        if (Files.exists(output)) throw new IllegalStateException("Output already exists; choose a fresh fixture workspace: " + output);
        try (var files = Files.walk(root.resolve("project"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path target = output.resolve(root.resolve("project").relativize(file));
                Files.createDirectories(target.getParent()); Files.copy(file, target);
            }
        }
        var rendered = MigrationArtifacts.render(plan, Files.readString(output.resolve("pom.xml")));
        for (var entry : rendered.entrySet()) {
            Path target = output.resolve(entry.getKey()); Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue());
        }
        var module = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "fixture", false);
        for (var flow : module.getFlows()) for (var component : flow.getFlowElementsNoExternalEndPoints()) {
            String name = component.getPropertyValueAsString("userImplementedClassName");
            if (name.isBlank()) continue;
            String pkg = org.ikasan.studio.core.generator.GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
            Path target = output.resolve("user/src/main/java/" + pkg.replace('.', '/') + "/" + name + ".java");
            if (!Files.exists(target)) {
                if (component.getPropertyValueAsString("conversionRecipeId").isBlank())
                    throw new IllegalStateException("Missing fixture implementation: " + target);
                Files.createDirectories(target.getParent());
                Files.writeString(target, org.ikasan.studio.core.generator.FlowsUserImplementedComponentTemplate.create(pkg, module, flow, component));
            }
        }
        Files.copy(root.resolve("fixture.py"), output.resolve("fixture.py"));
        Files.copy(root.resolve("coverage.json"), output.resolve("coverage.json"));
        Files.writeString(output.resolve("AGENTS.md"), AiProjectContractGenerator.agentsGuide());
        Files.writeString(output.resolve("migration-plan.txt"), plan.report());
        Files.writeString(root.resolve("model.json"), source);
        System.out.println("Fixture generated: " + output);
    }
}
