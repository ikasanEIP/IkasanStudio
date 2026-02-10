package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

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
         StudioUIUtils.displayIdeaInfoMessage(project, "Sent request to your browser to launch the blue console.");
         BrowserUtil.browse("http:localhost:" + (module.getPort() != null ? module.getPort() : "8080") + "/" + module.getIdentity().toLowerCase());
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "Blue console can't be launched unless a module is defined.");
      }
   }
}
