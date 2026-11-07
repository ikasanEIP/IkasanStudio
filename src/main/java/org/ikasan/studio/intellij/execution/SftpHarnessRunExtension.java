package org.ikasan.studio.intellij.execution;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;

import org.jetbrains.annotations.NotNull;

/** Applies the saved per-component selection to Application launches; never affects JUnit or Maven. */
public final class SftpHarnessRunExtension extends RunConfigurationExtension {
    @Override public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return configuration instanceof ApplicationConfiguration app
                && "org.ikasan.studio.boot.Application".equals(app.getMainClassName());
    }
    @Override public <T extends RunConfigurationBase<?>> void updateJavaParameters(
            @NotNull T configuration, @NotNull JavaParameters parameters, RunnerSettings runnerSettings) throws ExecutionException {
        try {
            var project = configuration.getProject();
            project.getService(HarnessOverrideService.class); // Initialize process snapshot tracking before launch.
            if (project.getBasePath() == null) throw new IllegalStateException("Open a project directory before launching harness overrides.");
            var root = java.nio.file.Path.of(project.getBasePath());
            var inventory = root.resolve(org.ikasan.studio.core.generator.HarnessOverridesTemplate.PATH);
            var targets = java.nio.file.Files.isRegularFile(inventory) ?
                    org.ikasan.studio.core.generator.HarnessOverridesTemplate.launchTargets(java.nio.file.Files.readString(inventory)) :
                    java.util.List.<org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget>of();
            var selected = HarnessOverrideService.explicitSelection(((ApplicationConfiguration) configuration).getVMParameters());
            if (selected == null) {
                selected = targets.stream().filter(t -> "JMS".equals(t.kind()) ||
                        "SFTP".equals(t.kind()) && project.getService(TestSftpServerService.class).isLocal())
                        .map(org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget::id)
                        .collect(java.util.stream.Collectors.toSet());
            }
            final var launchSelection = selected;
            var available = targets.stream().map(org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget::id)
                    .collect(java.util.stream.Collectors.toSet());
            if (!available.containsAll(selected))
                throw new IllegalStateException(org.ikasan.studio.ui.StudioBundle.message("harnessOverrides.stale"));
            if (!selected.isEmpty()) verifyHarnessSupport(java.nio.file.Path.of(project.getBasePath()), targets, launchSelection);
            boolean sftpSelected = targets.stream().anyMatch(t -> launchSelection.contains(t.id()) && "SFTP".equals(t.kind()));
            boolean ftpSelected = targets.stream().anyMatch(t -> launchSelection.contains(t.id()) && "FTP".equals(t.kind()));
            if (sftpSelected) {
                var sftp = project.getService(TestSftpServerService.class);
                if (!sftp.isRunning()) throw new IllegalStateException(org.ikasan.studio.ui.StudioBundle.message("harnessOverrides.startSftp"));
                sftp.launchProperties().forEach((key, value) -> parameters.getVMParametersList().addProperty(key, value));
            }
            if (ftpSelected) {
                var ftp = project.getService(org.ikasan.studio.intellij.runtime.TestFtpServerService.class);
                var expected = org.ikasan.studio.intellij.runtime.HarnessEndpointSettings.localFtp();
                if (!ftp.isRunningAt(expected))
                    throw new IllegalStateException(org.ikasan.studio.ui.StudioBundle.message("harnessOverrides.startFtp"));
                parameters.getVMParametersList().addProperty("studio.harness.ftp.home", ftp.getRootDirectory().toString());
            }
            parameters.getVMParametersList().addProperty("studio.harness.components", String.join(",", selected));
            parameters.getVMParametersList().addProperty("studio.harness.session", java.util.UUID.randomUUID().toString());
            configuration.putUserData(HarnessOverrideService.LAUNCH_SELECTION, java.util.Set.copyOf(selected));
            configuration.putUserData(HarnessOverrideService.LAUNCH_LABELS, targets.stream()
                    .filter(t -> launchSelection.contains(t.id())).collect(java.util.stream.Collectors.toUnmodifiableMap(
                            org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget::id,
                            org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget::label)));
        } catch (IllegalStateException | java.io.IOException failure) { throw new ExecutionException(failure.getMessage()); }
    }
    @Override public void attachToProcess(@NotNull RunConfigurationBase<?> configuration,
            com.intellij.execution.process.@NotNull ProcessHandler handler, RunnerSettings runnerSettings) {
        if (isApplicableFor(configuration))
            configuration.getProject().getService(ModuleStartupDiagnostics.class).attach(handler);
    }

    static void verifyHarnessSupport(java.nio.file.Path root,
            java.util.List<org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget> targets,
            java.util.Set<String> selected) throws java.io.IOException {
        var settings = root.resolve(org.ikasan.studio.core.generator.HarnessOverridesTemplate.PATH);
        if (!java.nio.file.Files.isRegularFile(settings) || !java.nio.file.Files.isRegularFile(
                root.resolve(org.ikasan.studio.core.generator.HarnessOverridesTemplate.JAVA_PATH)))
            throw new java.io.IOException("Regenerate code before using harness overrides.");
        for (var target : targets) {
            if (!selected.contains(target.id())) continue;
            var source = root.resolve("generated/src/main/java/").resolve(target.factory());
            if (!java.nio.file.Files.isRegularFile(source) ||
                    !java.nio.file.Files.readString(source).contains("\"" + target.id() + "\""))
                throw new java.io.IOException("Regenerate code before using harness overrides: " + target.label());
        }
    }
}
