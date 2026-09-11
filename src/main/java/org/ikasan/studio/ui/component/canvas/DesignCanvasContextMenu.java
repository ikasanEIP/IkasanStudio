package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.intellij.migration.MigrationController;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.analysis.JmsFlowConnections;
import org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_POSITION;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
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
        JPopupMenu menu = createCanvasMenu(project, mouseEvent, ikasanBasicElement);
        if (menu.getComponentCount() > 0) {
            menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
        }
    }

    static JPopupMenu createCanvasMenu(Project project, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
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
        } else if (ikasanBasicElement instanceof FlowElement flowElement) {
            if (project.getService(UiContext.class).isRestartPending(UiContext.restartPendingKey(flowElement))) {
                menu.add(createModuleRestartRequiredMenuItem(project));
                menu.addSeparator();
            }
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
            if (flowElement.getComponentMeta().isTimeEventConsumer()
                    && (flowElement.getComponentMeta().isLocalFileConsumer()
                    || org.ikasan.studio.core.metapack.model.ComponentMeta.FILE_TRANSFER_TEST_PAYLOAD_ADAPTER
                    .equals(flowElement.getComponentMeta().getTestPayloadAdapter()))) {
                JMenuItem scan = new JMenuItem(StudioBundle.message("menu.TriggerLocalFileScan"));
                scan.addActionListener(new TriggerScheduledConsumerAction(project, flowElement));
                menu.add(scan);
                menu.add(createTriggerNowLimitationsMenuItem(project));
            }
            if (flowElement.getComponentMeta().isLocalFileConsumer()) {
                JMenuItem directory = new JMenuItem(StudioBundle.message("menu.ShowLocalFileScanDirectory"));
                directory.addActionListener(new ShowLocalFileScanDirectoryAction(project, flowElement));
                menu.add(directory);
            }
            if (JmsFlowConnections.isJmsProducer(flowElement)
                    && CreateTestJmsConsumerFlowAction.supports(flowElement)) {
                menu.addSeparator();
                menu.add(createTestJmsConsumerFlowMenuItem(project, flowElement));
            }
            if (flowElement.getComponentMeta().supportsTestMailServer()) {
                menu.addSeparator();
                menu.add(createStartTestMailServerMenuItem(project, ikasanBasicElement));
                menu.add(createStopTestMailServerMenuItem(project, ikasanBasicElement));
            }
            if (flowElement.getComponentMeta().supportsTestFtpServer()) {
                menu.addSeparator();
                menu.add(createStartTestFtpServerMenuItem(project, ikasanBasicElement));
                menu.add(createStopTestFtpServerMenuItem(project, ikasanBasicElement));
                menu.add(createShowTestFtpOverwriteLimitationMenuItem(project));
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
        }
        if (ikasanBasicElement instanceof Module module) {
            JCheckBoxMenuItem keepCanvas = new JCheckBoxMenuItem(
                    StudioBundle.message("checkbox.KeepCanvasSelectedAtDebugBreakpoints"),
                    org.ikasan.studio.intellij.settings.IkasanStudioSettings.isKeepCanvasSelectedAtDebugBreakpoints());
            keepCanvas.setToolTipText(StudioBundle.message("tooltip.KeepCanvasSelectedAtDebugBreakpointsMenu"));
            keepCanvas.addActionListener(event -> org.ikasan.studio.intellij.settings.IkasanStudioSettings
                    .setKeepCanvasSelectedAtDebugBreakpoints(keepCanvas.isSelected()));
            menu.add(keepCanvas);
            menu.addSeparator();
            JMenuItem migrate = new JMenuItem(StudioBundle.message("action.IkasanStudio.MigrateVersion.text"));
            migrate.setToolTipText(StudioBundle.message("action.IkasanStudio.MigrateVersion.description"));
            migrate.setEnabled(module.isInitialised());
            migrate.addActionListener(event -> MigrationController.open(project, false));
            menu.add(migrate);
            menu.addSeparator();
            JMenuItem importModel = new JMenuItem(StudioBundle.message("button.ImportModelJson"));
            importModel.setToolTipText(StudioBundle.message("tooltip.ImportModelJson"));
            importModel.addActionListener(event ->
                    org.ikasan.studio.intellij.project.ModelImporter.openImportDialog(project));
            menu.add(importModel);
            menu.add(createSaveAsMenuItem(project));
            menu.add(createLoadMenuItem(project));
            menu.add(createLaunchDashboardMenuItem(project));
            menu.add(createLaunchH2MenuItem(project));
            menu.add(createDebugMenuItem(project));
        }
        return menu;
    }

    /**
     * Minimal popup for a right-click directly on the shared "Test Mail Server" canvas node (see
     * {@code DesignerCanvas#paintTestMailServerNode}) - deliberately just the one Stop item, not the full
     * producer menu {@link #showPopupAndNavigateMenu} would show for {@code ikasanBasicElement} itself (Delete
     * Component, Edit Component, etc. would act on the underlying Email Producer, which isn't what's visually
     * being pointed at here). No Start item either: this node is only ever painted while its address is
     * actually listening. The popup exposes its connection details and the applicable Stop action.
     */
    public static void showStopTestMailServerMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, BasicElement ikasanBasicElement) {
        JPopupMenu menu = createTestMailServerMenu(project, ikasanBasicElement);
        menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
    }

    public static void showStopTestFtpServerMenu(Project project, DesignerCanvas canvas, MouseEvent event, BasicElement element) {
        JPopupMenu menu = createTestFtpServerMenu(project, element);
        menu.show(canvas, event.getX(), event.getY());
    }

    /**
     * Minimal popup for a right-click on the compact "Test JMS harness" canvas node (see
     * {@code DesignerCanvas#paintTestJmsHarnessNode}) - the consumption warning, "Jump to Debug Component" (the
     * same navigation a double-click on the node already performs - see {@code DesignerCanvas#mouseClickAction}
     * - given a right-click entry point too, since it's otherwise not discoverable), and Remove. Removing
     * deletes the hidden harness Flow itself (Consumer + Debug + Dev Null sink) via the normal
     * {@link DeleteComponentAction} whole-flow path, including its generated Debug class and Undo support - the
     * harness node is the only on-canvas handle to that flow once it's hidden from normal rendering, so it
     * needs its own delete entry point here.
     */
    public static void showRemoveJmsHarnessMenu(Project project, DesignerCanvas canvas, MouseEvent event, TestJmsHarnessLinks.Link link) {
        JPopupMenu menu = new JPopupMenu();
        if (project.getService(UiContext.class).isRestartPending(UiContext.restartPendingKey(link.harnessFlow()))) {
            menu.add(createModuleRestartRequiredMenuItem(project));
            menu.addSeparator();
        }
        menu.add(createShowJmsHarnessConsumptionWarningMenuItem(project));
        menu.addSeparator();
        menu.add(createJumpToJmsHarnessDebugMenuItem(project, link));
        menu.addSeparator();
        menu.add(createRemoveJmsHarnessMenuItem(project, link.harnessFlow()));
        menu.show(canvas, event.getX(), event.getY());
    }

    /**
     * Same "attention-coloured item, click for the full warning dialog" pattern as
     * {@link #createShowTestFtpOverwriteLimitationMenuItem} - shown every time the harness node's menu opens,
     * not just once at creation, since this is exactly the kind of consequence a developer targeting the wrong
     * (real, production) destination would otherwise only discover after messages had already gone missing.
     */
    private static JMenuItem createShowJmsHarnessConsumptionWarningMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ShowJmsHarnessConsumptionWarning"));
        item.setForeground(StudioUIUtils.getAttentionColor());
        item.addActionListener(event -> Messages.showWarningDialog(project,
                "<html>" + StudioBundle.message("message.JmsHarnessConsumptionWarning") + "</html>",
                StudioBundle.message("menu.ShowJmsHarnessConsumptionWarning").replace("...", "")));
        return item;
    }

    private static JMenuItem createRemoveJmsHarnessMenuItem(Project project, Flow harnessFlow) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.RemoveTestJmsHarness"));
        item.addActionListener(new DeleteComponentAction(project, harnessFlow));
        return item;
    }

    /**
     * Attention-coloured "Restart required" warning item shown when an element (a Test JMS harness flow, or a
     * Debug component) was added while the module was still running - the running instance cannot contain it
     * until the next launch.
     */
    private static JMenuItem createModuleRestartRequiredMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ModuleRestartRequired"));
        item.setForeground(StudioUIUtils.getAttentionColor());
        item.addActionListener(event -> Messages.showWarningDialog(project,
                StudioBundle.message("message.ModuleRestartRequired"),
                StudioBundle.message("menu.ModuleRestartRequired").replace("...", "")));
        return item;
    }

    /**
     * Same navigation a double-click on the harness node performs (see
     * {@code DesignerCanvas#mouseClickAction}) - given a right-click entry point too, since double-click isn't
     * otherwise discoverable from the menu alone. Disabled, rather than omitted, when the harness flow's own
     * Debug component can't be resolved (e.g. it was since deleted), so the option is still visible but explains
     * why it doesn't work instead of silently disappearing.
     */
    private static JMenuItem createJumpToJmsHarnessDebugMenuItem(Project project, TestJmsHarnessLinks.Link link) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.JumpToDebugComponent"));
        FlowElement debugComponent = link.debugComponent();
        if (debugComponent == null) {
            item.setEnabled(false);
            item.setToolTipText(StudioBundle.message("message.CouldNotNavigateToDebugComponentForJmsHarness"));
        } else {
            item.addActionListener(new NavigateToCodeAction(project, debugComponent, true));
        }
        return item;
    }

    /**
     * Right-click on a component's own external endpoint pill (see
     * {@code IkasanFlowRouteViewHandler#displayExternalEndpointIfExists}) - a click there resolves back to the
     * same owning FlowElement {@link #showPopupAndNavigateMenu} would act on for a click on the component's own
     * icon box, but showing that full menu here duplicated every structural item (Delete, Edit, Add Debug,
     * Wiretap/Logging, Help, Jump to code/properties) on a target that visually reads as "the external system
     * this wire connects to", not "this step in the flow". This popup instead includes only the items that
     * actually relate to that external system: testing/triggering it, and starting or stopping the local test
     * server standing in for it - exactly the same conditions {@link #showPopupAndNavigateMenu} already used
     * for those same items, just relocated here and left out of the component's own menu. Shows nothing at all
     * (rather than an empty popup) when none of those conditions apply, e.g. a Generic Producer's endpoint.
     */
    public static void showEndpointMenu(Project project, DesignerCanvas designerCanvas, MouseEvent mouseEvent, FlowElement flowElement) {
        JPopupMenu menu = new JPopupMenu();
        if (flowElement.getComponentMeta().supportsSendTestMessage()
                && project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
            menu.add(IkasanFlowRouteViewHandler.usesTriggerBadge(flowElement)
                    ? createTriggerScheduledConsumerMenuItem(project, flowElement)
                    : createSendTestMessageMenuItem(project, flowElement));
        }
        if (flowElement.getComponentMeta().isTimeEventConsumer()
                && (flowElement.getComponentMeta().isLocalFileConsumer()
                || org.ikasan.studio.core.metapack.model.ComponentMeta.FILE_TRANSFER_TEST_PAYLOAD_ADAPTER
                .equals(flowElement.getComponentMeta().getTestPayloadAdapter()))) {
            JMenuItem scan = new JMenuItem(StudioBundle.message("menu.TriggerLocalFileScan"));
            scan.addActionListener(new TriggerScheduledConsumerAction(project, flowElement));
            menu.add(scan);
            menu.add(createTriggerNowLimitationsMenuItem(project));
        }
        if (flowElement.getComponentMeta().isLocalFileConsumer()) {
            JMenuItem directory = new JMenuItem(StudioBundle.message("menu.ShowLocalFileScanDirectory"));
            directory.addActionListener(new ShowLocalFileScanDirectoryAction(project, flowElement));
            menu.add(directory);
        }
        if (JmsFlowConnections.isJmsProducer(flowElement)
                && CreateTestJmsConsumerFlowAction.supports(flowElement)) {
            addSeparatorIfNotEmpty(menu);
            menu.add(createTestJmsConsumerFlowMenuItem(project, flowElement));
        }
        if (flowElement.getComponentMeta().supportsTestMailServer()) {
            addSeparatorIfNotEmpty(menu);
            menu.add(createStartTestMailServerMenuItem(project, flowElement));
            menu.add(createStopTestMailServerMenuItem(project, flowElement));
        }
        if (flowElement.getComponentMeta().supportsTestFtpServer()) {
            addSeparatorIfNotEmpty(menu);
            menu.add(createStartTestFtpServerMenuItem(project, flowElement));
            menu.add(createStopTestFtpServerMenuItem(project, flowElement));
            menu.add(createShowTestFtpOverwriteLimitationMenuItem(project));
        }
        if (menu.getComponentCount() > 0) {
            menu.show(designerCanvas, mouseEvent.getX(), mouseEvent.getY());
        }
    }

    private static void addSeparatorIfNotEmpty(JPopupMenu menu) {
        if (menu.getComponentCount() > 0) {
            menu.addSeparator();
        }
    }

    static JPopupMenu createTestMailServerMenu(Project project, BasicElement element) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createShowTestMailServerDetailsMenuItem(project, element));
        menu.addSeparator();
        menu.add(createStopTestMailServerMenuItem(project, element));
        return menu;
    }

    static JPopupMenu createTestFtpServerMenu(Project project, BasicElement element) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createShowTestFtpServerDetailsMenuItem(project, element));
        menu.add(createShowTestFtpOverwriteLimitationMenuItem(project));
        menu.add(createOpenTestFtpFileMenuItem(project, element));
        menu.add(createShowTestFtpDirectoryMenuItem(project, element));
        menu.addSeparator();
        menu.add(createStopTestFtpServerMenuItem(project, element));
        return menu;
    }

    private static JMenuItem createDeleteComponentMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.DeleteComponent"));
        item.addActionListener(new DeleteComponentAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createSendTestMessageMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message(ikasanBasicElement instanceof FlowElement element
                && element.getComponentMeta().isLocalFileConsumer() ? "menu.TestWithSelectedFiles" : "menu.SendTestMessage"));
        item.addActionListener(new SendTestMessageAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createTriggerScheduledConsumerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.TriggerScheduledConsumer"));
        item.addActionListener(new TriggerScheduledConsumerAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createTestJmsConsumerFlowMenuItem(Project project, FlowElement producer) {
        if (TestJmsHarnessLinks.findLinks(project.getService(UiContext.class).getIkasanModule())
                .stream().anyMatch(link -> link.ownerProducer() == producer)) {
            JMenuItem unavailable = new JMenuItem(StudioBundle.message("menu.TestJmsConsumerAlreadyExists"));
            unavailable.setEnabled(false);
            unavailable.setToolTipText(StudioBundle.message("tooltip.TestJmsConsumerAlreadyExists"));
            return unavailable;
        }
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.CreateTestJmsConsumerFlow"));
        item.addActionListener(new CreateTestJmsConsumerFlowAction(project, producer));
        return item;
    }

    private static JMenuItem createStartTestMailServerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StartTestMailServer"));
        item.addActionListener(new StartTestMailServerAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createShowTestMailServerDetailsMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ShowTestMailServerDetails"));
        item.addActionListener(new ShowTestMailServerDetailsAction(project, element));
        return item;
    }

    private static JMenuItem createStopTestMailServerMenuItem(Project project, BasicElement ikasanBasicElement) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StopTestMailServer"));
        item.addActionListener(new StopTestMailServerAction(project, ikasanBasicElement));
        return item;
    }

    private static JMenuItem createStartTestFtpServerMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StartTestFtpServer"));
        item.addActionListener(new StartTestFtpServerAction(project, element));
        return item;
    }

    private static JMenuItem createOpenTestFtpFileMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.OpenTestFtpFile"));
        item.addActionListener(new OpenTestFtpFileAction(project, element));
        return item;
    }

    private static JMenuItem createShowTestFtpDirectoryMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ShowTestFtpDirectory"));
        item.addActionListener(new ShowTestFtpDirectoryAction(project, element));
        return item;
    }

    private static JMenuItem createShowTestFtpOverwriteLimitationMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ShowTestFtpOverwriteLimitation"));
        item.setForeground(StudioUIUtils.getAttentionColor());
        item.addActionListener(event -> Messages.showWarningDialog(project,
                "<html>" + StudioBundle.message("message.TestFtpServerOverwriteLimitation") + "</html>",
                StudioBundle.message("menu.ShowTestFtpOverwriteLimitation").replace("...", "")));
        return item;
    }

    /**
     * Explains, from the context menu itself, why a Trigger scan now click can deliver nothing: the real scan is
     * still subject to the consumer's own acquisition rules (minimum file age, duplicate detection on name +
     * last-modified, and the filename pattern). Shown directly under Trigger scan now, mirroring the FTP overwrite
     * limitation item's attention-coloured menu entry.
     */
    private static JMenuItem createTriggerNowLimitationsMenuItem(Project project) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.TriggerNowLimitations"));
        item.setForeground(StudioUIUtils.getAttentionColor());
        item.addActionListener(event -> Messages.showWarningDialog(project,
                StudioBundle.message("message.TriggerNowLimitations"),
                StudioBundle.message("menu.TriggerNowLimitations").replace("...", "")));
        return item;
    }

    private static JMenuItem createShowTestFtpServerDetailsMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.ShowTestFtpServerDetails"));
        item.addActionListener(new ShowTestFtpServerDetailsAction(project, element));
        return item;
    }

    private static JMenuItem createStopTestFtpServerMenuItem(Project project, BasicElement element) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.StopTestFtpServer"));
        item.addActionListener(new StopTestFtpServerAction(project, element));
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
