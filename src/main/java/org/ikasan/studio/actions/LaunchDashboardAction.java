package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class LaunchDashboardAction implements ActionListener {
   private String projectKey;

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
