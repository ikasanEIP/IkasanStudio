package org.ikasan.studio.ui.actions;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SaveAction implements ActionListener {
   private final Project project;

   public SaveAction(Project project) {
      this.project = project;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      UiContext uiContext = project.getService(UiContext.class);
      Module module = uiContext.getIkasanModule();

      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(project, "Saving image.");
         boolean transparentBackground = false ; // cant get this to work for now.
//      String[] extensions = transparentBackground ? new String[]{"png", "svg"} : new String[]{"png", "jpg", "svg"};
         String[] extensions = transparentBackground ? new String[]{"png"} : new String[]{"png", "jpg",};
         // Modern approach: FileChooserFactory automatically uses native dialogs on Mac
         // No need to check for deprecated registry key ide.mac.native.save.dialog
         // The platform automatically uses native file choosers when available
         FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Save as Image", "Choose the destination to save the image", extensions);
         FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, (Project) null);

         String moduleName = uiContext.getIkasanModule().getComponentName();
         // FileChooserFactory handles platform-specific file extensions automatically
         String imageFileName = "ModuleDiagram-" + moduleName + ".png";
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
//         uiContext.getDesignerCanvas(project).saveAsSvg(file, transparentBackground);
//      } else {
         uiContext.getDesignerCanvas().saveAsImage(file, imageFormat, transparentBackground);
//      }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "Save of image can't be launched unless a module is defined.");
      }
   }
}
