package org.ikasan.studio.core.migration;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders the actual target artifacts before any project state is changed. */
public final class MigrationArtifacts {
    public static final String MODEL = "generated/src/main/model/model.json";
    private MigrationArtifacts() { }

    public static Map<String, String> render(ModelMigration.Plan plan, String rootPom) throws Exception {
        if (!plan.canApply()) throw new IllegalStateException("Resolve migration blockers before rendering.");
        Module module = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "migration preview", false);
        Map<String, String> files = new LinkedHashMap<>();
        files.put(MODEL, plan.targetJson());
        IkasanPomModel pom = new IkasanPomModel(new MavenXpp3Reader().read(new StringReader(rootPom)));
        MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(plan.targetVersion());
        pom.addProperty("version.ikasan", manifest.ikasanVersion());
        pom.addProperty("maven.compiler.source", manifest.javaVersion());
        pom.addProperty("maven.compiler.target", manifest.javaVersion());
        if (pom.getProperty("maven.compiler.release") != null) pom.addProperty("maven.compiler.release", manifest.javaVersion());
        for (MetaPackManifest.BomImport bom : manifest.dependencyManagement()) pom.addOrUpdateBomImport(bom.groupId(), bom.artifactId(), bom.version());
        for (Dependency dependency : module.getAllUniqueSortedJarDependencies()) {
            Dependency copy = dependency.clone();
            if ("org.ikasan".equals(copy.getGroupId())) copy.setVersion(null);
            pom.checkIfDependancyAlreadyExists(copy);
        }
        files.put("pom.xml", pom.getModelAsString());
        java(files, Generator.STUDIO_BOOT_PACKAGE, "Application", ApplicationTemplate.create(module));
        java(files, Generator.STUDIO_BOOT_PACKAGE, "ModuleConfig", ModuleConfigTemplate.create(module));
        java(files, Generator.STUDIO_BOOT_PACKAGE, "StudioInjectController", StudioInjectControllerTemplate.create(module));
        files.put("generated/h2/pom.xml", H2StartStopTemplate.create(module.getMetaVersion()));
        files.put("generated/src/main/resources/application.properties", PropertiesTemplate.create(module));
        for (var flow : module.getFlows()) {
            String pkg = Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
            java(files, pkg, FlowsComponentFactoryTemplate.COMPONENT_FACTORY_CLASS_NAME + flow.getJavaClassName(), FlowsComponentFactoryTemplate.create(pkg, module, flow));
            java(files, pkg, flow.getJavaClassName(), FlowTemplate.create(pkg, module, flow));
            for (var component : flow.getFlowElementsNoExternalEndPoints()) {
                for (var property : component.getUserSuppliedClassProperties()) {
                    if (property.getMeta().isProtectFromOverwrite() || property.getMeta().isNoStubRequired()) continue;
                    String userPkg = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
                    String clazz = StudioBuildUtils.toJavaClassName(property.getValueString());
                    java(files, userPkg, clazz, FlowsUserImplementedClassPropertyTemplate.create(module.getMetaVersion(),
                            property, userPkg, clazz, GeneratorUtils.getUniquePrefix(module, flow, component)));
                }
            }
        }
        files.put("generated/IKASAN_STUDIO.md", AiProjectContractGenerator.studioGuide(module.getMetaVersion()));
        files.put("generated/src/main/model/model.schema.json", AiProjectContractGenerator.modelSchema());
        files.put("generated/src/main/model/component-catalogue.json", AiProjectContractGenerator.componentCatalogue(module.getMetaVersion()));
        if (files.values().stream().anyMatch(value -> value == null || value.isBlank())) throw new IllegalStateException("A migration artifact is empty.");
        return files;
    }

    private static void java(Map<String, String> files, String pkg, String name, String content) {
        String path = "generated/src/main/java/" + pkg.replace('.', '/') + "/" + name + ".java";
        if (files.putIfAbsent(path, content) != null) throw new IllegalStateException("Duplicate generated path: " + path);
    }
}
