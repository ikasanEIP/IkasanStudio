package org.ikasan.studio.ui.component.properties;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;

import static org.ikasan.studio.ui.Styling.*;
import static org.ikasan.studio.ui.component.properties.CronExpression.DAY_OF_MONTH;
import static org.ikasan.studio.ui.component.properties.CronExpression.DAY_OF_WEEK;

@Setter
@Getter
public class CronPanel extends JPanel {
//    private static final Logger LOG = Logger.getInstance("#CronPanel");
    private String title = "Quartz Cron Configuration";
    JTextField[] textFields = new JTextField[CronExpression.values().length];
    JLabel[] labelFields = new JLabel[CronExpression.values().length];
    JTextPane helpTextPane;
    JFrame testFrame;
    JPanel helpPanel;
    boolean helpEnabled = false;

    protected final String projectKey;
    private transient CronPopupDialogue cronPopupDialogue;

    public CronPanel(String projectKey, String currentValue) {
        super();
        this.projectKey = projectKey;
        this.setLayout(new BorderLayout());
        JPanel dataEntryPanel = new JPanel();
        dataEntryPanel.setBorder(null);
        dataEntryPanel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.gridy = 0;

        int index = 0;
        int maxIndex = CronExpression.values().length;
        // Populate based on existing field
        if (currentValue != null && !currentValue.isBlank()) {
            String[] parts = currentValue.split(" ");
            for (index = 0; index < parts.length && index < maxIndex; index++) {
                CronExpression cronField = CronExpression.values()[index];
                textFields[index] = new JTextField(10);
                labelFields[index] = new JLabel();

                addRow(gc, dataEntryPanel, new JButton(cronField.fieldName), textFields[index], labelFields[index], cronField.defaultValue, parts[index], toolTip(cronField));
            }
        }
        // Fill in remaining values with defaults
        for (; index < maxIndex; index++) {
            CronExpression cronField = CronExpression.values()[index];
            textFields[index] = new JTextField(10);
            labelFields[index] = new JLabel();
            addRow(gc, dataEntryPanel, new JButton(cronField.fieldName), textFields[index], labelFields[index], cronField.defaultValue, cronField.defaultValue, toolTip(cronField));
        }

        add(dataEntryPanel, BorderLayout.NORTH);

        JPanel okCancelPanel = new JPanel();
        okCancelPanel.setBorder(null);
        okCancelPanel.setLayout(new FlowLayout());

        JButton helpButton = new JButton("Expand help");
        okCancelPanel.add(helpButton);
        helpButton.addActionListener( e -> {
            helpEnabled = !helpEnabled;
            helpButton.setText(helpEnabled ? "Collapse Help" : "Expand help");
            helpPanel.setVisible(helpEnabled);
            if (testFrame != null) {
                testFrame.pack();
                testFrame.repaint();
            } else {
                cronPopupDialogue.redraw();
            }
        });

        add(okCancelPanel, BorderLayout.SOUTH);

        helpPanel = new JPanel(new BorderLayout());
        Border helpBorder = BorderFactory.createTitledBorder("Quartz Cron Configuration Help");
        helpPanel.setBorder(helpBorder);
        helpTextPane = new JTextPane();
        helpTextPane.setContentType("text/html");
        helpTextPane.setText(
                "<p>Hover over each field to see what values are allowed per field." +
                        "<p>Click the buttons to reset the field to its default value." +
                        "<p>Further details can be found at " +
                        "<ul><li>https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html"
        );
        helpPanel.setBackground(IKASAN_GREY);
        helpPanel.add(helpTextPane, BorderLayout.CENTER);
        add(helpPanel, BorderLayout.CENTER);
        helpPanel.setVisible(helpEnabled);
    }

    protected String getValue() {
        StringBuilder value = new StringBuilder();
        for (JTextField textField : textFields) {
            value.append(textField.getText()).append(" ");
        }
        return value.toString() ;
    }

    protected static String toolTip(CronExpression cronField) {
        return "Enter one of " + cronField.specialCharacters + ", n is " + cronField.allowedValues;
    }

    protected void addRow(GridBagConstraints gc, JPanel dataEntryPanel, JButton resetButton, JTextField textEntryField, JLabel description, String defaultValue, String currentValue, String toolTip) {
        gc.gridx = 0;
        dataEntryPanel.add(resetButton, gc);
        gc.gridx = 1;
        gc.weightx = 0.75;
        dataEntryPanel.add(textEntryField, gc);
        gc.gridx = 2;
        gc.weightx = 0.25;
        dataEntryPanel.add(description, gc);
        gc.weightx = 0.0;
        resetButton.addActionListener(e->{
            textEntryField.setText(defaultValue);
            setMessageField(defaultValue, resetButton.getText(), description);
        });

        resetButton.setToolTipText(toolTip);
        textEntryField.setToolTipText(toolTip);

        textEntryField.getDocument().addDocumentListener(new DocumentListener() {
            // @See ComponentPropertiesPanel#editBoxChangeListener()
            @Override
            public void insertUpdate(DocumentEvent e) {
                editBoxChangeListener(e, resetButton.getText(), description);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                editBoxChangeListener(e, resetButton.getText(), description);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                editBoxChangeListener(e, resetButton.getText(), description);
            }
        });
        textEntryField.setText(currentValue);
        gc.gridy++;
    }


    public void editBoxChangeListener(DocumentEvent e, String fieldName, JLabel description) {
        javax.swing.text.Document doc = e.getDocument();
        // Get the current text from the document
        String currentText = null;
        try {
            currentText = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
//            LOG.warn("STUDIO: WARN, non-fatal unexpected BadLocationException " + ex.getMessage());
        }
        setMessageField(currentText, fieldName, description);
    }
    public void setMessageField(String currentText, String fieldName, JLabel description) {
        String validatonMessage = validateFields(fieldName);
        if (!validatonMessage.isBlank()) {
            description.setText(validatonMessage);
            description.setForeground(IKASAN_RED);
        } else {
            String cronDescription = CronExpression.describeField(currentText, fieldName);
            description.setText(cronDescription);
            description.setForeground(IKASAN_BLACK);
        }
    }

    protected String validateFields(String fieldName) {
        String validationMessage = "";
        CronExpression cronField = CronExpression.getFromName(fieldName);
        if ((DAY_OF_WEEK.equals(cronField) || DAY_OF_MONTH.equals(cronField)) &&
                textFields[DAY_OF_MONTH.index] != null &&
                textFields[DAY_OF_WEEK.index] != null) {
            if (!textFields[DAY_OF_MONTH.index].getText().equals("?") &&
                !textFields[DAY_OF_WEEK.index].getText().equals("?")) {
                validationMessage = "Day of week and day of month cant both be set";
            } else if ( textFields[DAY_OF_MONTH.index].getText().equals("?") &&
                        textFields[DAY_OF_WEEK.index].getText().equals("?")) {
                validationMessage = "Day of week and day of month cant both be ?";
            }
        }

        return validationMessage;
    }
}
