package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.intellij.project.StudioProjectFiles;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget;
import org.ikasan.studio.core.generator.HarnessOverridesTemplate;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.ui.StudioBundle;
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
            Project project = configuration.getProject();
            project.getService(HarnessOverrideService.class); // Initialize process snapshot tracking before launch.
            if (StudioProjectFiles.getApplicationBasePath(project) == null) throw new IllegalStateException("Open a project directory before launching harness overrides.");
            Path root = Path.of(StudioProjectFiles.getApplicationBasePath(project));
            Path inventory = root.resolve(HarnessOverridesTemplate.PATH);
            List<LaunchTarget> targets = Files.isRegularFile(inventory) ?
                    HarnessOverridesTemplate.launchTargets(Files.readString(inventory)) :
                    List.of();
            Set<String> selected = HarnessOverrideService.explicitSelection(((ApplicationConfiguration) configuration).getVMParameters());
            if (selected == null) {
                selected = targets.stream().filter(t -> "JMS".equals(t.kind()) ||
                        "SFTP".equals(t.kind()) && project.getService(TestSftpServerService.class).isLocal())
                        .map(LaunchTarget::id)
                        .collect(Collectors.toSet());
            }
            final Set<String> launchSelection = selected;
            Set<String> available = targets.stream().map(LaunchTarget::id)
                    .collect(Collectors.toSet());
            if (!available.containsAll(selected))
                throw new IllegalStateException(StudioBundle.message("harnessOverrides.stale"));
            if (!selected.isEmpty()) verifyHarnessSupport(Path.of(StudioProjectFiles.getApplicationBasePath(project)), targets, launchSelection);
            boolean sftpSelected = targets.stream().anyMatch(t -> launchSelection.contains(t.id()) && "SFTP".equals(t.kind()));
            boolean ftpSelected = targets.stream().anyMatch(t -> launchSelection.contains(t.id()) && "FTP".equals(t.kind()));
            if (sftpSelected) {
                TestSftpServerService sftp = project.getService(TestSftpServerService.class);
                if (!sftp.isRunning()) throw new IllegalStateException(StudioBundle.message("harnessOverrides.startSftp"));
                sftp.launchProperties().forEach((key, value) -> parameters.getVMParametersList().addProperty(key, value));
            }
            if (ftpSelected) {
                TestFtpServerService ftp = project.getService(TestFtpServerService.class);
                ftp.launchProperties().forEach((key, value) -> parameters.getVMParametersList().addProperty(key, value));
            }
            parameters.getVMParametersList().addProperty("studio.harness.components", String.join(",", selected));
            parameters.getVMParametersList().addProperty("studio.harness.session", UUID.randomUUID().toString());
            configuration.putUserData(HarnessOverrideService.LAUNCH_SELECTION, Set.copyOf(selected));
            configuration.putUserData(HarnessOverrideService.LAUNCH_LABELS, targets.stream()
                    .filter(t -> launchSelection.contains(t.id())).collect(Collectors.toUnmodifiableMap(
                            LaunchTarget::id,
                            LaunchTarget::label)));
        } catch (IllegalStateException | IOException failure) { throw new ExecutionException(failure.getMessage()); }
    }
    @Override public void attachToProcess(@NotNull RunConfigurationBase<?> configuration,
            @NotNull ProcessHandler handler, RunnerSettings runnerSettings) {
        if (isApplicableFor(configuration))
            configuration.getProject().getService(ModuleStartupDiagnostics.class).attach(handler);
    }

    static void verifyHarnessSupport(Path root,
            List<LaunchTarget> targets,
            Set<String> selected) throws IOException {
        Path settings = root.resolve(HarnessOverridesTemplate.PATH);
        if (!Files.isRegularFile(settings) || !Files.isRegularFile(
                root.resolve(HarnessOverridesTemplate.JAVA_PATH)))
            throw new IOException("Regenerate code before using harness overrides.");
        for (LaunchTarget target : targets) {
            if (!selected.contains(target.id())) continue;
            Path source = root.resolve("generated/src/main/java/").resolve(target.factory());
            if (!Files.isRegularFile(source) ||
                    !Files.readString(source).contains("\"" + target.id() + "\""))
                throw new IOException("Regenerate code before using harness overrides: " + target.label());
        }
    }
}
