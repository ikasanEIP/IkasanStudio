package org.ikasan.studio.actions;

import org.ikasan.studio.Navigator;
import org.ikasan.studio.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigateToCodeAction implements ActionListener {
   private final String projectKey;
   BasicElement component;
   boolean jumpToLine;

   public NavigateToCodeAction(String projectKey, BasicElement component, boolean jumpToLine) {
      this.projectKey = projectKey;
      this.component = component;
      this.jumpToLine = jumpToLine;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (component.getViewHandler().getOffsetInclassToNavigateTo() != 0 && jumpToLine) {
         StudioUIUtils.displayMessage(projectKey, "Jumpt to offset " + component.getViewHandler().getOffsetInclassToNavigateTo());
         Navigator.navigateToSource(projectKey, component.getViewHandler().getClassToNavigateTo(), component.getViewHandler().getOffsetInclassToNavigateTo());
      } else {
         Navigator.navigateToSource(projectKey, component.getViewHandler().getClassToNavigateTo());
      }
   }
}
