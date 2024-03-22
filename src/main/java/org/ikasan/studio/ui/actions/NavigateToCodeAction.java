package org.ikasan.studio.ui.actions;

import org.ikasan.studio.Navigator;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigateToCodeAction implements ActionListener {
   private final String projectKey;
   private final BasicElement component;
   boolean jumpToLine;

   public NavigateToCodeAction(String projectKey, BasicElement component, boolean jumpToLine) {
      this.projectKey = projectKey;
      this.component = component;
      this.jumpToLine = jumpToLine;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (StudioUIUtils.getViewHandler(projectKey, component).getOffsetInclassToNavigateTo() != 0 && jumpToLine) {
         StudioUIUtils.displayMessage(projectKey, "Jumpt to offset " + StudioUIUtils.getViewHandler(projectKey, component).getOffsetInclassToNavigateTo());
         Navigator.navigateToSource(projectKey, StudioUIUtils.getViewHandler(projectKey, component).getClassToNavigateTo(), StudioUIUtils.getViewHandler(projectKey, component).getOffsetInclassToNavigateTo());
      } else {
         Navigator.navigateToSource(projectKey, StudioUIUtils.getViewHandler(projectKey, component).getClassToNavigateTo());
      }
   }
}
