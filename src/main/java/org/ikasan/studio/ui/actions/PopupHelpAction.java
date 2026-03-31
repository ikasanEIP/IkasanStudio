package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.HtmlScrollingDisplayPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PopupHelpAction  implements ActionListener {
   private final Project project;
   private final BasicElement ikasanBasicElement;
   private final MouseEvent mouseEvent;
   private final boolean webHelp;

   private static final Logger LOG = Logger.getInstance("#PopupHelpAction");

   public PopupHelpAction(Project project, BasicElement ikasanBasicElement, MouseEvent mouseEvent, boolean webHelp) {
      this.project = project;
      this.ikasanBasicElement = ikasanBasicElement;
      this.mouseEvent = mouseEvent;
      this.webHelp = webHelp;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
       String webURL = "";
       String helpText = "";
       if (ikasanBasicElement instanceof FlowElement) {
          webURL = ((FlowElement)ikasanBasicElement).getComponentMeta().getWebHelpURL();
          helpText = ((FlowElement)ikasanBasicElement).getComponentMeta().getHelpText();
       } else if (ikasanBasicElement instanceof Flow) {
          webURL = ((Flow)ikasanBasicElement).getComponentMeta().getWebHelpURL();
          helpText = ((Flow)ikasanBasicElement).getComponentMeta().getHelpText();
       }

      if (webHelp && !webURL.isEmpty()) {
         StudioUIUtils.displayIdeaInfoMessage(project, "Check your default browser, the help information should be automatically populated.");
         BrowserUtil.browse(webURL);
      } else if (!helpText.isEmpty()) {
         UiContext uiContext = project.getService(UiContext.class);
         DesignerCanvas designerCanvas = uiContext.getDesignerCanvas();
         int minWidth = Math.max(designerCanvas.getWidth() > 0 ? designerCanvas.getWidth() / 3 : 200, 200);
         int minHeight = Math.max(designerCanvas.getHeight() > 0 ? designerCanvas.getHeight() / 5 : 200, 200);

         HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel(null, new Dimension(minWidth, minHeight));
         htmlScrollingDisplayPanel.setText(helpText);
         JBPopupFactory.getInstance().createComponentPopupBuilder(htmlScrollingDisplayPanel, htmlScrollingDisplayPanel)
                 .setTitle("Description")
                 .setResizable(true)
                 .setMovable(true)
                 .createPopup()
                 .show(new RelativePoint(mouseEvent));
      }
   }
}
