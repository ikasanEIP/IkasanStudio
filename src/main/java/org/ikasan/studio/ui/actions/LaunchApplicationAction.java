package org.ikasan.studio.ui.actions;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.intellij.IkasanRunConfigurationService;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
      String applicationRelativePath = "generated/src/main/java/org/ikasan/studio/boot/Application.java";
      VirtualFile applicationFile = StudioPsiUtils.getVirtualFile(project, applicationRelativePath);
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
