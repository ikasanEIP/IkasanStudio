package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.ui.StudioBundle;

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
@SuppressWarnings("rawtypes")
public class CronPanel extends JBPanel {
//    private static final Logger LOG = Logger.getInstance("#CronPanel");
    private String title = StudioBundle.message("dialog.QuartzCronConfiguration");
    JTextField[] textFields = new JTextField[CronExpression.values().length];
    JLabel[] labelFields = new JLabel[CronExpression.values().length];
    JTextPane helpTextPane;
    JFrame testFrame;
    @SuppressWarnings("rawtypes")
    JBPanel helpPanel;
    boolean helpEnabled = false;

    protected final Project project;
    private transient CronPopupDialogue cronPopupDialogue;

    public CronPanel(Project project, String currentValue) {
        super();
        this.project = project;
        this.setLayout(new BorderLayout());
        @SuppressWarnings("rawtypes")
        JBPanel dataEntryPanel = new JBPanel();
        dataEntryPanel.setBorder(null);
        dataEntryPanel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = JBUI.insets(5);
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

                addRow(gc, dataEntryPanel, cronField, textFields[index], labelFields[index], cronField.defaultValue, parts[index], toolTip(cronField));
            }
        }
        // Fill in remaining values with defaults
        for (; index < maxIndex; index++) {
            CronExpression cronField = CronExpression.values()[index];
            textFields[index] = new JTextField(10);
            labelFields[index] = new JLabel();
            addRow(gc, dataEntryPanel, cronField, textFields[index], labelFields[index], cronField.defaultValue, cronField.defaultValue, toolTip(cronField));
        }

        add(dataEntryPanel, BorderLayout.NORTH);

        @SuppressWarnings("rawtypes")
        JBPanel okCancelPanel = new JBPanel();
        okCancelPanel.setBorder(null);
        okCancelPanel.setLayout(new FlowLayout());

        JButton helpButton = new JButton(StudioBundle.message("button.ExpandHelp"));
        okCancelPanel.add(helpButton);
        helpButton.addActionListener( e -> {
            helpEnabled = !helpEnabled;
            helpButton.setText(helpEnabled ? StudioBundle.message("button.CollapseHelp") : StudioBundle.message("button.ExpandHelp"));
            helpPanel.setVisible(helpEnabled);
            if (testFrame != null) {
                testFrame.pack();
                testFrame.repaint();
            } else {
                cronPopupDialogue.redraw();
            }
        });

        add(okCancelPanel, BorderLayout.SOUTH);

        helpPanel = new JBPanel(new BorderLayout());
        Border helpBorder = BorderFactory.createTitledBorder(StudioBundle.message("label.QuartzCronConfigurationHelp"));
        helpPanel.setBorder(helpBorder);
        helpTextPane = new JTextPane();
        helpTextPane.setContentType("text/html");
        helpTextPane.setText(StudioBundle.message("label.CronHelpText"));
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
        return StudioBundle.message("message.EnterOneOfNIs", cronField.specialCharacters, cronField.allowedValues);
    }

    protected void addRow(GridBagConstraints gc, @SuppressWarnings("rawtypes") JBPanel dataEntryPanel, CronExpression cronField, JTextField textEntryField, JLabel description, String defaultValue, String currentValue, String toolTip) {
        JButton resetButton = new JButton(cronField.getFieldName());
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
            setMessageField(defaultValue, cronField, description);
        });

        resetButton.setToolTipText(toolTip);
        textEntryField.setToolTipText(toolTip);

        textEntryField.getDocument().addDocumentListener(new DocumentListener() {
            // @See ComponentPropertiesPanel#editBoxChangeListener()
            @Override
            public void insertUpdate(DocumentEvent e) {
                editBoxChangeListener(e, cronField, description);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                editBoxChangeListener(e, cronField, description);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                editBoxChangeListener(e, cronField, description);
            }
        });
        textEntryField.setText(currentValue);
        gc.gridy++;
    }


    public void editBoxChangeListener(DocumentEvent e, CronExpression cronField, JLabel description) {
        javax.swing.text.Document doc = e.getDocument();
        // Get the current text from the document
        String currentText = null;
        try {
            currentText = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
//            LOG.warn("STUDIO: WARN, non-fatal unexpected BadLocationException " + ex.getMessage());
        }
        setMessageField(currentText, cronField, description);
    }
    public void setMessageField(String currentText, CronExpression cronField, JLabel description) {
        String validatonMessage = validateFields(cronField);
        if (!validatonMessage.isBlank()) {
            description.setText(validatonMessage);
            description.setForeground(IKASAN_RED);
        } else {
            String cronDescription = CronExpression.describeField(currentText, cronField);
            description.setText(cronDescription);
            description.setForeground(IKASAN_BLACK);
        }
    }

    protected String validateFields(CronExpression cronField) {
        String validationMessage = "";
        if ((DAY_OF_WEEK.equals(cronField) || DAY_OF_MONTH.equals(cronField)) &&
                textFields[DAY_OF_MONTH.index] != null &&
                textFields[DAY_OF_WEEK.index] != null) {
            if (!textFields[DAY_OF_MONTH.index].getText().equals("?") &&
                !textFields[DAY_OF_WEEK.index].getText().equals("?")) {
                validationMessage = StudioBundle.message("message.DayOfWeekAndDayOfMonthCantBothBeSet");
            } else if ( textFields[DAY_OF_MONTH.index].getText().equals("?") &&
                        textFields[DAY_OF_WEEK.index].getText().equals("?")) {
                validationMessage = StudioBundle.message("message.DayOfWeekAndDayOfMonthCantBothBeQuestionMark");
            }
        }

        return validationMessage;
    }
}
