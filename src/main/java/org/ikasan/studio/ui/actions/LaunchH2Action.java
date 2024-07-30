package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

public class LaunchH2Action implements ActionListener {
   private final String projectKey;
   private final JComponent jComponent;
   private boolean start=true;
   public LaunchH2Action(String projectKey, JComponent jComponent) {
      this.projectKey = projectKey;
      this.jComponent = jComponent;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {

      //  mvn exec:java@StartH2 -Dh2DbPortNumber=12461  -Dh2WebPortNumber=12462
      // mvn exec:java@StopH2 -Dh2DbPortNumber=12461
      Module module = UiContext.getIkasanModule(projectKey);
      module.getH2PortNumber();
      module.getH2WebPortNumber();

      if (module != null) {
         Map<String, String> applicationProperties = UiContext.getApplicationProperties(projectKey);
         String connectionString ;
         if (applicationProperties != null && applicationProperties.get("datasource.url") != null) {
            String connnectionString = (String) applicationProperties.get("datasource.url");
            if (connnectionString != null && connnectionString.contains(";")) {
               connnectionString = connnectionString.split(";")[0];
            }
            connectionString = "use ["+ connnectionString + "],";
         } else {
            connectionString = "see 'datasource.url in you application.properties file,";
         }
         Project project = UiContext.getProject(projectKey);
         String command = start ?
           "mvn exec:java@StartH2 -Dh2DbPortNumber=" + module.getH2PortNumber() + " -Dh2WebPortNumber=" + module.getH2WebPortNumber() :
           "mvn exec:java@StopH2 -Dh2DbPortNumber=" + module.getH2PortNumber();
         String path = project.getBasePath() + "/generated/h2";
         try {
            executeCommandInTerminal(project, path, (start ? "H2 start" : "H2 stop"), command);
            StudioUIUtils.displayIdeaInfoMessage(projectKey,
                 (start ? "Starting H2 server, console should appear in your browser, for JDBC URL " + connectionString + " username and password is sa, review H2 Terminal tabs for output"
                        : "Stopping H2 server, review H2 Terminal tabs for output"));
         } catch (IOException e) {
            StudioUIUtils.displayIdeaWarnMessage(projectKey, "Attempt to run command failed due to IOException: " + e.getMessage());
         }
         start = !start;
         toggleButtonText();
      } else {
         StudioUIUtils.displayIdeaWarnMessage(projectKey, "H2 console can't be launched unless a module is defined.");
      }
   }

   private void toggleButtonText() {
      String action = start ? "H2 start" : "H2 stop";
      Color backgroundColor = start ? UIManager.getColor("Panel.background") : Color.GREEN ;
      if (jComponent instanceof JButton) {
         ((JButton) jComponent).setText(action);
         jComponent.setBackground(backgroundColor);
         jComponent.setOpaque(!start);
      } else if (jComponent instanceof JMenuItem) {
         ((JMenuItem)jComponent).setText(action);
      }
   }

   private void executeCommandInTerminal(Project project, String path, String title, String command) throws IOException {
      ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
      if (window == null) return;

      ContentManager contentManager = window.getContentManager();
      Content h2ContentTab = contentManager.findContent(title);
      JBTerminalWidget terminalWidget = null;
      if (h2ContentTab != null) {
         terminalWidget = TerminalView.getWidgetByContent(h2ContentTab);
      }

      if (!(terminalWidget instanceof ShellTerminalWidget)) {
         TerminalView terminalView = TerminalView.getInstance(project);
         terminalWidget = terminalView.createLocalShellWidget(path, title);
      } else {
         contentManager.setSelectedContent(h2ContentTab);
      }
      ((ShellTerminalWidget) terminalWidget).executeCommand(command);
   }
}
