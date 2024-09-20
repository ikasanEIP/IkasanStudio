package org.ikasan.studio.ui.actions;

import org.ikasan.studio.Navigator;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigateToCodeAction implements ActionListener {
   private final String projectKey;
   private final BasicElement ikasanBasicElement;
   boolean jumpToLine;

   public NavigateToCodeAction(String projectKey, BasicElement ikasanBasicElement, boolean jumpToLine) {
      this.projectKey = projectKey;
      this.ikasanBasicElement = ikasanBasicElement;
      this.jumpToLine = jumpToLine;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(projectKey, ikasanBasicElement);
      if (viewHandler != null) {
         if (viewHandler.getOffsetInclassToNavigateTo() != 0 && jumpToLine) {
            StudioUIUtils.displayMessage(projectKey, "Jumpt to offset " + viewHandler.getOffsetInclassToNavigateTo());
            Navigator.navigateToSource(projectKey, viewHandler.getClassToNavigateTo(), viewHandler.getOffsetInclassToNavigateTo());
         } else {
            Navigator.navigateToSource(projectKey, viewHandler.getClassToNavigateTo());
         }
      }
   }
}
