package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugAction implements ActionListener {
   private static final Logger log = Logger.getLogger(DebugAction.class);
   private String projectKey;

   public DebugAction(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      IkasanModule module = Context.getIkasanModule(projectKey);
      log.info("Ikasan module was " + module.toString());
//      Context.getDesignerCanvas(projectKey).setInitialiseCanvas(true);
//      Context.getDesignerCanvas(projectKey).repaint(); // Tell swing the panel is dirty and needs re-painting.
//      String message = StudioPsiUtils.getIkasanModuleFromSourceCode().toString();
//      StudioUIUtils.displayMessage(message);
   }
}
