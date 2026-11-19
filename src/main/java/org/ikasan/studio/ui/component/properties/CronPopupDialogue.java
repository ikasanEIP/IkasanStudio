package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.ui.StudioDialogWrapper;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CronPopupDialogue extends StudioDialogWrapper {
    private final CronPanel cronPanel;
    /**
     * CronPopupDialogue Modal window to support entry of cron configurations.
     *
     * @param project is the Intellij project instance
     * @param cronPanel to display and have entries taken on.
     */
    public CronPopupDialogue(Project project, CronPanel cronPanel) {
        // Modern approach: DialogWrapper with project scoping
        // The boolean parameter indicates if this dialog can be a parent (true = can be parent)
        // This replaces the deprecated IdeModalityType.PROJECT
        super(project, true);
        cronPanel.setCronPopupDialogue(this);
        this.cronPanel = cronPanel;
        init();  // from DialogWrapper which calls createStudioContent() below so make sure any state is initialised first.
        setTitle(cronPanel.getTitle());
    }

    @Override
    public boolean isModal() {
        return true;
    }

    @Override
    protected @Nullable JComponent createStudioContent() {
        return cronPanel;
    }
}
