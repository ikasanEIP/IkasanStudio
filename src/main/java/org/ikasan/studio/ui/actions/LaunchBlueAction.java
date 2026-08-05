package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
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
         BrowserUtil.browse("http://localhost:" + (module.getPort() != null ? module.getPort() : "8080") + "/" + module.getIdentity().toLowerCase());
         DesignerCanvas.markConsoleOpened(project);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.BlueConsoleCannotBeOpenedUnlessAModuleIsDefined"));
      }
   }
}
