package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchDashboardAction implements ActionListener {
   private static final Logger log = Logger.getLogger(LaunchDashboardAction.class);
   private String projectKey;

   public LaunchDashboardAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      BrowserUtil.browse("http:localhost:8090/example-im");
   }
}
