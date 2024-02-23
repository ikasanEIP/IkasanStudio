package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.Context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class LaunchH2Action implements ActionListener {
   private final String projectKey;

   public LaunchH2Action(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Properties properties = Context.getApplicationProperties(projectKey);
      String webPortNumber = properties.getProperty("h2.web.port");
      String h2DbPortNumber = properties.getProperty("h2.db.port");
      BrowserUtil.browse("http:localhost:" + (webPortNumber != null ? webPortNumber : "8091") + "/login.do");
   }
}
