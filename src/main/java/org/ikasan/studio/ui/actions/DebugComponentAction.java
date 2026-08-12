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
         DesignerCanvas designerCanvas = project.getService(UiContext.class).getDesignerCanvas();
         if (designerCanvas != null) {
            designerCanvas.insertDebugComponentAfter(ikasanFlowComponent);
         }
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DebugCanOnlyBeAddedToFlowElements"));
      }
   }
}