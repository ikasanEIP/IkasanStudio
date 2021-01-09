package org.ikasan.studio.ui.component;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.actions.ModelRefreshAction;
import org.ikasan.studio.actions.NavigateToCodeAction;
import org.ikasan.studio.actions.PopupHelpAction;
import org.ikasan.studio.actions.SaveAction;
import org.ikasan.studio.model.Ikasan.IkasanFlowElement;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DesignCanvasContextMenu {
    public static Logger log = Logger.getInstance(DesignCanvasContextMenu.class);

    public static void showPopupAndNavigateMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent mouseEvent, IkasanFlowElement flowElement) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createSaveAsMenuItem(projectKey, "Save Image"));
        menu.add(createRefreshMenuItem(projectKey, "Refresh Model"));
        menu.add(createHelpTextItem(projectKey, "Description", flowElement, mouseEvent));
        menu.add(createWebHelpTextItem(projectKey, "Web help", flowElement, mouseEvent));
        menu.add(createNavigateToCode(projectKey, "Jump to code", flowElement, false));
        menu.add(createNavigateToCode(projectKey, "Jump to code line", flowElement, true));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    public static void showPopupMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createSaveAsMenuItem(projectKey, "Save Image"));
        menu.add(createRefreshMenuItem(projectKey, "Refresh Model"));
        menu.show(designerCanvas, event.getX(), event.getY());
    }

    private static JMenuItem createHelpTextItem(String projectKey, String label, IkasanFlowElement flowElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, flowElement, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(String projectKey, String label, IkasanFlowElement flowElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, flowElement, mouseEvent, true));
        return item;
    }

    private static JMenuItem createSaveAsMenuItem(String projectKey, String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new SaveAction(projectKey));
        return item;
    }

    private static JMenuItem createRefreshMenuItem(String projectKey, String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new ModelRefreshAction(projectKey));
        return item;
    }

    private static JMenuItem createNavigateToCode(String projectKey, String label, IkasanFlowElement flowElement, boolean jumpToLine) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new NavigateToCodeAction(projectKey, flowElement, jumpToLine));
        return item;
    }
}

