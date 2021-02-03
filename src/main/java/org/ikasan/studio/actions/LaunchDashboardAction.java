package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchDashboardAction implements ActionListener {
   private String projectKey;

   public LaunchDashboardAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      BrowserUtil.browse("http:localhost:8090/example-im");
   }
}
