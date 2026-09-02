package org.ikasan.studio.ui.actions;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.intellij.execution.IkasanRunConfigurationService;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
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
      if (!confirmLaunchWithUnsavedPropertyChanges()) {
         return;
      }
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

   /**
    * The docked properties panel (Properties tab) is reused across component selections and is only ever
    * written back into the model when "Update Code" is clicked - see ComponentPropertiesPanel#doOKAction().
    * Edits left sitting in its fields are silently ignored by a run/debug launch, so the generated code the
    * module actually runs can be stale relative to what's on screen. Warn rather than launch straight through.
    * @return true if it's OK to proceed with the launch, false if the user chose to cancel.
    */
   private boolean confirmLaunchWithUnsavedPropertyChanges() {
      ComponentPropertiesPanel propertiesPanel = project.getService(UiContext.class).getPropertiesPanel();
      if (propertiesPanel != null && propertiesPanel.dataHasChangedAndOKToProcess()) {
         int answer = Messages.showYesNoDialog(project,
                 StudioBundle.message("message.UnsavedPropertyChangesBeforeLaunch"),
                 StudioBundle.message("dialog.UnsavedPropertyChanges"),
                 Messages.getWarningIcon());
         return answer == Messages.YES;
      }
      return true;
   }
}
