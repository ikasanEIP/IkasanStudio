package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Stops module processes launched and tracked by Ikasan Studio for this project. */
public class StopApplicationAction implements ActionListener {
    private final Project project;

    public StopApplicationAction(Project project) {
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        project.getService(IkasanDebugSessionService.class).stopModule();
    }
}
