package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.ui.Context;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

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
      pipsiIkasanModel.generateSourceFromModelInstance3(false);
      StudioPsiUtils.generateModelInstanceFromJSON(projectKey, false);

      Module module = Context.getIkasanModule(projectKey);
      LOG.info("ikasan module was " + ComponentIO.toJson(module));

      Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
      Context.getDesignerCanvas(projectKey).repaint();
   }
}
