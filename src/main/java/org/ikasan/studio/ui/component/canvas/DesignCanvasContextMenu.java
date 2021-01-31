package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.actions.*;
import org.ikasan.studio.model.Ikasan.IkasanComponent;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DesignCanvasContextMenu {
    public static Logger log = Logger.getInstance(DesignCanvasContextMenu.class);

    public static void showPopupAndNavigateMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent mouseEvent, IkasanComponent component) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createSaveAsMenuItem(projectKey, "Save Image"));
        menu.add(createRefreshMenuItem(projectKey, "Refresh Model"));
        menu.add(createLaunchDashboardMenuItem(projectKey, "Launch Browser"));
        menu.add(createHelpTextItem(projectKey, "Description", component, mouseEvent));
        menu.add(createWebHelpTextItem(projectKey, "Web help", component, mouseEvent));
        menu.add(createNavigateToCode(projectKey, "Jump to code", component, false));
        menu.add(createNavigateToCode(projectKey, "Jump to code line", component, true));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    public static void showPopupMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createSaveAsMenuItem(projectKey, "Save Image"));
        menu.add(createRefreshMenuItem(projectKey, "Refresh Model"));
        menu.add(createLaunchDashboardMenuItem(projectKey, "Launch Browser"));
        menu.show(designerCanvas, event.getX(), event.getY());
    }

    private static JMenuItem createHelpTextItem(String projectKey, String label, IkasanComponent component, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, component, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(String projectKey, String label, IkasanComponent component, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, component, mouseEvent, true));
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

    private static JMenuItem createLaunchDashboardMenuItem(String projectKey, String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new LaunchDashboardAction(projectKey));
        return item;
    }

    private static JMenuItem createNavigateToCode(String projectKey, String label, IkasanComponent component, boolean jumpToLine) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new NavigateToCodeAction(projectKey, component, jumpToLine));
        return item;
    }
}

