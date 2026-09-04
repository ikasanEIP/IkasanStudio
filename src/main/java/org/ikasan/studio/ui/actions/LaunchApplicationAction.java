package org.ikasan.studio.ui.actions;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.intellij.execution.IkasanRunConfigurationService;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class LaunchApplicationAction implements ActionListener {
   private static final Logger LOG = LoggerFactory.getLogger(LaunchApplicationAction.class);
   private final Project project;
   private final boolean debug;

   public LaunchApplicationAction(Project project) {
      this(project, false);
   }

   public LaunchApplicationAction(Project project, boolean debug) {
      this.project = project;
      this.debug = debug;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      ComponentPropertiesPanel propertiesPanel = project.getService(UiContext.class).getPropertiesPanel();
      CompletableFuture<Void> generation = propertiesPanel != null
              ? propertiesPanel.preparePendingChangesForLaunch()
              : CompletableFuture.completedFuture(null);
      if (generation == null) {
         return;
      }
      continueAfterGeneration(generation, project::isDisposed, this::launch, failure -> {
         LOG.warn("STUDIO: Could not generate code before launching the module", failure);
         String detail = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
         StudioUIUtils.displayIdeaWarnMessage(project,
                 StudioBundle.message("message.CouldNotGenerateCodeBeforeLaunch", detail));
      }, runnable -> ApplicationManager.getApplication().invokeLater(runnable));
   }

   static void continueAfterGeneration(CompletableFuture<Void> generation, BooleanSupplier disposed,
                                       Runnable launch, Consumer<Throwable> failureHandler,
                                       Consumer<Runnable> uiScheduler) {
      generation.whenComplete((ignored, failure) -> uiScheduler.accept(() -> {
         if (disposed.getAsBoolean()) return;
         if (failure != null) {
            failureHandler.accept(failure);
         } else {
            launch.run();
         }
      }));
   }

   private void launch() {
      String applicationRelativePath = "generated/src/main/java/org/ikasan/studio/boot/Application.java";
      VirtualFile applicationFile = StudioProjectFiles.getVirtualFile(project, applicationRelativePath);
      if (applicationFile == null) {
         StudioUIUtils.displayIdeaWarnMessage(project,
                 StudioBundle.message("message.ApplicationJavaIsNotAvailableYetRegenerateTheModule"));
         LOG.warn("STUDIO: Could not find " + applicationRelativePath + " in " + project);
         return;
      }

      Executor executor = debug
              ? DefaultDebugExecutor.getDebugExecutorInstance()
              : DefaultRunExecutor.getRunExecutorInstance();

      project.getService(IkasanRunConfigurationService.class).selectAndRun(applicationFile, executor, launched -> {
         if (launched) {
            DesignerCanvas.markModuleLaunched(project);
         } else {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TheIkasanRunConfigurationCouldNotBeCreated"));
         }
      });
   }
}
