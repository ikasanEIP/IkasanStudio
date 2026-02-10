package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
            if (((FlowElement)ikasanBasicElement).getComponentMeta().isRouter()) {
               final int answer = Messages.showYesNoDialog(project,
                       "Deleting a route will also delete all downstream elements, do you wish to proceed", "Warning", null);
               if (answer != Messages.YES) {
                  return;
               }
            }
            parentFlow.removeFlowElement(ikasanFlowComponentToRemove);
         } else {
            LOG.warn("STUDIO: Attempt to remove flow element " + ikasanBasicElement + " failed because its containing flow could not be found.");
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
      } else if (ikasanBasicElement instanceof Flow ikasanFlowToRemove) {
         if (((Flow)ikasanBasicElement).hasAnyComponents()) {
            final int answer = Messages.showYesNoDialog(project,
                    "Deleteing a flow will delete all the elements, do you wish to proceed", "Warning", null);
            if (answer == Messages.YES) {
               UiContext uiContext = project.getService(UiContext.class);
               Module ikasanModule = uiContext.getIkasanModule();
               ikasanModule.getFlows().remove(ikasanFlowToRemove);
               StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
            }
         }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "Action ignored, you can only delete flow elements or flows");
      }
   }
}
