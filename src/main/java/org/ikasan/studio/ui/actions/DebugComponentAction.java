package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugComponentAction implements ActionListener {
   private final String projectKey;
   private final BasicElement ikasanBasicElement;

   public DebugComponentAction(String projectKey, BasicElement ikasanBasicElement) {
      this.projectKey = projectKey;
      this.ikasanBasicElement = ikasanBasicElement;
   }
   /**
    * doDebug for component under mouse
    * // @TODO this is broken, needs to add a debug like dragging debug component from right hand palette
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponent) {
         Flow parentFlow = ikasanFlowComponent.getContainingFlow();
         if (parentFlow != null) {
            parentFlow.removeFlowElement(ikasanFlowComponent);
            StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
         }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "Debug can only be added to flow elements");
      }
   }
}