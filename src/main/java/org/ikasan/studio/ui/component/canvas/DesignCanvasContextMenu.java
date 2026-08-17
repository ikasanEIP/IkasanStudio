package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.actions.*;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DesignCanvasContextMenu {
    public static final Logger LOG = Logger.getInstance("DesignCanvasContextMenu");

    // Enforce as utility clASS
    private DesignCanvasContextMenu () {
    }

    public static void showPopupAndNavigateMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
        JPopupMenu menu = new JPopupMenu();

        if (ikasanBasicElement instanceof Flow) {
            menu.add(createDeleteComponentMenuItem(project, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(project, ikasanBasicElement));
            menu.addSeparator();
            menu.add(createHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(project, ikasanBasicElement, false));
            menu.addSeparator();
        } else if (ikasanBasicElement instanceof FlowElement flowElement) {
            menu.add(createDeleteComponentMenuItem(project, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(project, ikasanBasicElement));
            if (!flowElement.getComponentMeta().isProducer() && !flowElement.getComponentMeta().isDebug()) {
                menu.add(createDebugComponentMenuItem(project, ikasanBasicElement));
            }
            if (flowElement.getComponentMeta().isConsumer()
                    && project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
                menu.add(createSendTestMessageMenuItem(project, ikasanBasicElement));
            }
            menu.addSeparator();
            addDecoratorMenuItem(menu, project, flowElement, DECORATOR_TYPE.Wiretap,
                    DECORATOR_POSITION.BEFORE, "menu.AddWiretapBefore", "menu.DeleteWiretapBefore");
            addDecoratorMenuItem(menu, project, flowElement, DECORATOR_TYPE.Wiretap,
                    DECORATOR_POSITION.AFTER, "menu.AddWiretapAfter", "menu.DeleteWiretapAfter");
            addDecoratorMenuItem(menu, project, flowElement, DECORATOR_TYPE.LogWiretap,
                    DECORATOR_POSITION.BEFORE, "menu.AddLoggingBefore", "menu.DeleteLoggingBefore");
            addDecoratorMenuItem(menu, project, flowElement, DECORATOR_TYPE.LogWiretap,
                    DECORATOR_POSITION.AFTER, "menu.AddLoggingAfter", "menu.DeleteLoggingAfter");

            menu.addSeparator();
            menu.add(createHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(project, ikasanBasicElement, true));
            menu.addSeparator();
        }
        menu.add(createSaveAsMenuItem(project));
        menu.add(createLoadMenuItem(project));
        menu.add(createLaunchDashboardMenuItem(project));
        menu.add(createLaunchH2MenuItem(project));
        menu.add(createDebugMenuItem(project));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    private static JMenuItem createDeleteComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.DeleteComponent"));
        item.addActionListener(new DeleteComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createSendTestMessageMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.SendTestMessage"));
        item.addActionListener(new SendTestMessageAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createDebugComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.AddDebugToComponent"));
        item.addActionListener(new DebugComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createEditComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.EditComponent"));
        item.addActionListener(new EditComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static void addDecoratorMenuItem(JPopupMenu menu, Project project, FlowElement flowElement,
                                             DECORATOR_TYPE type, DECORATOR_POSITION position,
                                             String addLabelKey, String deleteLabelKey) {
        boolean present = flowElement.getDecorators() != null && flowElement.getDecorators().stream()
                .anyMatch(existing -> type.equals(existing.getType()) && position.equals(existing.getPosition()));
        menu.add(present
                ? removeDecoratorItem(project, StudioBundle.message(deleteLabelKey), flowElement, type, position)
                : createDecoratorItem(project, StudioBundle.message(addLabelKey), flowElement, type, position));
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
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.DescribeComponent"));
        item.addActionListener(new PopupHelpAction(project, ikasanBasicElement, mouseEvent, false));
        return item;
    }

    private static JMenuItem createWebHelpTextItem(Project project, BasicElement ikasanBasicElement, MouseEvent mouseEvent) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ComponentWebHelp"));
        item.addActionListener(new PopupHelpAction(project, ikasanBasicElement, mouseEvent, true));
        return item;
    }

    private static JMenuItem createSaveAsMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.SaveImage"));
        item.addActionListener(new SaveAction(project));
        return item;
    }

    private static JMenuItem createLoadMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("label.Load"));
        item.addActionListener(new ModelLoadAction(project));
        return item;
    }

    private static JMenuItem createLaunchDashboardMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("label.Console"));
        item.addActionListener(new LaunchBlueAction(project));
        return item;
    }

    private static JMenuItem createLaunchH2MenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.LaunchH2"));
        item.addActionListener(new LaunchH2Action(project, item));
        return item;
    }

    private static JMenuItem createDebugMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.DebugModuleToLogs"));
        item.addActionListener(new DebugAction(project));
        return item;
    }

    private static JMenuItem createNavigateToCode(Project project, BasicElement ikasanBasicElement, boolean jumpToLine) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.JumpToCode"));
        item.addActionListener(new NavigateToCodeAction(project, ikasanBasicElement, jumpToLine));
        return item;
    }
}
