package org.ikasan.studio.ui.component.properties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ExceptionResolverPanelUITest {
    private static final String TITLE = "ExceptionResolverPanel";
    private static final String TEST_PROJECT = "Test Project";

    public static void main(String[] args) {
        ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(TEST_PROJECT, true);
        // Create the main frame
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(exceptionResolverPanel);
        frame.setSize(400, 200);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setLayout(null); // Using null layout for simplicity
//
////         Create a label
//        JLabel label = new JLabel("Hello, World!");
//        label.setBounds(150, 50, 100, 30); // Setting position and size
//        frame.add(label);
//
//        // Create a button
//        JButton button = new JButton("Click Me");
//        button.setBounds(150, 100, 100, 30); // Setting position and size
//        frame.add(button);
//
//        // Add an action listener to the button
//        button.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                label.setText("Button Clicked!");
//            }
//        });

        // Make the frame visible
        frame.setVisible(true);
    }
}