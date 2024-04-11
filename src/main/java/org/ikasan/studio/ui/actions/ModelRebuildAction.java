package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRebuildAction implements ActionListener {
   private final String projectKey;

   public ModelRebuildAction(String projectKey) {
      this.projectKey = projectKey;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);

      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "Rebuilding source code from memory model.");
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "Rebuilding can't be launched unless a module is defined.");
      }
   }
}
