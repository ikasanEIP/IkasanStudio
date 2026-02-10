package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRebuildAction implements ActionListener {
   private final Project project;
   public static final Logger LOG = Logger.getInstance("#ModelRebuildAction");

   public ModelRebuildAction(Project project) {
      this.project = project;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      UiContext uiContext = project.getService(UiContext.class);
      Module module = uiContext.getIkasanModule();

      if (module != null) {
         StudioUIUtils.displayIdeaInfoMessage(project, "Rebuilding source code from memory model.");
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "Rebuilding can't be launched unless a module is defined.");
      }
   }
}
