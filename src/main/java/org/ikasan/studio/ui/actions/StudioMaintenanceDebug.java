package org.ikasan.studio.ui.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class StudioMaintenanceDebug extends AnAction
{
   public static final Logger LOG = Logger.getInstance("#StudioMaintenanceDebug");
    public StudioMaintenanceDebug() {
      super();
}

   @Override
   public void actionPerformed(@NotNull AnActionEvent ae)
   {
      String text = "STUDIO: Any debug action can be placed here";
      LOG.info(text);
   }
}
