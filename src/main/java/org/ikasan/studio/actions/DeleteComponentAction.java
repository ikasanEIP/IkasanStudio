package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanComponent;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeleteComponentAction implements ActionListener {
   private static final Logger log = Logger.getLogger(DeleteComponentAction.class);
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
         IkasanModule ikasanModule = Context.getIkasanModule(projectKey);
         IkasanFlow parentFlow = ikasanFlowComponentToRemove.getParent();
         IkasanFlow flowToChange = ikasanModule.getFlow(parentFlow);
         if (flowToChange != null) {
            flowToChange.removeFlowElement(ikasanFlowComponentToRemove);
            log.info("Removed component " + ikasanFlowComponentToRemove);
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      }
   }
}
