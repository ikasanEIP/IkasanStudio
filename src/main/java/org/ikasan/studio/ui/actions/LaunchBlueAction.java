package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchBlueAction implements ActionListener {
   private final String projectKey;

   public LaunchBlueAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {


      Module module = UiContext.getIkasanModule(projectKey);
      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "Sent request to your browser to launch the blue console.");
         BrowserUtil.browse("http:localhost:" + (module.getPort() != null ? module.getPort() : "8080") + "/" + module.getIdentity().toLowerCase());
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "Blue console can't be launched unless a module is defined.");
      }
   }
}
