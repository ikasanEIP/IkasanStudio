package org.ikasan.studio.intellij.testing;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.ui.StudioBundle;

import java.nio.file.Path;

/** Runs the existing flow-test class using the IDE's normal configuration producers and executor. */
public final class RunFlowTestAction {
    private static final Logger LOG = Logger.getInstance(RunFlowTestAction.class);
    private RunFlowTestAction() { }

    public static void run(Project project, Flow flow) {
        if (project.isDisposed() || project.getBasePath() == null) return;
        Path path = Path.of(project.getBasePath()).resolve(FlowTestScaffold.testPath(flow));
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (project.isDisposed()) return;
            try {
                VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
                if (file == null) {
                    showMessage(project, StudioBundle.message("flowTest.navigation.missing", path.toString()));
                    return;
                }
                ReadAction.nonBlocking(() -> {
                            try { return resolve(project, file); }
                            catch (ProcessCanceledException cancelled) { throw cancelled; }
                            catch (Exception failure) {
                                LOG.warn("Could not resolve flow test run configuration: " + path, failure);
                                return null;
                            }
                        })
                        .inSmartMode(project)
                        .expireWith(project.getService(StudioProjectInitialisationService.class))
                        .finishOnUiThread(ModalityState.defaultModalityState(), target -> {
                            if (project.isDisposed()) return;
                            if (target == null) {
                                showMessage(project, StudioBundle.message("flowTest.run.unavailable"));
                                return;
                            }
                            try {
                                target.start(() -> {
                                    if (project.isDisposed()) return;
                                    RunManager manager = RunManager.getInstance(project);
                                    if (!manager.hasSettings(target.settings())) manager.setTemporaryConfiguration(target.settings());
                                    manager.setSelectedConfiguration(target.settings());
                                    ProgramRunnerUtil.executeConfiguration(target.settings(), DefaultRunExecutor.getRunExecutorInstance());
                                });
                            } catch (ProcessCanceledException cancelled) { throw cancelled; }
                            catch (Exception failure) { reportFailure(project, failure); }
                        })
                        .submit(AppExecutorUtil.getAppExecutorService());
            } catch (ProcessCanceledException cancelled) { throw cancelled; }
            catch (Exception failure) { reportFailure(project, failure); }
        });
    }

    record Target(ConfigurationContext context, RunnerAndConfigurationSettings settings, ConfigurationFromContext produced) {
        void start(Runnable launch) {
            if (produced == null) launch.run();
            else produced.onFirstRun(context, launch);
        }
    }

    /** PSI/configuration discovery stays off the EDT and waits for indexing in the caller. */
    static Target resolve(Project project, VirtualFile file) {
        var psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof PsiJavaFile javaFile)) return null;
        String name = file.getNameWithoutExtension();
        for (var psiClass : javaFile.getClasses()) {
            if (!name.equals(psiClass.getName())) continue;
            var context = new ConfigurationContext(psiClass);
            if (context.getModule() == null) return null;
            var existing = context.findExisting();
            if (existing != null) return new Target(context, existing, null);
            var configurations = context.getConfigurationsFromContext();
            if (configurations == null || configurations.isEmpty()) return null;
            // The platform returns preferred producers first, just as its normal Run Context action uses them.
            var produced = configurations.get(0);
            var settings = produced.getConfigurationSettings();
            context.setConfiguration(settings);
            return new Target(context, settings, produced);
        }
        return null;
    }

    private static void reportFailure(Project project, Exception failure) {
        LOG.warn("Could not run flow test", failure);
        showMessage(project, StudioBundle.message("flowTest.run.failed"));
    }

    private static void showMessage(Project project, String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) Messages.showInfoMessage(project, message, StudioBundle.message("menu.RunFlowTest"));
        });
    }
}
