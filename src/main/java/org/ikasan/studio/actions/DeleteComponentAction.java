package org.ikasan.studio.actions;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.ikasan.IkasanComponent;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeleteComponentAction implements ActionListener {
   private String projectKey;
   IkasanComponent component;

   public DeleteComponentAction(String projectKey, IkasanComponent component) {
      this.projectKey = projectKey;
      this.component = component;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (component instanceof IkasanFlowComponent) {
         IkasanFlowComponent ikasanFlowComponentToRemove = (IkasanFlowComponent)component;
         IkasanFlow parentFlow = ikasanFlowComponentToRemove.getParent();
         if (parentFlow != null) {
            parentFlow.removeFlowElement(ikasanFlowComponentToRemove);
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      } else if (component instanceof IkasanFlow) {
         IkasanFlow ikasanFlowToRemove = (IkasanFlow)component;
         IkasanModule ikasanModule = Context.getIkasanModule(projectKey);
         ikasanModule.getFlows().remove(ikasanFlowToRemove);
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      }
   }
}
