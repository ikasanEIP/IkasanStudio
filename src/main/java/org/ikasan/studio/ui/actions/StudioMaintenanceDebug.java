package org.ikasan.studio.ui.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;

public class StudioMaintenanceDebug extends AnAction
{
   public static final Logger LOG = Logger.getInstance("#StudioMaintenanceDebug");
    public StudioMaintenanceDebug() {
      super();
}

   /**
    * This constructor support test data injection.
    * @param projectKey for the current project
    */
   public StudioMaintenanceDebug(String projectKey) {
      super();
   }

   @Override
   public void actionPerformed(AnActionEvent ae)
   {
      String text = "STUDIO: Any debug action can be placed here";
      LOG.info(text);
   }
}
