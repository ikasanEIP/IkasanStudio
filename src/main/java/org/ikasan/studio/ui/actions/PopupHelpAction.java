package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.HtmlScrollingDisplayPanel;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PopupHelpAction  implements ActionListener {
   private final String projectKey;
   private final BasicElement ikasanBasicElement;
   private final MouseEvent mouseEvent;
   private final boolean webHelp;

   public PopupHelpAction(String projectKey, BasicElement ikasanBasicElement, MouseEvent mouseEvent, boolean webHelp) {
      this.projectKey = projectKey;
      this.ikasanBasicElement = ikasanBasicElement;
      this.mouseEvent = mouseEvent;
      this.webHelp = webHelp;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      IkasanFlowComponentViewHandler viewHandler = ViewHandlerCache.getFlowComponentViewHandler(projectKey, ikasanBasicElement);
       if (viewHandler != null) {
         if (webHelp) {
            StudioUIUtils.displayIdeaInfoMessage(projectKey, "Check your default browser, the help information should be automatically populated.");
            BrowserUtil.browse(viewHandler.getFlowElement().getComponentMeta().getWebHelpURL());
         } else {
            DesignerCanvas designerCanvas = UiContext.getDesignerCanvas(projectKey);
            int minWidth = Math.max(designerCanvas.getWidth() > 0 ? designerCanvas.getWidth() / 3 : 200, 200);
            int minHeight = Math.max(designerCanvas.getHeight() > 0 ? designerCanvas.getHeight() / 5 : 200, 200);

            HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel(null, new Dimension(minWidth, minHeight));
            htmlScrollingDisplayPanel.setText(viewHandler.getFlowElement().getComponentMeta().getHelpText());
            JBPopupFactory.getInstance().createComponentPopupBuilder(htmlScrollingDisplayPanel, htmlScrollingDisplayPanel)
                    .setTitle("Description")
                    .setResizable(true)
                    .setMovable(true)
                    .createPopup()
                    .show(new RelativePoint(mouseEvent));
         }
      }
   }
}
