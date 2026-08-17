package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugComponentAction implements ActionListener {
   private final Project project;
   private final BasicElement ikasanBasicElement;

   public DebugComponentAction(Project project, BasicElement ikasanBasicElement) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
   }

   /**
    * Inserts a Debug component immediately after the component under the mouse.
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement ikasanFlowComponent) {
         // Mirrors insertDebugComponentAfter's own defensive check, but here we can tell the user why.
         if (ikasanFlowComponent.getComponentMeta().isProducer() || ikasanFlowComponent.getComponentMeta().isDebug()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DebugCannotBeAddedAfterAProducerOrDebugComponent"));
            return;
         }
         DesignerCanvas designerCanvas = project.getService(UiContext.class).getDesignerCanvas();
         if (designerCanvas != null) {
            // Debug components have their mandatory properties auto-defaulted before the properties
            // popup would be shown (see DesignerCanvas.createViableComponent), so unlike a generic
            // dragged-in component, a null result here isn't expected to be an ordinary user cancel -
            // it means insertion genuinely failed (already logged inside insertDebugComponentAfter),
            // so it's safe to tell the user rather than fail silently.
            FlowElement debugComponent = designerCanvas.insertDebugComponentAfter(ikasanFlowComponent);
            if (debugComponent == null) {
               StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DebugComponentCouldNotBeAdded"));
            }
         }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DebugCanOnlyBeAddedToFlowElements"));
      }
   }
}