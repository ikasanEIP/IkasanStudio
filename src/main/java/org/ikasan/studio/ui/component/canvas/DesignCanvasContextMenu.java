package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowRouteViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class DesignCanvasContextMenu {
    public static final Logger LOG = Logger.getInstance("DesignCanvasContextMenu");

    // Enforce as utility clASS
    private DesignCanvasContextMenu () {
    }

    public static void showPopupAndNavigateMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
        JPopupMenu menu = new JPopupMenu();

        if (ikasanBasicElement instanceof Flow flow) {
            menu.add(createDeleteComponentMenuItem(project, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(project, ikasanBasicElement));
            menu.addSeparator();
            addMoveFlowMenuItemsIfApplicable(menu, project, flow);
            menu.add(createHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createWebHelpTextItem(project, ikasanBasicElement, mouseEvent));
            menu.add(createNavigateToCode(project, ikasanBasicElement, false));
            addNavigateToPropertiesMenuItemIfAvailable(menu, project, ikasanBasicElement);
            menu.addSeparator();
        } else if (ikasanBasicElement instanceof FlowElement flowElement) {
            menu.add(createDeleteComponentMenuItem(project, ikasanBasicElement));
            menu.add(createEditComponentMenuItem(project, ikasanBasicElement));
            if (!flowElement.getComponentMeta().isProducer() && !flowElement.getComponentMeta().isDebug()) {
                menu.add(createDebugComponentMenuItem(project, ikasanBasicElement));
            }
            if (flowElement.getComponentMeta().supportsSendTestMessage()
                    && project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
                menu.add(IkasanFlowRouteViewHandler.usesTriggerBadge(flowElement)
                        ? createTriggerScheduledConsumerMenuItem(project, ikasanBasicElement)
                        : createSendTestMessageMenuItem(project, ikasanBasicElement));
            }
            if (flowElement.getComponentMeta().supportsTestMailServer()) {
                menu.addSeparator();
                menu.add(createStartTestMailServerMenuItem(project, ikasanBasicElement));
                menu.add(createStopTestMailServerMenuItem(project, ikasanBasicElement));
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
            addNavigateToPropertiesMenuItemIfAvailable(menu, project, ikasanBasicElement);
            menu.addSeparator();
        }
        menu.add(createSaveAsMenuItem(project));
        menu.add(createLoadMenuItem(project));
        menu.add(createLaunchDashboardMenuItem(project));
        menu.add(createLaunchH2MenuItem(project));
        menu.add(createDebugMenuItem(project));
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    /**
     * Minimal popup for a right-click directly on the shared "Test Mail Server" canvas node (see
     * {@code DesignerCanvas#paintTestMailServerNode}) - deliberately just the one Stop item, not the full
     * producer menu {@link #showPopupAndNavigateMenu} would show for {@code ikasanBasicElement} itself (Delete
     * Component, Edit Component, etc. would act on the underlying Email Producer, which isn't what's visually
     * being pointed at here). No Start item either: this node is only ever painted while its address is
     * actually listening, so Stop is the only action that ever makes sense from it.
     */
    public static void showStopTestMailServerMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createStopTestMailServerMenuItem(project, ikasanBasicElement));
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

    private static JMenuItem createTriggerScheduledConsumerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.TriggerScheduledConsumer"));
        item.addActionListener(new TriggerScheduledConsumerAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createStartTestMailServerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StartTestMailServer"));
        item.addActionListener(new StartTestMailServerAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createStopTestMailServerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StopTestMailServer"));
        item.addActionListener(new StopTestMailServerAction(project, ikasanBasicElement));
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

    /**
     * "Move Flow Up"/"Move Flow Down" - lets the user manually order flows exactly as they like (e.g. to sit a
     * JMS-connected pair adjacent to one another), rather than any automatic heuristic guessing on their
     * behalf. Only added when applicable: the first flow can only move down, the last only up.
     */
    private static void addMoveFlowMenuItemsIfApplicable(JPopupMenu menu, Project project, Flow flow) {
        Module ikasanModule = project.getService(UiContext.class).getIkasanModule();
        if (ikasanModule == null || ikasanModule.getFlows() == null) {
            return;
        }
        List<Flow> flows = ikasanModule.getFlows();
        int index = flows.indexOf(flow);
        if (index < 0) {
            return;
        }
        if (index > 0) {
            JMenuItem moveUpItem = new JMenuItem(StudioBundle.message("menu.MoveFlowUp"));
            moveUpItem.addActionListener(new MoveFlowAction(project, flow, true));
            menu.add(moveUpItem);
        }
        if (index < flows.size() - 1) {
            JMenuItem moveDownItem = new JMenuItem(StudioBundle.message("menu.MoveFlowDown"));
            moveDownItem.addActionListener(new MoveFlowAction(project, flow, false));
            menu.add(moveDownItem);
        }
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

    /**
     * Unlike "Jump to code", not every component has properties externalized into application.properties (only
     * ones with a propertyConfigFileLabel and a value ever appear there), so this item is only added when a
     * target was actually found for this component - see {@link AbstractViewHandlerIntellij#hasPropertiesNavigationTarget()}.
     */
    private static void addNavigateToPropertiesMenuItemIfAvailable(JPopupMenu menu, Project project, BasicElement ikasanBasicElement) {
        AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement);
        if (viewHandler != null && viewHandler.hasPropertiesNavigationTarget()) {
            JMenuItem item = new JMenuItem(StudioBundle.message("menu.JumpToProperties"));
            item.addActionListener(new NavigateToPropertiesAction(project, ikasanBasicElement));
            menu.add(item);
        }
    }
}
