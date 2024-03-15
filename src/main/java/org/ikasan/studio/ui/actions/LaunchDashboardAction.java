package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.ui.Context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class LaunchDashboardAction implements ActionListener {
   private final String projectKey;

   public LaunchDashboardAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Properties properties = Context.getApplicationProperties(projectKey);
      String portNumber = properties.getProperty("server.port");
      BrowserUtil.browse("http:localhost:" + (portNumber != null ? portNumber : "8080") + "/" + properties.getProperty("server.servlet.context-path"));
   }
}
