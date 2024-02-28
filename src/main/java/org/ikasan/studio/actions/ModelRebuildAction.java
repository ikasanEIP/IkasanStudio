package org.ikasan.studio.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.io.ComponentIO;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;

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
      PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
      pipsiIkasanModel.generateSourceFromModelInstance();
      StudioPsiUtils.generateModelInstanceFromJSON(projectKey, false);

      Module module = Context.getIkasanModule(projectKey);
      LOG.info("ikasan module was " + ComponentIO.toJson(module));

      Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
      Context.getDesignerCanvas(projectKey).repaint();
   }
}
