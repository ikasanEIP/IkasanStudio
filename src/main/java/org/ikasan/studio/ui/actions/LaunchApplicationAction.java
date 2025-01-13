package org.ikasan.studio.ui.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchApplicationAction implements ActionListener {
   private static final Logger LOG = LoggerFactory.getLogger(LaunchApplicationAction.class);
   private final String projectKey;
   private final JComponent jComponent;
   private boolean start=true;
   public LaunchApplicationAction(String projectKey, JComponent jComponent) {
      this.projectKey = projectKey;
      this.jComponent = jComponent;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);
      module.getH2PortNumber();
      module.getH2WebPortNumber();
      String applicationRelativePath = "generated/src/main/java/org/ikasan/studio/boot/Application.java";

      if (start) {
         VirtualFile applicationVF = StudioPsiUtils.getVirtualFile(projectKey, applicationRelativePath);
         if (applicationVF != null) {
            StudioPsiUtils.runClassFromEditor(projectKey, applicationVF, "org.ikasan.studio.boot.Application");
         } else {
            StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were experienced getting the virtual application, launch is unavailable at this time.");
            LOG.warn("STUDIO: SERIOUS: LaunchApplicationAction could not find virtual file for application for project [" + projectKey +
                    "] and applicationRelativePath [" + applicationRelativePath + "]");
         }
      } else {
         StudioPsiUtils.stopRunningProcess(projectKey);
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "The module has been stopped.");
      }
      start = !start;
      toggleButtonText();
   }

   private void toggleButtonText() {
      String action = start ? "Module start" : "Module stop";
      Color backgroundColor = start ? UIManager.getColor("Panel.background") : Color.GREEN ;
      if (jComponent instanceof JButton) {
         ((JButton) jComponent).setText(action);
         jComponent.setBackground(backgroundColor);
         jComponent.setOpaque(!start);
      } else if (jComponent instanceof JMenuItem) {
         ((JMenuItem)jComponent).setText(action);
      }
   }
}
