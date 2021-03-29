package org.ikasan.studio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.sun.istack.NotNull;
import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.generator.ApplicationTemplate;
import org.ikasan.studio.generator.ModuleConfigTemplate;
import org.ikasan.studio.generator.PropertiesTemplate;
import org.ikasan.studio.generator.VelocityUtils;

import java.util.Arrays;

public class StudioMaintenanceDebug extends AnAction
{
   private static final Logger log = Logger.getLogger(StudioMaintenanceDebug.class);

   private String projectKey;

   public StudioMaintenanceDebug() {
      super();
}

   /**
    * This constructor support test data injection.
    * @param projectKey for the current project
    */
   public StudioMaintenanceDebug(String projectKey) {
      super();
      this.projectKey = projectKey;
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
   public void actionPerformed(@NotNull AnActionEvent ae)
   {
      System.out.println("actionPerformed test");
//      try {
         StudioUtils.readIkasanComponentProperties("/studio/componentDefinitions/BROKER_en_GB.csv");
//      } catch (IOException e) {
//         e.printStackTrace();
//         log.error("XXXXXXXXXXXXXXXXXXXXXXXXXX can read configs");
//      }

   }

   private void testModelUpdate(AnActionEvent ae) {
//      VelocityUtils.generateFromTemplate("ApplicationTemplate.vm", null);

      final PsiFile file = ae.getData(LangDataKeys.PSI_FILE);
      final Project project = ae.getProject();

      if (file == null || project == null) {
         return;
      }

      final VirtualFile virtualFile = file.getVirtualFile();
      ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Arrays.asList(virtualFile));

      final String clazzName = "MyFile";
      final String fileName = "MyFile.java";
      String HELLO = "public class " + clazzName + " {public static void main(String[] args) {System.out.println(java.util.Arrays.asList(\"Hi\"));}}";

      CommandProcessor.getInstance().executeCommand(
              project,
              () -> ApplicationManager.getApplication().runWriteAction(
                      () -> {
                         ApplicationTemplate.create(project);
                         ModuleConfigTemplate.create(project);
                         //@todo check the file produced, if non produced, bring up messeage in Intillij Error messging window.
                         PropertiesTemplate.create(project);
                      }),
              "Maintenance Debug Command",
              "Undo group ID");
   }

   /**
    * Get some key data about project so we can prove we can retrieve data
    * @param ae supplied action event
    * @return some data to be displayed in message box
    */
   protected String getKeyData(AnActionEvent ae) {
      StringBuilder message = new StringBuilder();
      message.append("Thou could ny anything");

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
//      message.append("looking for Module config = " + StudioPsiUtils.findClass(ae.getProject(), "ModuleConfigTemplate"));

      //      StudioPsiUtils.getAllSourceRootsForProject(ae.getProject());
//      StudioUIUtils.displayMessage(projectKey, getKeyData(ae));
//      VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(ae.getProject(),StudioPsiUtils.JAVA_CODE);
//      PsiDirectory baseDir = PsiDirectoryFactory.getInstance(ae.getProject()).createDirectory(sourceRoot);
//      PsiDirectory bobDir = StudioPsiUtils.createPackage(baseDir, "bob");
//      PsiFile newFile = StudioPsiUtils.createFileInDirectory(bobDir, "myFile", "Some text", ae.getProject());
//      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(ae.getProject());
//      documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(newFile));
//      StudioPsiUtils.
//      if(caretModel.getCurrentCaret().hasSelection())
//      {
//         String query = caretModel.getCurrentCaret().getSelectedText().replace(' ', '+') + languageTag;
//         BrowserUtil.browse("https://stackoverflow.com/search?q=" + query);
//      }

      return message.toString();
   }


}
