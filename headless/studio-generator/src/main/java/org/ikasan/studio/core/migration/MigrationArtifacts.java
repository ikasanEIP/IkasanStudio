package org.ikasan.studio.core.migration;

import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.io.ComponentIO;
import java.util.Map;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.project.DistributionScaffolding.FileChange;
import org.ikasan.studio.core.project.DistributionScaffolding;
import org.ikasan.studio.core.project.StudioProjectLayout;

/** Renders the actual target artifacts before any project state is changed. */
public final class MigrationArtifacts {
    public static final String MODEL = ProjectArtifactRenderer.MODEL;
    private MigrationArtifacts() { }

    public static Map<String, String> render(Path workspace, ModelMigration.Plan plan, String rootPom) throws Exception {
        Map<String, String> application = render(plan, rootPom);
        if (!StudioProjectLayout.isNested(workspace)) return application;
        Map<String, String> files = new LinkedHashMap<>();
        application.forEach((path, text) -> files.put("jar/" + path, text));
        files.put("jar/generated/IKASAN_STUDIO.md", AiProjectContractGenerator.studioGuide(plan.targetVersion(), true));
        for (FileChange change :
                DistributionScaffolding.prepare(workspace,
                        ComponentLibrary.getMetaPackManifest(plan.targetVersion())))
            files.put(change.path(), change.content());
        return files;
    }

    public static Map<String, String> render(ModelMigration.Plan plan, String rootPom) throws Exception {
        if (!plan.canApply()) throw new IllegalStateException("Resolve migration blockers before rendering.");
        Module target = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "migration preview", false);
        Module source = ComponentIO.validatePersistedModuleJson(plan.sourceJson(), "migration source dependencies", false);
        return ProjectArtifactRenderer.render(source, target, rootPom);
    }
}
