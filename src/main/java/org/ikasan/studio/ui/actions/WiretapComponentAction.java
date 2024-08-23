package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Decorator;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WiretapComponentAction implements ActionListener {
   private static final Logger LOG = Logger.getInstance("#WiretapComponentAction");
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
         !((FlowElement)ikasanBasicElement).getComponentMeta().isDebug()) {

         FlowElement flowElement = ((FlowElement)ikasanFlowComponentToRemove);

         flowElement.addDecorator(
              Decorator.decoratorBuilder()
                   .type(type.toString())
                   .name(beforeOrAfter.toString() + " " + flowElement.getName())
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