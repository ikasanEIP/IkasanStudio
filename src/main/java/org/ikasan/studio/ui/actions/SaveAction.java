package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

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
         StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.SavingImage"));
         // Transparent background / SVG cant get this to work for now, see commented-out block below.
         String[] extensions = new String[]{"png", "jpg"};
         // Shared with ModuleDiagramAutoSaver, so a manual save and the auto-save on project close agree on
         // where to look for a module's diagram.
         String imageFileName = DesignerCanvas.moduleDiagramFileName(module);
         // FileChooserFactory (behind StudioProjectFiles.chooseSaveFile) handles platform-specific file
         // extensions automatically, including using native dialogs on Mac.
         File file = StudioProjectFiles.chooseSaveFile(project,
                 StudioBundle.message("dialog.SaveAsImage"),
                 StudioBundle.message("message.ChooseTheDestinationToSaveTheImage"), extensions, imageFileName);

         if (file == null) {
            return;
         }

         String imageFormat = FileUtilRt.getExtension(file.getName());
         if (imageFormat.trim().isEmpty()) {
            imageFormat = "png";
         }
// SVG has temporary compatibility problems with Intellij Verify.
//      if ("svg".equals(imageFormat)) {
//         uiContext.getDesignerCanvas(project).saveAsSvg(file, false);
//      } else {
         uiContext.getDesignerCanvas().saveAsImage(file, imageFormat, false);
//      }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.SaveOfImageCantBeLaunchedUnlessAModuleIsDefined"));
      }
   }
}
