package org.ikasan.studio.ui.component.properties;

import javax.swing.*;
import java.awt.*;

/**
 * Popup test for ComponentDescriptionPanel
 */
class ComponentDescriptionPanelUiTest {
    private static final String TITLE = "Component Description Panel";

    public static void main(String[] args) {
        ComponentDescriptionPanelUiTest cronPanelUiTest = new ComponentDescriptionPanelUiTest();
        cronPanelUiTest.run();
    }

    public void run() {
        JFrame frame;
        String TEST_TEXT = "<html><body><h3>Brokers</h3><p>Brokers enrich the contents of the existing message with additional data or structure in a number of different ways.</p><p>Request Response Brokers can make calls to other systems such as a database or HTTP(s) RESTful services. Aggregating Brokers consume all incoming messages until a condition is met ie aggregate every 10 messages. Re-Sequencing Brokers consume all incoming messages until a condition is met and then release them messages as a list of newly ordered events. This can provide a powerful function when combined with a Splitter as the next component</p></body></html>";
        Dimension dimension = new Dimension(300, 300);
        HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel(null, dimension);
        htmlScrollingDisplayPanel.setText(TEST_TEXT);
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(htmlScrollingDisplayPanel);
        frame.pack();
        // Center the frame on the screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}