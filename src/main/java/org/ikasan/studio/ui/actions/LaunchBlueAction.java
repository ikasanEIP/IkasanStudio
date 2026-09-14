package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.core.StudioBuildUtils;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchBlueAction implements ActionListener {
   private final Project project;

   public LaunchBlueAction(Project project) {
   this.project = project;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {

      UiContext uiContext = project.getService(UiContext.class);
      Module module = uiContext.getIkasanModule();
      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.SentRequestToYourBrowserToOpenTheBlueConsole"));
         BrowserUtil.browse(consoleUrl(module));
         DesignerCanvas.markConsoleOpened(project);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.BlueConsoleCannotBeOpenedUnlessAModuleIsDefined"));
      }
   }
   /** Uses the same context-path naming rule as generated application.properties and runtime controls. */
   static String consoleUrl(Module module) {
      return "http://localhost:" + (module.getPort() != null ? module.getPort() : "8080")
              + "/" + StudioBuildUtils.toUrlString(module.getIdentity());
   }
}
