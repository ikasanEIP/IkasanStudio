package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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

    public static void showPopupAndNavigateMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement, Decorator decorator) {
        JPopupMenu menu = new JPopupMenu();
        if (ikasanBasicElement instanceof FlowElement) {

            JMenu moduleNavigation = new JMenu("Module Navigation");
            UiContext uiContext = project.getService(UiContext.class);
            Module module = uiContext.getIkasanModule();
//            menusForModuleNavigation(module, moduleNavigation);
            menu.add(moduleNavigation);

            menu.add(createDeleteComponentMenuItem(project, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(project, ikasanBasicElement));
            // @TODO look to optimise this and store in context rather than create each time.
            // @TODO add debug is broken, it needs to perform similar action to dragging a debug component from palette
//            menu.add(createDebugComponentMenuItem(project, ikasanBasicElement));
            menu.addSeparator();
            if (decorator != null && decorator.isBefore() && decorator.isWiretap()) {
                menu.add(removeDecoratorItem(project, "Delete Wiretap Before", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.BEFORE));
            } else {
                menu.add(createDecoratorItem(project, "Add Wiretap Before", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.BEFORE));
            }
            if (decorator != null && decorator.isAfter() && decorator.isWiretap()) {
                menu.add(removeDecoratorItem(project, "Delete Wiretap After", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.AFTER));
            } else {
                menu.add(createDecoratorItem(project, "Add Wiretap After", ikasanBasicElement, DECORATOR_TYPE.Wiretap, DECORATOR_POSITION.AFTER));
            }
            if (decorator != null && decorator.isBefore() && decorator.isLogWiretap()) {
                menu.add(removeDecoratorItem(project, "Delete Logging Before", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.BEFORE));
            } else {
                menu.add(createDecoratorItem(project, "Add Logging Before", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.BEFORE));
            }
            if (decorator != null && decorator.isAfter() && decorator.isLogWiretap()) {
                menu.add(removeDecoratorItem(project, "Delete Logging After", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.AFTER));
            } else {
                menu.add(createDecoratorItem(project, "Add Logging After", ikasanBasicElement, DECORATOR_TYPE.LogWiretap, DECORATOR_POSITION.AFTER));
            }

            menu.addSeparator();
            menu.add(createHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(project, ikasanBasicElement, false));
            menu.addSeparator();
        }
        menu.add(createSaveAsMenuItem(project));
        menu.add(createLoadMenuItem(project));
        menu.add(createLaunchDashboardMenuItem(project));
        menu.add(createLaunchH2MenuItem(project));
        menu.add(createDebugMenuItem(project));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }



    private static void menusForModuleNavigation(Module module, JMenu parent) {
//        if (module != null && module.)
//        JMenuItem item = new JMenuItem("Save Image");
//        item.addActionListener(new SaveAction(project));
//        return null;
    }

    private static JMenuItem createDeleteComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Delete Component");
        item.addActionListener(new DeleteComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createDebugComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Add Debug to Component");
        item.addActionListener(new DebugComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createEditComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem("Edit Component");
        item.addActionListener(new EditComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem removeDecoratorItem(Project project, String label, BasicElement ikasanBasicElement, DECORATOR_TYPE decoratorType, DECORATOR_POSITION decoratorPosition) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DecoratorComponentAction(project, ikasanBasicElement, false, decoratorType, decoratorPosition));
        return item;
    }
    private static JMenuItem createDecoratorItem(Project project, String label, BasicElement ikasanBasicElement, DECORATOR_TYPE decoratorType, DECORATOR_POSITION decoratorPosition) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new DecoratorComponentAction(project, ikasanBasicElement, true, decoratorType, decoratorPosition));
        return item;
    }
    private static JMenuItem createHelpTextItem(Project project, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem("Describe Component");
        item.addActionListener(new PopupHelpAction(project, ikasanBasicElement, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(Project project, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem("Component Web help");
        item.addActionListener(new PopupHelpAction(project, ikasanBasicElement, mouseEvent, true));
        return item;
    }

    private static JMenuItem createSaveAsMenuItem(Project project) {
        JMenuItem item = new JMenuItem("Save Image");
        item.addActionListener(new SaveAction(project));
        return item;
    }

    private static JMenuItem createLoadMenuItem(Project project) {
        JMenuItem item = new JMenuItem("Load");
        item.addActionListener(new ModelLoadAction(project));
        return item;
    }

    private static JMenuItem createLaunchDashboardMenuItem(Project project) {
        JMenuItem item = new JMenuItem("Launch Browser");
        item.addActionListener(new LaunchBlueAction(project));
        return item;
    }

    private static JMenuItem createLaunchH2MenuItem(Project project) {
        JMenuItem item = new JMenuItem("Launch H2");
        item.addActionListener(new LaunchH2Action(project, item));
        return item;
    }

    private static JMenuItem createDebugMenuItem(Project project) {
        JMenuItem item = new JMenuItem("Debug Module to logs");
        item.addActionListener(new DebugAction(project));
        return item;
    }

    private static JMenuItem createNavigateToCode(Project project, BasicElement ikasanBasicElement, boolean jumpToLine) {
        JMenuItem item = new JMenuItem("Jump to code");
        item.addActionListener(new NavigateToCodeAction(project, ikasanBasicElement, jumpToLine));
        return item;
    }
}

