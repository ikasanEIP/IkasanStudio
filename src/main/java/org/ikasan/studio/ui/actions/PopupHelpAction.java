package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import javax.swing.*;
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
      IkasanFlowComponentViewHandler viewHandler = ViewHandlerFactoryIntellij.getOrCreateFlowComponentViewHandler(projectKey, ikasanBasicElement);
       if (viewHandler != null) {
         if (webHelp) {
            BrowserUtil.browse(viewHandler.getFlowElement().getComponentMeta().getWebHelpURL());
         } else {
            JTextArea jTextArea = new JTextArea(viewHandler.getFlowElement().getComponentMeta().getHelpText());
            jTextArea.setLineWrap(true);
            JComponent helpPanel = new JPanel(new BorderLayout());
            helpPanel.add(jTextArea, BorderLayout.CENTER);

            DesignerCanvas designerCanvas = UiContext.getDesignerCanvas(projectKey);
            int minWidth = Math.max(designerCanvas.getWidth() > 0 ? designerCanvas.getWidth() / 3 : 200, 200);
            int minHeight = Math.max(designerCanvas.getHeight() > 0 ? designerCanvas.getHeight() / 5 : 200, 200);

            JBPopupFactory.getInstance().createComponentPopupBuilder(new JScrollPane(helpPanel), jTextArea)
                    .setTitle("Generator Description")
                    .setResizable(true)
                    .setMovable(true)
                    .setMinSize(new Dimension(minWidth, minHeight))
                    .createPopup()
                    .show(new RelativePoint(mouseEvent));
         }
      }
   }
}
