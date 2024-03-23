package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeleteComponentAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#DeleteComponentAction");
   private final String projectKey;
   private final BasicElement ikasanBasicElement;

   public DeleteComponentAction(String projectKey, BasicElement ikasanBasicElement) {
      this.projectKey = projectKey;
      this.ikasanBasicElement = ikasanBasicElement;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponentToRemove) {
         Flow parentFlow = ikasanFlowComponentToRemove.getContainingFlow();
         if (parentFlow != null) {
            parentFlow.removeFlowElement(ikasanFlowComponentToRemove);
         } else {
            LOG.warn("Attrempt to remove flow element " + ikasanBasicElement + " because its containing flow could not be found.");
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      } else if (ikasanBasicElement instanceof Flow ikasanFlowToRemove) {
         Module ikasanModule = UiContext.getIkasanModule(projectKey);
         ikasanModule.getFlows().remove(ikasanFlowToRemove);
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      }
   }
}
