package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DecoratorComponentAction implements ActionListener {
   private final Project project;
   private final BasicElement ikasanBasicElement;
   private final DECORATOR_TYPE decoratorType;
   private final DECORATOR_POSITION beforeOrAfter;
   private final boolean isAdd;

   public DecoratorComponentAction(Project project, BasicElement ikasanBasicElement, boolean isAdd, DECORATOR_TYPE decoratorType, DECORATOR_POSITION beforeOrAfter) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
      this.decoratorType = decoratorType;
      this.beforeOrAfter = beforeOrAfter;
      this.isAdd = isAdd;
   }
   /**
    * doDelete for component under mouse
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponent &&
         !(ikasanBasicElement.getComponentMeta().isDebug())) {

         if (isAdd) {
            ikasanFlowComponent.addDecorator(
                    Decorator.decoratorBuilder()
                            .type(decoratorType.toString())
                            .name(beforeOrAfter.toString() + " " + ikasanFlowComponent.getIdentity())
                            .configurationId("0")
                            .configurable(false)
                            .build()
            );
         } else {
            ikasanFlowComponent.removeDecorator(decoratorType, beforeOrAfter);
         }
         StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project);
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "Wiretap can only be added to a non-debug flow elements");
      }
   }
}