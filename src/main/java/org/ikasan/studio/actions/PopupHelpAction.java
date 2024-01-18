package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.IkasanElement;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PopupHelpAction  implements ActionListener {
   private String projectKey;
   private IkasanElement component;
   private MouseEvent mouseEvent;
   private boolean webHelp;

   public PopupHelpAction(String projectKey, IkasanElement component, MouseEvent mouseEvent, boolean webHelp) {
      this.projectKey = projectKey;
      this.component = component;
      this.mouseEvent = mouseEvent;
      this.webHelp = webHelp;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      final IkasanFlowComponentViewHandler viewHandler = (IkasanFlowComponentViewHandler) component.getViewHandler();
      if (webHelp) {
         BrowserUtil.browse(viewHandler.getFlowElement().getIkasanComponentMeta().getWebHelpURL());
      } else {
         JTextArea jTextArea = new JTextArea(viewHandler.getFlowElement().getIkasanComponentMeta().getHelpText());
         jTextArea.setLineWrap(true);
         JComponent helpPanel = new JPanel(new BorderLayout());
         helpPanel.add(jTextArea, BorderLayout.CENTER);

         DesignerCanvas designerCanvas = Context.getDesignerCanvas(projectKey);
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
