package org.ikasan.studio.core.migration;

import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.io.ComponentIO;
import java.util.Map;

/** Renders the actual target artifacts before any project state is changed. */
public final class MigrationArtifacts {
    public static final String MODEL = ProjectArtifactRenderer.MODEL;
    private MigrationArtifacts() { }

    public static Map<String, String> render(ModelMigration.Plan plan, String rootPom) throws Exception {
        if (!plan.canApply()) throw new IllegalStateException("Resolve migration blockers before rendering.");
        var target = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "migration preview", false);
        var source = ComponentIO.validatePersistedModuleJson(plan.sourceJson(), "migration source dependencies", false);
        return ProjectArtifactRenderer.render(source, target, rootPom);
    }
}
