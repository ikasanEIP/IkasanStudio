package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Local navigation bindings leave IDE shortcuts and normal Tab traversal intact. */
final class CanvasKeyboardNavigation {
    private CanvasKeyboardNavigation() { }

    static void install(DesignerCanvas canvas, Project project) {
        canvas.getAccessibleContext().setAccessibleName(StudioBundle.message("accessible.DesignerCanvas"));
        canvas.getAccessibleContext().setAccessibleDescription(StudioBundle.message("accessible.DesignerCanvasDescription"));
        bind(canvas, "RIGHT", () -> move(canvas, project, 1));
        bind(canvas, "DOWN", () -> move(canvas, project, 1));
        bind(canvas, "LEFT", () -> move(canvas, project, -1));
        bind(canvas, "UP", () -> move(canvas, project, -1));
        Runnable menu = () -> {
            if (project.isDisposed()) return;
            var context = project.getService(UiContext.class);
            var selected = context.getSelectedComponent();
            BasicElement target = selected instanceof BasicElement element ? element : context.getIkasanModule();
            if (target == null) return;
            var visible = canvas.getVisibleRect();
            DesignCanvasContextMenu.showPopupAndNavigateMenu(project, canvas,
                    new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                            visible.x + 16, visible.y + 16, 1, true, MouseEvent.BUTTON3), target);
        };
        bind(canvas, "shift F10", menu);
        bind(canvas, "CONTEXT_MENU", menu);
    }

    static List<BasicElement> elements(Module module) {
        List<BasicElement> result = new ArrayList<>();
        if (module == null) return result;
        result.add(module);
        for (var flow : module.getFlows()) {
            result.add(flow);
            result.addAll(flow.getFlowRoute().getConsumerAndFlowRouteElements());
        }
        return result;
    }

    private static void move(DesignerCanvas canvas, Project project, int direction) {
        if (project.isDisposed()) return;
        var context = project.getService(UiContext.class);
        List<BasicElement> elements = elements(context.getIkasanModule());
        if (elements.isEmpty() || context.getPropertiesTabPanel() == null) return;
        var selected = context.getSelectedComponent();
        int current = selected instanceof BasicElement element ? elements.indexOf(element) : -1;
        int next = current < 0 ? 0 : Math.max(0, Math.min(elements.size() - 1, current + direction));
        BasicElement target = elements.get(next);
        canvas.editComponent(target);
        // Selection can be refused when the developer keeps pending property edits.
        if (context.getSelectedComponent() != target) return;
        var handler = ViewHandlerCache.getAbstractViewHandler(project, target);
        if (handler != null) canvas.scrollRectToVisible(new java.awt.Rectangle(
                handler.getLeftX(), handler.getTopY(), Math.min(handler.getWidth(), canvas.getVisibleRect().width),
                Math.min(handler.getHeight(), canvas.getVisibleRect().height)));
        canvas.repaint();
    }

    private static void bind(JComponent component, String key, Runnable action) {
        String name = "studio.canvas." + key;
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }
}
