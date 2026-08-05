package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.ui.TerminalWidget;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class LaunchH2Action implements ActionListener {
   private static final Logger LOG = LoggerFactory.getLogger(LaunchH2Action.class);
   private final Project project;
   private final JComponent jComponent;
   private boolean start=true;
   public LaunchH2Action(Project project, JComponent jComponent) {
      this.project = project;
      this.jComponent = jComponent;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {

      //  mvn exec:java@StartH2 -Dh2DbPortNumber=12461  -Dh2WebPortNumber=12462
      // mvn exec:java@StopH2 -Dh2DbPortNumber=12461
      UiContext uiContext = project.getService(UiContext.class);
      Module module = uiContext.getIkasanModule();
      module.getH2PortNumber();
      module.getH2WebPortNumber();

      Map<String, String> applicationProperties = uiContext.getApplicationProperties();
      String connectionString ;
      if (applicationProperties != null && applicationProperties.get("datasource.url") != null) {
         String connnectionString = applicationProperties.get("datasource.url");
         if (connnectionString != null && connnectionString.contains(";")) {
            connnectionString = connnectionString.split(";")[0];
         }
         connectionString = StudioBundle.message("message.UseConnectionString", connnectionString);
      } else {
         connectionString = StudioBundle.message("message.SeeDatasourceUrlInApplicationProperties");
      }

      VirtualFile virtualProjectRoot = StudioPsiUtils.getProjectBaseDir(project);
      if (virtualProjectRoot == null) {
         LOG.warn("STUDIO: WARN: Could not get virtual project root for project [" + project + "], consider resaving");
      } else {
         String basePath = virtualProjectRoot.getPath();
         String command =
              (start ?
                 "mvn exec:java@StartH2 -Dh2DbPortNumber=" + module.getH2PortNumber() + " -Dh2WebPortNumber=" + module.getH2WebPortNumber() :
                 "mvn exec:java@StopH2 -Dh2DbPortNumber=" + module.getH2PortNumber());
         String path = basePath + "/generated/h2";
         try {
            executeCommandInTerminal(project, path, (start ? StudioBundle.message("button.H2Start") : StudioBundle.message("button.H2Stop")), command);
            StudioUIUtils.displayIdeaInfoMessage(project,
                    (start ? StudioBundle.message("message.StartingH2ServerConsoleShouldAppear", connectionString)
                            : StudioBundle.message("message.StoppingH2ServerReviewH2TerminalTabs")));
         } catch (RuntimeException e) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.AttemptToRunCommandFailed", e.getMessage()));
         }
      }

      start = !start;
      toggleButtonText();
   }

   private void toggleButtonText() {
      String action = start ? StudioBundle.message("button.H2Start") : StudioBundle.message("button.H2Stop");
      Color backgroundColor = start ? UIManager.getColor("Panel.background") : JBColor.GREEN;
      if (jComponent instanceof JButton) {
         ((JButton) jComponent).setText(action);
         jComponent.setBackground(backgroundColor);
         jComponent.setOpaque(!start);
      } else if (jComponent instanceof JMenuItem) {
         ((JMenuItem)jComponent).setText(action);
      }
   }

   private void executeCommandInTerminal(Project project, String path, String title, String command) {
      ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
      if (window == null) return;

      ContentManager contentManager = window.getContentManager();
      Content h2ContentTab = contentManager.findContent(title);
      TerminalWidget terminalWidget;

      // Check if the content already exists and reuse its terminal widget
      if (h2ContentTab != null) {
         terminalWidget = TerminalToolWindowManager.findWidgetByContent(h2ContentTab);
         contentManager.setSelectedContent(h2ContentTab);
      } else {
         terminalWidget = createTerminalWidget(project, path, title);
      }

      // Execute the command if we have a valid terminal widget
      if (terminalWidget != null) {
         terminalWidget.sendCommandToExecute(command);
      } else {
         LOG.warn("STUDIO: WARN: Could not create or find terminal widget for H2 command execution");
      }
   }

   /**
    * Create a new terminal widget via TerminalToolWindowManager, the non-deprecated replacement for
    * TerminalView#createLocalShellWidget (scheduled for removal).
    *
    * @param project The IntelliJ project
    * @param workingDirectory The working directory for the terminal
    * @param displayName The display name for the terminal tab
    * @return A new TerminalWidget instance, or null if creation fails
    */
   private TerminalWidget createTerminalWidget(Project project, String workingDirectory, String displayName) {
      try {
         return TerminalToolWindowManager.getInstance(project).createShellWidget(workingDirectory, displayName, true, true);
      } catch (Exception e) {
         LOG.warn("STUDIO: WARN: Failed to create terminal widget: " + e.getMessage(), e);
         return null;
      }
   }
}
