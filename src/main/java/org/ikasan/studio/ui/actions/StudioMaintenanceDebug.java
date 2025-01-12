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
    * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
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
