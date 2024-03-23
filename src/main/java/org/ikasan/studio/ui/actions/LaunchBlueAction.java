package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.ikasan.studio.ui.UiContext.IKASAN_NOTIFICATION_GROUP;

public class LaunchBlueAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#LaunchBlueAction");
   private final String projectKey;

   public LaunchBlueAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {


      Module module = UiContext.getIkasanModule(projectKey);
      if (module != null) {
         IKASAN_NOTIFICATION_GROUP
            .createNotification("Sent request to your browser to launch the blue console.", NotificationType.INFORMATION)
            .notify(UiContext.getProject(projectKey));
         BrowserUtil.browse("http:localhost:" + (module.getPort() != null ? module.getPort() : "8080") + "/" + module.getName().toLowerCase());
      } else {
         IKASAN_NOTIFICATION_GROUP
            .createNotification("Blue console can't be launched unless a module is defined.", NotificationType.INFORMATION)
            .notify(UiContext.getProject(projectKey));
      }
   }
}
