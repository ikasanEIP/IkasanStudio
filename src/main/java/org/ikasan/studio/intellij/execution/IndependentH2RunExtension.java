package org.ikasan.studio.intellij.execution;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import org.ikasan.studio.ui.StudioBundle;

/** H2 belongs to the project's child JVM, never the IDE's plugin classloader. */
public final class IndependentH2RunExtension extends RunConfigurationExtension {
    @Override public boolean isApplicableFor(RunConfigurationBase<?> configuration) {
        return IndependentH2RunConfigurationService.isOurs(configuration);
    }
    @Override public <T extends RunConfigurationBase<?>> void updateJavaParameters(
            T configuration, JavaParameters parameters, RunnerSettings runnerSettings) throws ExecutionException {
        if (!isApplicableFor(configuration)) return;
        long jars = parameters.getClassPath().getPathList().stream().distinct().filter(path -> {
            var filename = java.nio.file.Path.of(path).getFileName();
            if (filename == null) return false;
            String name = filename.toString();
            return name.equals("h2.jar") || name.startsWith("h2-") && name.endsWith(".jar")
                    && name.length() > 3 && Character.isDigit(name.charAt(3));
        }).count();
        if (jars != 1) throw new ExecutionException(StudioBundle.message(jars == 0
                ? "h2.independent.missingDependency" : "h2.independent.multipleDependencies"));
    }
    @Override public void attachToProcess(RunConfigurationBase<?> configuration, ProcessHandler handler, RunnerSettings runnerSettings) {
        if (isApplicableFor(configuration)) configuration.getProject().getService(IndependentH2RunConfigurationService.class).attach(handler);
    }
}
