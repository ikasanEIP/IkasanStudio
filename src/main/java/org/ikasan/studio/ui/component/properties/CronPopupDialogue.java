package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.DialogWrapper;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CronPopupDialogue extends DialogWrapper {
    private CronPanel cronPanel;
    /**
     * CronPopupDialogue Modal window to support entry of cron configurations.
     *
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param parentComponent of this popup
     * @param cronPanel to display and have entries taken on.
     */
    public CronPopupDialogue(String projectKey, Component parentComponent, CronPanel cronPanel) {
        super(UiContext.getProject(projectKey), parentComponent, true, IdeModalityType.PROJECT); // use current window as parent
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
