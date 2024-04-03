package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchH2Action implements ActionListener {
   private final String projectKey;
   public LaunchH2Action(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);
      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "Sent request to your browser to launch the H2 console.");
         BrowserUtil.browse("http:localhost:" + module.getH2WebPortNumber() + "/login.do");
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "H2 console can't be launched unless a module is defined.");

      }
   }
}
