package org.ikasan.studio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.UIUtils;

/**
 * @deprecated
 */
public class PsiAtAction extends AnAction
{
   private static final Logger log = Logger.getLogger(PsiAtAction.class);

   private String projectKey;

   public PsiAtAction () {
      super();
}

   /**
    * This constructor support test data injection.
    * @param projectKey
    */
   public PsiAtAction(String projectKey) {
      super();
      this.projectKey = projectKey;
   }

   /**
    * Display data in message window
    * @param ae supplied action event
    */
   @Override
   public void actionPerformed(AnActionEvent ae)
   {
//      UIUtils.displayMessage(projectKey, getKeyData(ae));
//      if(caretModel.getCurrentCaret().hasSelection())
//      {
//         String query = caretModel.getCurrentCaret().getSelectedText().replace(' ', '+') + languageTag;
//         BrowserUtil.browse("https://stackoverflow.com/search?q=" + query);
//      }
   }

   /**
    * Get some key data about project so we can prove we can retrieve data
    * @param ae supplied action event
    * @return some data to be displayed in message box
    */
   protected String getKeyData(AnActionEvent ae) {
      StringBuilder message = new StringBuilder();
////      final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
////      CaretModel caretModel = editor.getCaretModel();
//
//      // get the psi file from the action event.
//      PsiFile psiFile = ae.getData(CommonDataKeys.PSI_FILE);
//      if(psiFile != null) {
////         PIPSIIkasanModel pipsiIkasanModel = (PIPSIIkasanModel)Context.get("pipsiIkasanModel");
//         PIPSIIkasanModel pipsiIkasanModel = new PIPSIIkasanModel(projectKey);
//         IkasanModule ikasanModule = pipsiIkasanModel.buildIkasanModule(psiFile);
//
//         message.append(ikasanModule.toString());
////         message.append(StudioPsiUtils.getSimpleFileData(file));
//      }
//
//      message.append("looking for Module config = " + StudioPsiUtils.findClass(ae.getProject(), "ModuleConfig"));
      return message.toString();
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
}
