package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EditComponentAction implements ActionListener {
   private final Project project;
   private final BasicElement ikasanBasicElement;

   public EditComponentAction(Project project, BasicElement ikasanBasicElement) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
   }

   /**
    * Edit the component in the properties panel.
    * @param actionEvent the event to be processed
    */
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (ikasanBasicElement instanceof FlowElement) {
         UiContext uiContext = project.getService(UiContext.class);
         DesignerCanvas canvasPanel = uiContext.getDesignerCanvas();
         canvasPanel.editComponent(ikasanBasicElement);
      }
   }
}
