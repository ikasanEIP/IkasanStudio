package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CronPopupDialogue extends DialogWrapper {
    private CronPanel cronPanel;
    /**
     * CronPopupDialogue Modal window to support entry of cron configurations.
     *
     * @param project currently being worked upon
     * @param parentComponent of this popup
     * @param cronPanel to display and have entries taken on.
     */
    public CronPopupDialogue(Project project, Component parentComponent, CronPanel cronPanel) {
        super(project, parentComponent, true, IdeModalityType.PROJECT); // use current window as parent
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
