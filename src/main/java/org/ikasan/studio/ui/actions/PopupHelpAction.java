package org.ikasan.studio.ui.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.ui.Context;
import org.ikasan.studio.build.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentAbstractViewHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PopupHelpAction  implements ActionListener {
   private final String projectKey;
   private final BasicElement component;
   private final MouseEvent mouseEvent;
   private final boolean webHelp;

   public PopupHelpAction(String projectKey, BasicElement component, MouseEvent mouseEvent, boolean webHelp) {
      this.projectKey = projectKey;
      this.component = component;
      this.mouseEvent = mouseEvent;
      this.webHelp = webHelp;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
      final IkasanFlowComponentAbstractViewHandler viewHandler = (IkasanFlowComponentAbstractViewHandler) component.getViewHandler();
      if (webHelp) {
         BrowserUtil.browse(viewHandler.getFlowElement().getComponentMeta().getWebHelpURL());
      } else {
         JTextArea jTextArea = new JTextArea(viewHandler.getFlowElement().getComponentMeta().getHelpText());
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