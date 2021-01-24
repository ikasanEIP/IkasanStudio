package org.ikasan.studio.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.component.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.IkasanFlowElementViewHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PopupHelpAction  implements ActionListener {
   private String projectKey;
   private IkasanFlowComponent flowElement;
   private MouseEvent mouseEvent;
   private boolean webHelp;

   public PopupHelpAction(String projectKey, IkasanFlowComponent flowElement, MouseEvent mouseEvent, boolean webHelp) {
      this.projectKey = projectKey;
      this.flowElement = flowElement;
      this.mouseEvent = mouseEvent;
      this.webHelp = webHelp;
   }
   @Override
   public void actionPerformed(ActionEvent actionEvent) {
//      new SampleDialogWrapper(projectKey, flowElement).showAndGet();

      final IkasanFlowElementViewHandler viewHandler = (IkasanFlowElementViewHandler) flowElement.getViewHandler();
      if (webHelp) {
         BrowserUtil.browse(viewHandler.getIkasanFlowUIComponent().getWebHelpURL());
      } else {
         JTextArea jTextArea = new JTextArea(viewHandler.getIkasanFlowUIComponent().getHelpText());
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
