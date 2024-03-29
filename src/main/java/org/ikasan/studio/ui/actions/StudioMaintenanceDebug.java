package org.ikasan.studio.ui.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;

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

   /**
    * Only make this action visible when text is selected.
    * @param ae supplied action event
    */
   @Override
   public void update(AnActionEvent ae)
   {
      final Editor editor = ae.getRequiredData(CommonDataKeys.EDITOR);
      CaretModel caretModel = editor.getCaretModel();
      ae.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
   }

   @Override
   public void actionPerformed(AnActionEvent ae)
   {
      String text = "Any debug action can be placed here";
      LOG.info(text);
   }
}
