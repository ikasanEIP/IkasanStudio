package org.ikasan.studio.ui.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static org.ikasan.studio.ui.UiContext.IKASAN_NOTIFICATION_GROUP;

public class SaveAction implements ActionListener {
   private final String projectKey;

   public SaveAction(String projectKey) {
      this.projectKey = projectKey;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);

      if (module != null) {
         IKASAN_NOTIFICATION_GROUP
                 .createNotification("Saving image.", NotificationType.INFORMATION)
                 .notify(UiContext.getProject(projectKey));
         boolean transparentBackground = false ; // cant get this to work for now.
//      String[] extensions = transparentBackground ? new String[]{"png", "svg"} : new String[]{"png", "jpg", "svg"};
         String[] extensions = transparentBackground ? new String[]{"png"} : new String[]{"png", "jpg",};
         boolean isMacNativeSaveDialog = SystemInfo.isMac && Registry.is("ide.mac.native.save.dialog");
         FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Save as Image", "Choose the destination to save the image", extensions);
         FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, (Project) null);

         String moduleName = UiContext.getIkasanModule(projectKey).getComponentName();
         String imageFileName = "ModuleDiagram-" + moduleName + (isMacNativeSaveDialog ? ".png" : "");
         VirtualFileWrapper vf = dialog.save(imageFileName);

         if (vf == null) {
            return;
         }

         File file = vf.getFile();
         String imageFormat = FileUtilRt.getExtension(file.getName());
         if (imageFormat.trim().isEmpty()) {
            imageFormat = "png";
         }
// SVG has temporary compatibility problems with Intellij Verify.
//      if ("svg".equals(imageFormat)) {
//         UiContext.getDesignerCanvas(projectKey).saveAsSvg(file, transparentBackground);
//      } else {
         UiContext.getDesignerCanvas(projectKey).saveAsImage(file, imageFormat, transparentBackground);
//      }
      } else {
         IKASAN_NOTIFICATION_GROUP
                 .createNotification("Save of image can't be launched unless a module is defined.", NotificationType.INFORMATION)
                 .notify(UiContext.getProject(projectKey));
      }





   }
}
