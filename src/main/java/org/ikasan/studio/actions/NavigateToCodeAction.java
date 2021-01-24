package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.StudioUIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigateToCodeAction implements ActionListener {
   private static final Logger log = Logger.getLogger(NavigateToCodeAction.class);
   private String projectKey;
   IkasanFlowComponent flowElement;
   boolean jumpToLine;

   public NavigateToCodeAction(String projectKey, IkasanFlowComponent flowElement, boolean jumpToLine) {
      this.projectKey = projectKey;
      this.flowElement = flowElement;
      this.jumpToLine = jumpToLine;
   }

   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      if (flowElement.getViewHandler().getOffsetInclassToNavigateTo() != 0 && jumpToLine) {
         StudioUIUtils.displayMessage(projectKey, "Jumpt to offset " + flowElement.getViewHandler().getOffsetInclassToNavigateTo());
         Navigator.navigateToSource(projectKey, flowElement.getViewHandler().getClassToNavigateTo(), flowElement.getViewHandler().getOffsetInclassToNavigateTo());
      } else {
         Navigator.navigateToSource(projectKey, flowElement.getViewHandler().getClassToNavigateTo());
      }
   }
}
