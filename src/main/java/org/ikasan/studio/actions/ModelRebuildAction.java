package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRebuildAction implements ActionListener {
   private static final Logger log = Logger.getLogger(ModelRebuildAction.class);
   private String projectKey;

   public ModelRebuildAction(String projectKey) {
      this.projectKey = projectKey;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      StudioPsiUtils.resetIkasanModelFromSourceCode(projectKey, false);
      IkasanModule module = Context.getIkasanModule(projectKey);
      log.info("Ikasan module was " + module.toString());

      Context.getDesignerCanvas(projectKey).setInitialiseCanvas(true);
      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
   }
}
