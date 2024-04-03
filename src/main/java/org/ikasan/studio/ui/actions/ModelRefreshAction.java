package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRefreshAction implements ActionListener {
   private final String projectKey;

   public ModelRefreshAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);

      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "Recreating json model from memory instance.");

          StudioUIUtils.resetModelFromDisk(projectKey);

          if (UiContext.getPalettePanel(projectKey) != null) {
              UiContext.getPalettePanel(projectKey).resetPallette();
          }
          UiContext.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
          UiContext.getDesignerCanvas(projectKey).repaint();
          UiContext.getPalettePanel(projectKey).repaint();
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "Refresh can't be launched unless a module is defined.");
      }
   }
}
