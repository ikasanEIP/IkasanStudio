package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DesignCanvasContextMenu {
    public static final Logger LOG = Logger.getInstance("DesignCanvasContextMenu");

    // Enforce as utility clASS
    private DesignCanvasContextMenu () {
    }

    public static void showPopupAndNavigateMenu(String projectKey, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement, Decorator decorator) {
        JPopupMenu menu = new JPopupMenu();
        if (ikasanBasicElement instanceof FlowElement) {

            JMenu moduleNavigation = new JMenu("Module Navigation");

            Module module = UiContext.getIkasanModule(projectKey);
            menusForModuleNavigation(module, moduleNavigation);
            menu.add(moduleNavigation);

            menu.add(createDeleteComponentMenuItem(projectKey, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(projectKey, ikasanBasicElement));
            // @TODO look to optimise this and store in context rather than create each time.
            // @TODO add debug is broken, it needs to perform similar action to dragging a debug component from palette
//            menu.add(createDebugComponentMenuItem(projectKey, ikasanBasicElement));
            menu.addSeparator();
            if (decorator != null && decorator.isBefore() && decorator.isWiretap()) {
                menu.add(removeDecoratorItem(projectKey, "Delete Wiretap Before", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.BEFORE));
            } else {
                menu.add(createDecoratorItem(projectKey, "Add Wiretap Before", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.BEFORE));
            }
            if (decorator != null && decorator.isAfter() && decorator.isWiretap()) {
                menu.add(removeDecoratorItem(projectKey, "Delete Wiretap After", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.AFTER));
            } else {
                menu.add(createDecoratorItem(projectKey, "Add Wiretap After", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.AFTER));
            }
            if (decorator != null && decorator.isBefore() && decorator.isLogWiretap()) {
                menu.add(removeDecoratorItem(projectKey, "Delete Logging Before", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.BEFORE));
            } else {
                menu.add(createDecoratorItem(projectKey, "Add Logging Before", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.BEFORE));
            }
            if (decorator != null && decorator.isAfter() && decorator.isLogWiretap()) {
                menu.add(removeDecoratorItem(projectKey, "Delete Logging After", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.AFTER));
            } else {
                menu.add(createDecoratorItem(projectKey, "Add Logging After", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.AFTER));
            }

            menu.addSeparator();
            menu.add(createHelpTextItem(projectKey, ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(projectKey, ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(projectKey, ikasanBasicElement, false));
            menu.addSeparator();
        }
        menu.add(createSaveAsMenuItem(projectKey));
        menu.add(createRefreshMenuItem(projectKey));
        menu.add(createLaunchDashboardMenuItem(projectKey));
        menu.add(createLaunchH2MenuItem(projectKey));
        menu.add(createDebugMenuItem(projectKey));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }



    private static void menusForModuleNavigation(Module module, JMenu parent) {
//        if (module != null && module.)
//        JMenuItem item = new JMenuItem("Save Image");
//        item.addActionListener(new SaveAction(projectKey));
//        return null;
    }

    private static JMenuItem menusForFlows(FlowElement flowElement) {
//        JMenuItem item = new JMenuItem("Save Image");
//        item.addActionListener(new SaveAction(projectKey));
        return null;
    }

    private static JMenuItem menusForComponent(BasicElement ikasanBasicElement) {
//        JMenuItem item = new JMenuItem("Save Image");
//        item.addActionListener(new SaveAction(projectKey));
        return null;
    }

    private static JMenuItem createDeleteComponentMenuItem(String projectKey, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Delete Component");
        item.addActionListener(new DeleteComponentAction(projectKey, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createDebugComponentMenuItem(String projectKey, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Add Debug to Component");
        item.addActionListener(new DebugComponentAction(projectKey, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createEditComponentMenuItem(String projectKey, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Edit Component");
        item.addActionListener(new EditComponentAction(projectKey, ikasanBasicElement));
        return item;
    }

    private static JMenuItem removeDecoratorItem(String projectKey, String label, BasicElement ikasanBasicElement, DECORATOR_TYPE decoratorType, DECORATOR_POSITION decoratorPosition) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DecoratorComponentAction(projectKey, ikasanBasicElement, false, decoratorType, decoratorPosition));
        return item;
    }
    private static JMenuItem createDecoratorItem(String projectKey, String label, BasicElement ikasanBasicElement, DECORATOR_TYPE decoratorType, DECORATOR_POSITION decoratorPosition) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DecoratorComponentAction(projectKey, ikasanBasicElement, true, decoratorType, decoratorPosition));
        return item;
    }
    private static JMenuItem createHelpTextItem(String projectKey, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem("Describe Component");
        item.addActionListener(new PopupHelpAction(projectKey, ikasanBasicElement, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(String projectKey, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem("Component Web help");
        item.addActionListener(new PopupHelpAction(projectKey, ikasanBasicElement, mouseEvent, true));
        return item;
    }

    private static JMenuItem createSaveAsMenuItem(String projectKey) {
        JMenuItem item = new JMenuItem("Save Image");
        item.addActionListener(new SaveAction(projectKey));
        return item;
    }

    private static JMenuItem createRefreshMenuItem(String projectKey) {
        JMenuItem item = new JMenuItem("Refresh");
        item.addActionListener(new ModelRefreshAction(projectKey));
        return item;
    }

    private static JMenuItem createLaunchDashboardMenuItem(String projectKey) {
        JMenuItem item = new JMenuItem("Launch Browser");
        item.addActionListener(new LaunchBlueAction(projectKey));
        return item;
    }

    private static JMenuItem createLaunchH2MenuItem(String projectKey) {
        JMenuItem item = new JMenuItem("Launch H2");
        item.addActionListener(new LaunchH2Action(projectKey, item));
        return item;
    }

    private static JMenuItem createDebugMenuItem(String projectKey) {
        JMenuItem item = new JMenuItem("Debug Module to logs");
        item.addActionListener(new DebugAction(projectKey));
        return item;
    }

    private static JMenuItem createNavigateToCode(String projectKey, BasicElement ikasanBasicElement, boolean jumpToLine) {
        JMenuItem item = new JMenuItem("Jump to code");
        item.addActionListener(new NavigateToCodeAction(projectKey, ikasanBasicElement, jumpToLine));
        return item;
    }
}

