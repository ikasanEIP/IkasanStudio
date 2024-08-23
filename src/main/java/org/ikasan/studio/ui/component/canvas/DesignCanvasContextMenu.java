package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Decorator;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DesignCanvasContextMenu {
    public static final Logger LOG = Logger.getInstance("DesignCanvasContextMenu");

    // Enforce as utility clASS
    private DesignCanvasContextMenu () {
    }

    public static void showPopupAndNavigateMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
        JPopupMenu menu = new JPopupMenu();
        if (ikasanBasicElement instanceof FlowElement) {
            menu.add(createDeleteComponentMenuItem(projectKey, "Delete Component", ikasanBasicElement));
            menu.add(createDebugComponentMenuItem(projectKey, "Add Debug to Component", ikasanBasicElement));
            menu.add(createWiretapItem(projectKey, "Wiretap Before", ikasanBasicElement, Decorator.TYPE.Wiretap, Decorator.POSITION.BEFORE));
            menu.add(createWiretapItem(projectKey, "Wiretap After", ikasanBasicElement, Decorator.TYPE.Wiretap, Decorator.POSITION.AFTER));
            menu.add(createWiretapItem(projectKey, "Logging Before", ikasanBasicElement, Decorator.TYPE.LogWiretap, Decorator.POSITION.BEFORE));
            menu.add(createWiretapItem(projectKey, "Logging After", ikasanBasicElement, Decorator.TYPE.LogWiretap, Decorator.POSITION.AFTER));
            menu.add(createHelpTextItem(projectKey, "Describe Component", ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(projectKey, "Component Web help", ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(projectKey, "Jump to code", ikasanBasicElement, false));
        }
        menu.add(createSaveAsMenuItem(projectKey, "Save Image"));
        menu.add(createRefreshMenuItem(projectKey, "Refresh Model"));
        menu.add(createLaunchDashboardMenuItem(projectKey, "Launch Browser"));
        menu.add(createLaunchH2MenuItem(projectKey, "Launch H2"));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    private static JMenuItem createDeleteComponentMenuItem(String projectKey, String label, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DeleteComponentAction(projectKey, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createDebugComponentMenuItem(String projectKey, String label, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DebugComponentAction(projectKey, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createWiretapItem(String projectKey, String label, BasicElement ikasanBasicElement, Decorator.TYPE type, Decorator.POSITION position) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new WiretapComponentAction(projectKey, ikasanBasicElement, type, position));
        return item;
    }
    private static JMenuItem createHelpTextItem(String projectKey, String label, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, ikasanBasicElement, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(String projectKey, String label, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new PopupHelpAction(projectKey, ikasanBasicElement, mouseEvent, true));
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
        item.addActionListener(new LaunchBlueAction(projectKey));
        return item;
    }

    private static JMenuItem createLaunchH2MenuItem(String projectKey, String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new LaunchH2Action(projectKey, item));
        return item;
    }

    private static JMenuItem createNavigateToCode(String projectKey, String label, BasicElement ikasanBasicElement, boolean jumpToLine) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new NavigateToCodeAction(projectKey, ikasanBasicElement, jumpToLine));
        return item;
    }
}

