package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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

      if (module != null) {
         Map<String, String> applicationProperties = uiContext.getApplicationProperties();
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
               executeCommandInTerminal(project, path, (start ? "H2 start" : "H2 stop"), command);
               StudioUIUtils.displayIdeaInfoMessage(project,
                       (start ? "Starting H2 server, console should appear in your browser, for JDBC URL " + connectionString + " username and password is sa, review H2 Terminal tabs for output"
                               : "Stopping H2 server, review H2 Terminal tabs for output"));
            } catch (IOException e) {
               StudioUIUtils.displayIdeaWarnMessage(project, "Attempt to run command failed due to IOException: " + e.getMessage());
            }
         }

         start = !start;
         toggleButtonText();
      } else {
         StudioUIUtils.displayIdeaWarnMessage(project, "H2 console can't be launched unless a module is defined.");
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
      ShellTerminalWidget terminalWidget = null;

      // Check if the content already exists and extract its terminal widget
      if (h2ContentTab != null) {
         // Get the component from the content and check if it's a JBTerminalWidget
         JBTerminalWidget widget = getTerminalWidgetFromContent(h2ContentTab);
         if (widget instanceof ShellTerminalWidget) {
            terminalWidget = (ShellTerminalWidget) widget;
         }
      }

      // Create a new terminal if one doesn't exist
      if (terminalWidget == null) {
         // Use modern API: create widget directly without getInstance
         JBTerminalWidget newWidget = createTerminalWidget(project, path, title);
         if (newWidget instanceof ShellTerminalWidget) {
            terminalWidget = (ShellTerminalWidget) newWidget;
         }
      } else {
         // Select the existing content tab
         contentManager.setSelectedContent(h2ContentTab);
      }

      // Execute the command if we have a valid terminal widget
      if (terminalWidget != null) {
         terminalWidget.executeCommand(command);
      } else {
         LOG.warn("STUDIO: WARN: Could not create or find terminal widget for H2 command execution");
      }
   }

   /**
    * Extract the JBTerminalWidget from a Content object's component.
    * Modern replacement for the deprecated TerminalView.getWidgetByContent.
    *
    * @param content The content object from the terminal tool window
    * @return The JBTerminalWidget if found, null otherwise
    */
   private JBTerminalWidget getTerminalWidgetFromContent(Content content) {
      if (content == null) {
         return null;
      }

      // The component in the content is the terminal widget
      java.awt.Component component = content.getComponent();
      if (component instanceof JBTerminalWidget) {
         return (JBTerminalWidget) component;
      }

      return null;
   }

   /**
    * Create a new terminal widget using the modern TerminalView API.
    * Uses the project service locator to get TerminalView instance.
    *
    * @param project The IntelliJ project
    * @param workingDirectory The working directory for the terminal
    * @param displayName The display name for the terminal tab
    * @return A new JBTerminalWidget instance, or null if creation fails
    */
   private JBTerminalWidget createTerminalWidget(Project project, String workingDirectory, String displayName) {
      try {
         // Modern approach: Use project service locator instead of deprecated getInstance
         // This is the supported way in IntelliJ 2025+ for accessing project-scoped services
         TerminalView terminalView = project.getService(TerminalView.class);
         if (terminalView != null) {
            return terminalView.createLocalShellWidget(workingDirectory, displayName);
         } else {
            LOG.warn("STUDIO: WARN: TerminalView service not available");
            return null;
         }
      } catch (Exception e) {
         LOG.warn("STUDIO: WARN: Failed to create terminal widget: " + e.getMessage(), e);
         return null;
      }
   }
}
