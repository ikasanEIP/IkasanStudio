package org.ikasan.studio.ui.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.ikasan.studio.ui.UiContext.IKASAN_NOTIFICATION_GROUP;

public class ModelRefreshAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#ModelRefreshAction");
   private final String projectKey;

   public ModelRefreshAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);

      if (module != null) {
         IKASAN_NOTIFICATION_GROUP
                 .createNotification("Recreating json model from memory instance.", NotificationType.INFORMATION)
                 .notify(UiContext.getProject(projectKey));
         PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
         pipsiIkasanModel.generateJsonFromModelInstance();
         module = UiContext.getIkasanModule(projectKey);

         LOG.info("ikasan module was " + ComponentIO.toJson(module));

         UiContext.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
         UiContext.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
      } else {
         IKASAN_NOTIFICATION_GROUP
                 .createNotification("Refresh can't be launched unless a module is defined.", NotificationType.INFORMATION)
                 .notify(UiContext.getProject(projectKey));
      }
   }
}
