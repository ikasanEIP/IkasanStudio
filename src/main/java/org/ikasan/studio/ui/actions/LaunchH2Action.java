package org.ikasan.studio.ui.actions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.execution.IndependentH2RunConfigurationService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Canvas controls reflect the actual project-scoped H2 process, including normal IDE Run/Stop actions. */
public final class LaunchH2Action implements ActionListener {
    private final Project project;
    private final JComponent component;
    public LaunchH2Action(Project project, JComponent component) {
        this.project = project; this.component = component; refresh();
    }
    public void listen(Disposable owner) {
        project.getService(IndependentH2RunConfigurationService.class).listen(this::refresh, owner);
    }
    @Override public void actionPerformed(ActionEvent event) {
        project.getService(IndependentH2RunConfigurationService.class).toggle(); refresh();
    }
    private void refresh() {
        var model = project.getService(UiContext.class).getIkasanModule();
        refreshPresentation(model == null || model.getUseEmbeddedH2());
    }
    public void refreshPresentation(boolean embedded) {
        var state = project.getService(IndependentH2RunConfigurationService.class).state();
        String key = switch (state) {
            case STOPPED -> component instanceof JMenuItem ? "menu.LaunchH2" : "button.H2Start";
            case RUNNING -> "h2.independent.stop";
            case STARTING -> "h2.independent.starting";
            case STOPPING -> "h2.independent.stopping";
        };
        if (component instanceof AbstractButton button) button.setText(StudioBundle.message(key));
        component.setEnabled(state == IndependentH2RunConfigurationService.State.RUNNING
                || state == IndependentH2RunConfigurationService.State.STOPPED && !embedded);
        component.setToolTipText(StudioBundle.message("h2.independent.hint"));
    }
}
