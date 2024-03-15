package org.ikasan.studio.ui.actions;

import org.ikasan.studio.build.model.ikasan.instance.BasicElement;
import org.ikasan.studio.build.model.ikasan.instance.Flow;
import org.ikasan.studio.build.model.ikasan.instance.FlowElement;
import org.ikasan.studio.build.model.ikasan.instance.Module;
import org.ikasan.studio.ui.Context;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeleteComponentAction implements ActionListener {
   private final String projectKey;
   private final BasicElement component;

   public DeleteComponentAction(String projectKey, BasicElement component) {
      this.projectKey = projectKey;
      this.component = component;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (component instanceof FlowElement ikasanFlowComponentToRemove) {
         Flow parentFlow = ikasanFlowComponentToRemove.getContainingFlow();
         if (parentFlow != null) {
            parentFlow.removeFlowElement(ikasanFlowComponentToRemove);
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      } else if (component instanceof Flow ikasanFlowToRemove) {
         Module ikasanModule = Context.getIkasanModule(projectKey);
         ikasanModule.getFlows().remove(ikasanFlowToRemove);
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      }
   }
}
