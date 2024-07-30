package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.apache.maven.cli.MavenCli;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopH2Action implements ActionListener {
   private final String projectKey;
   public StopH2Action(String projectKey) {
   this.projectKey = projectKey;
}
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      Module module = UiContext.getIkasanModule(projectKey);
      if (module != null) {
         Project project = UiContext.getProject(projectKey);
         MavenCli cli = new MavenCli();
         int result = cli.doMain(new String[]{"clean"}, project.getBasePath()+"/h2-stop", System.out, System.err);
//         if (result == 0) {
//            Messages.showMessageDialog(project, "Maven target executed successfully", "Information", Messages.getInformationIcon());
//         } else {
//            Messages.showMessageDialog(project, "Failed to execute Maven target", "Error", Messages.getErrorIcon());
//         }
         StudioUIUtils.displayIdeaInfoMessage(projectKey, "Sent request to your browser to launch the H2 console.");
//         BrowserUtil.browse("http:localhost:" + module.getH2WebPortNumber() + "/login.do");
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "H2 console can't be launched unless a module is defined.");

      }
   }
}
