package org.ikasan.studio.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRebuildAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#ModelRebuildAction");
   private final String projectKey;

   public ModelRebuildAction(String projectKey) {
      this.projectKey = projectKey;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      StudioPsiUtils.generateModelFromSourceCode(projectKey, false);
      IkasanModule module = Context.getIkasanModule(projectKey);
      LOG.info("ikasan module was " + StudioUtils.toJson(module));

      Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
   }
}
