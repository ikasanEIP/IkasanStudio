package org.ikasan.studio.ui.actions;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementRemoval;
import org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.intellij.IkasanStudioSettings;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DeleteComponentAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#DeleteComponentAction");
   private final Project project;
   private final BasicElement ikasanBasicElement;

   public DeleteComponentAction(Project project, BasicElement ikasanBasicElement) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
   }

   /**
    * doDelete for component under mouse
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponentToRemove) {
         Flow parentFlow = ikasanFlowComponentToRemove.getContainingFlow();

         if (parentFlow != null) {
            if (ikasanBasicElement.getComponentMeta().isRouter()) {
               final int answer = Messages.showYesNoDialog(project,
                       StudioBundle.message("message.DeletingARouteWillAlsoDeleteAllDownstreamElements"),
                       StudioBundle.message("dialog.Warning"), null);
               if (answer != Messages.YES) {
                  return;
               }
            }
            Module ikasanModule = project.getService(UiContext.class).getIkasanModule();
            CommandProcessor.getInstance().executeCommand(project, () -> {
               List<FlowElement> elementsBeforeRemoval = parentFlow.getFlowElementsNoExternalEndPoints();
               FlowElementRemoval removal = parentFlow.removeFlowElement(ikasanFlowComponentToRemove);
               List<FlowElement> elementsAfterRemoval = parentFlow.getFlowElementsNoExternalEndPoints();
               List<FlowElement> removedElements = new ArrayList<>(elementsBeforeRemoval);
               removedElements.removeAll(elementsAfterRemoval);
               deleteAssociatedUserCodeFiles(ikasanModule, parentFlow, removedElements);
               if (removal != null) {
                  UndoManager.getInstance(project).undoableActionPerformed(
                          new DeleteComponentUndoableAction(project, removal::undo, removal::redo));
               }
               // Kept inside this command so the JSON model save nests into (and is undone/redone as part
               // of) the same undo step as the model mutation above, rather than becoming a separate,
               // invisible-on-canvas undo entry (see DeleteComponentUndoableAction).
               StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
            }, StudioBundle.message("menu.DeleteComponent"), null);
         } else {
            LOG.warn("STUDIO: Attempt to remove flow element " + ikasanBasicElement + " failed because its containing flow could not be found.");
         }
      } else if (ikasanBasicElement instanceof Flow ikasanFlowToRemove) {
         if (ikasanFlowToRemove.hasAnyComponents()) {
            final int answer = Messages.showYesNoDialog(project,
                    StudioBundle.message("message.DeletingAFlowWillDeleteAllTheElements"),
                    StudioBundle.message("dialog.Warning"), null);
            if (answer == Messages.YES) {
               UiContext uiContext = project.getService(UiContext.class);
               Module ikasanModule = uiContext.getIkasanModule();
               List<Flow> flows = ikasanModule.getFlows();
               int flowIndex = flows.indexOf(ikasanFlowToRemove);
               CommandProcessor.getInstance().executeCommand(project, () -> {
                  List<FlowElement> removedElements = ikasanFlowToRemove.getFlowElementsNoExternalEndPoints();
                  flows.remove(ikasanFlowToRemove);
                  deleteAssociatedUserCodeFiles(ikasanModule, ikasanFlowToRemove, removedElements);
                  UndoManager.getInstance(project).undoableActionPerformed(new DeleteComponentUndoableAction(project,
                          () -> flows.add(Math.min(Math.max(flowIndex, 0), flows.size()), ikasanFlowToRemove),
                          () -> flows.remove(ikasanFlowToRemove)));
                  // Kept inside this command so the JSON model save nests into (and is undone/redone as
                  // part of) the same undo step as the model mutation above.
                  StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
               }, StudioBundle.message("menu.DeleteComponent"), null);
            }
         }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ActionIgnoredYouCanOnlyDeleteFlowElementsOrFlows"));
      }
   }

   /**
    * Components such as Debug or CustomConverter generate their own hand-editable class under the project's user
    * source tree (see {@code PIPSIIkasanModel.generateAndSaveUserImplementClassStubsForFlow}). When such a
    * component is removed from the canvas (directly, or as a side effect of deleting a router or a whole flow),
    * that generated class is no longer referenced by anything, so offer to delete it too.
    * @param ikasanModule containing the removed elements, used to resolve the user class package name
    * @param ownerFlow the flow the removed elements belonged to
    * @param removedElements flow elements that were just removed from the in-memory model
    */
   private void deleteAssociatedUserCodeFiles(Module ikasanModule, Flow ownerFlow, List<FlowElement> removedElements) {
      if (ikasanModule == null || ownerFlow == null || removedElements == null || removedElements.isEmpty()) {
         return;
      }
      for (FlowElement removedElement : removedElements) {
         if (!(removedElement instanceof FlowUserImplementedElement)) {
            continue;
         }
         ComponentProperty classNameProperty = removedElement.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
         String className = classNameProperty != null ? (String) classNameProperty.getValue() : null;
         if (className == null) {
            continue;
         }
         String packageName = GeneratorUtils.getUserImplementedClassesPackageName(ikasanModule, ownerFlow);
         VirtualFile userClassFile = StudioPsiUtils.getUserImplementedClassFile(project, packageName, className);
         if (userClassFile == null) {
            continue;
         }
         if (removedElement.getComponentMeta().isDebug() || confirmUserCodeDeletion(userClassFile.getPath())) {
            StudioPsiUtils.deleteFile(project, userClassFile);
         }
      }
   }

   /**
    * Confirm deletion of a generated user-code file, unless the user has previously opted out of this prompt
    * via the "don't ask again" checkbox (persisted in {@link IkasanStudioSettings}), in which case the deletion
    * proceeds automatically.
    * @param filePath of the user code file that would be deleted, shown to the user in the confirmation dialog
    * @return true if the file should be deleted
    */
   private boolean confirmUserCodeDeletion(String filePath) {
      if (!IkasanStudioSettings.isPromptBeforeDeletingUserCode()) {
         return true;
      }
      DoNotAskOption doNotAskOption = new DoNotAskOption.Adapter() {
         @Override
         public void rememberChoice(boolean isSelected, int exitCode) {
            if (isSelected) {
               IkasanStudioSettings.setPromptBeforeDeletingUserCode(false);
            }
         }
      };
      return MessageDialogBuilder.yesNo(
                      StudioBundle.message("dialog.Warning"),
                      StudioBundle.message("message.DeletingComponentWillDeleteUserCode", filePath))
              .asWarning()
              .doNotAsk(doNotAskOption)
              .ask(project);
   }
}