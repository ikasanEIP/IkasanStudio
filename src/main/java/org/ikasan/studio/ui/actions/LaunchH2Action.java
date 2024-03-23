package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.ikasan.studio.ui.UiContext.IKASAN_NOTIFICATION_GROUP;

public class LaunchH2Action implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#LaunchH2Action");
   private final String projectKey;
   public LaunchH2Action(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);
      if (module != null) {
         IKASAN_NOTIFICATION_GROUP
              .createNotification("Sent request to your browser to launch the H2 console.", NotificationType.INFORMATION)
              .notify(UiContext.getProject(projectKey));

         BrowserUtil.browse("http:localhost:" + module.getH2WebPortNumber() + "/login.do");
      } else {
         IKASAN_NOTIFICATION_GROUP
              .createNotification("H2 console can't be launched unless a module is defined.", NotificationType.INFORMATION)
              .notify(UiContext.getProject(projectKey));

      }
   }
}
