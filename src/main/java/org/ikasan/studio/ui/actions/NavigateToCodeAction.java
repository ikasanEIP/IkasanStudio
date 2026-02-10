package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigateToCodeAction implements ActionListener {
   private final Project project;
   private final BasicElement ikasanBasicElement;
   boolean jumpToLine;

   public NavigateToCodeAction(Project project, BasicElement ikasanBasicElement, boolean jumpToLine) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
      this.jumpToLine = jumpToLine;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement);
      if (viewHandler != null) {
         if (viewHandler.getOffsetInclassToNavigateTo() != 0 && jumpToLine) {
            StudioUIUtils.displayMessage(project, "Jump to offset " + viewHandler.getOffsetInclassToNavigateTo());
            Navigator.navigateToSource(project, viewHandler.getClassToNavigateTo(), viewHandler.getOffsetInclassToNavigateTo());
         } else {
            Navigator.navigateToSource(project, viewHandler.getClassToNavigateTo());
         }
      }
   }
}
