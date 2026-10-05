package org.ikasan.studio.intellij.execution;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;

/** Adds transient overrides to Studio module launches, never to saved configurations or JUnit/Maven runs. */
public final class SftpHarnessRunExtension extends RunConfigurationExtension {
    @Override public boolean isApplicableFor(RunConfigurationBase<?> configuration) {
        return configuration instanceof ApplicationConfiguration app
                && "org.ikasan.studio.boot.Application".equals(app.getMainClassName());
    }
    @Override public <T extends RunConfigurationBase<?>> void updateJavaParameters(
            T configuration, JavaParameters parameters, RunnerSettings runnerSettings) throws ExecutionException {
        try {
            var service = configuration.getProject().getService(TestSftpServerService.class);
            if (service.isLocal() && !verifyGeneratedSupport(java.nio.file.Path.of(configuration.getProject().getBasePath()))) return;
            service.launchProperties()
                    .forEach((key, value) -> parameters.getVMParametersList().addProperty(key, value));
        } catch (IllegalStateException | java.io.IOException failure) { throw new ExecutionException(failure.getMessage()); }
    }
    static boolean verifyGeneratedSupport(java.nio.file.Path project) throws java.io.IOException {
        java.nio.file.Path source = project.resolve("generated/src/main/java");
        if (!java.nio.file.Files.isDirectory(source)) throw new java.io.IOException("Regenerate code before using the local SFTP harness.");
        boolean found = false;
        try (var paths = java.nio.file.Files.walk(source)) {
            for (var path : paths.filter(p -> p.getFileName().toString().startsWith("ComponentFactory") && p.toString().endsWith(".java")).toList()) {
                String text = java.nio.file.Files.readString(path);
                if (text.contains(".sftpConsumer()") || text.contains(".sftpProducer()")) found = true;
                if ((text.contains(".sftpConsumer()") || text.contains(".sftpProducer()"))
                        && !text.contains("return studioLocalSftp("))
                    throw new java.io.IOException("Regenerate code before using the local SFTP harness: " + path.getFileName());
            }
        }
        return found;
    }
}
