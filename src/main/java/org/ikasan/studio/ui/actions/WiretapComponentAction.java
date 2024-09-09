package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Decorator;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WiretapComponentAction implements ActionListener {
   private final String projectKey;
   private final BasicElement ikasanBasicElement;
   private final Decorator.TYPE type;
   private final Decorator.POSITION beforeOrAfter;

   public WiretapComponentAction(String projectKey, BasicElement ikasanBasicElement, Decorator.TYPE type, Decorator.POSITION beforeOrAfter) {
      this.projectKey = projectKey;
      this.ikasanBasicElement = ikasanBasicElement;
      this.type = type;
      this.beforeOrAfter = beforeOrAfter;
   }
   /**
    * doDelete for component under mouse
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponentToRemove &&
         !(ikasanBasicElement.getComponentMeta().isDebug())) {

          ikasanFlowComponentToRemove.addDecorator(
              Decorator.decoratorBuilder()
                   .type(type.toString())
                   .name(beforeOrAfter.toString() + " " + ikasanFlowComponentToRemove.getName())
                   .configurationId("0")
                   .configurable(false)
                   .build()
         );
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(projectKey);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "Wiretap can only be added to a non-debug flow elements");
      }
   }
}