package org.ikasan.studio.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.io.ComponentIO;
import org.ikasan.studio.model.ikasan.instance.Module;

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
      // @TODO MODEL
//      StudioPsiUtils.generateModelFromSourceCode(projectKey, false);
      Module module = Context.getIkasanModule(projectKey);
      LOG.info("ikasan module was " + ComponentIO.toJson(module));

      Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
   }
}
