package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRefreshAction implements ActionListener {
   private static final Logger log = Logger.getLogger(ModelRefreshAction.class);
   private String projectKey;

   public ModelRefreshAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {

      Context.getDesignerCanvas(projectKey).setInitialiseCanvas(true);
      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
//      String message = StudioPsiUtils.getIkasanModuleFromSourceCode().toString();
//      StudioUIUtils.displayMessage(message);
   }
}
