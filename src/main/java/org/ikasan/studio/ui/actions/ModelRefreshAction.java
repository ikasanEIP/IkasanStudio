package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.ui.Context;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRefreshAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#ModelRefreshAction");
   private final String projectKey;

   public ModelRefreshAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
      pipsiIkasanModel.generateJsonFromModelInstance();

      Module module = Context.getIkasanModule(projectKey);
      LOG.info("ikasan module was " + ComponentIO.toJson(module));

      Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
   }
}
