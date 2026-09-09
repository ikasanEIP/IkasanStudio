package org.ikasan.studio.intellij.migration;

import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenWorkspaceSettingsComponent;

/** IDE settings change only after the reviewed migration files have been committed. Also reused by model.json import. */
public final class MigrationJdk {
    private MigrationJdk() { }

    public static boolean matches(Sdk sdk, int required) {
        if (sdk == null || !(sdk.getSdkType() instanceof JavaSdk)) return false;
        String versionString = sdk.getVersionString();
        var version = versionString == null ? null : com.intellij.util.lang.JavaVersion.tryParse(versionString);
        return version != null && version.feature == required;
    }

    static boolean isStudioApplication(String mainClass) {
        return "org.ikasan.studio.boot.Application".equals(mainClass);
    }

    public static void apply(Project project, Sdk sdk, int required) {
        if (!matches(sdk, required)) throw new IllegalStateException(StudioBundle.message("message.SelectAnInstalledJdk", required));
        WriteAction.run(() -> {
            ProjectRootManager.getInstance(project).setProjectSdk(sdk);
            // Explicit module SDKs otherwise survive a change of project SDK.
            var maven = MavenProjectsManager.getInstance(project);
            for (var module : com.intellij.openapi.module.ModuleManager.getInstance(project).getModules()) {
                if (maven.isMavenizedModule(module)) ModuleRootModificationUtil.setSdkInherited(module);
            }
        });
        MavenRunner.getInstance(project).getSettings().setJreName(sdk.getName());
        MavenWorkspaceSettingsComponent.getInstance(project).getSettings()
                .getImportingSettings().setJdkForImporter(sdk.getName());
        for (var settings : RunManager.getInstance(project).getAllSettings()) {
            if (settings.getConfiguration() instanceof ApplicationConfiguration application
                    && isStudioApplication(application.getMainClassName())) {
                application.setAlternativeJrePath(sdk.getName());
                application.setAlternativeJrePathEnabled(true);
            }
        }
    }
}
