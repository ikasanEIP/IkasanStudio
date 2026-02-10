package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;

import javax.swing.*;

/**
 * Popup test for CronPanel
 */
class CronPanelUiTest {
    private static final String TITLE = "Cron Data Entry Panel";
    private static final String  TEST_PROJECT = "Test Project";
    JFrame frame;
    public static void main(String[] args) {
        CronPanelUiTest cronPanelUiTest = new CronPanelUiTest();
        cronPanelUiTest.run();
    }

    // @todo use Mock for Project
    public void run() {
//        CronPanel cronPanel = new CronPanel(TEST_PROJECT, "0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2010");
//        frame = new JFrame(TITLE);
//        cronPanel.setTestFrame(frame);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.add(cronPanel);
//        frame.pack();
//        // Center the frame on the screen
//        frame.setLocationRelativeTo(null);
//        frame.setVisible(true);
    }
}