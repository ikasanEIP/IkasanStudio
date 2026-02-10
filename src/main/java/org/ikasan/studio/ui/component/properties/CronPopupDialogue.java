package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CronPopupDialogue extends DialogWrapper {
    private final CronPanel cronPanel;
    /**
     * CronPopupDialogue Modal window to support entry of cron configurations.
     *
     * @param project is the Intellij project instance
     * @param parentComponent of this popup
     * @param cronPanel to display and have entries taken on.
     */
    public CronPopupDialogue(Project project, Component parentComponent, CronPanel cronPanel) {
        // Modern approach: DialogWrapper with project scoping
        // The boolean parameter indicates if this dialog can be a parent (true = can be parent)
        // This replaces the deprecated IdeModalityType.PROJECT
        super(project, true);
        cronPanel.setCronPopupDialogue(this);
        this.cronPanel = cronPanel;
        init();  // from DialogWrapper which calls createCenterPanel() below so make sure any state is initialised first.
        setTitle(cronPanel.getTitle());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return cronPanel;
    }

    public void redraw() {
//        pack();
        repaint();
    }
}
