package org.ikasan.studio.actions;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SaveAction implements ActionListener {
   private static final org.apache.log4j.Logger log = Logger.getLogger(ActionListener.class);
   private String projectKey;

   public SaveAction(String projectKey) {
      this.projectKey = projectKey;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      boolean transparentBackground = false ; // cant get this to work for now.
      boolean isMacNativSaveDialog = SystemInfo.isMac && Registry.is("ide.mac.native.save.dialog");
      String[] extensions = transparentBackground ? new String[]{"png", "svg"} : new String[]{"png", "jpg", "svg"};
      FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Save as image", "Choose the destination to save the image", extensions);
      FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, (Project) null);

      String moduleName = Context.getIkasanModule(projectKey).getName();
      String imageFileName = "ModuleDiagram-" + moduleName + (isMacNativSaveDialog ? ".png" : "");
      VirtualFileWrapper vf = dialog.save(null, imageFileName);

      if (vf == null) {
          return;
      }

      File file = vf.getFile();
      String imageFormat = FileUtilRt.getExtension(file.getName());
      if (StringUtils.isBlank(imageFormat)) {
          imageFormat = "png";
      }

      if ("svg".equals(imageFormat)) {
         Context.getDesignerCanvas(projectKey).saveAsSvg(file, transparentBackground);
      } else {
         Context.getDesignerCanvas(projectKey).saveAsImage(file, imageFormat, transparentBackground);
      }
   }
}
