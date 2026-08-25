package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
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
    private static final Logger LOG = Logger.getInstance("#CronPanel");
    private String title = StudioBundle.message("dialog.QuartzCronConfiguration");
    JTextField[] textFields = new JTextField[CronExpression.values().length];
    JLabel[] labelFields = new JLabel[CronExpression.values().length];
    JTextPane helpTextPane;
    JFrame testFrame;
    @SuppressWarnings("rawtypes")
    JBPanel helpPanel;
    boolean helpEnabled = false;
    // The dialog's size immediately before Expand Help grows it - pack() alone doesn't reliably shrink the
    // dialog back down on Collapse (Container caches getPreferredSize() while still "valid", so a pack() issued
    // in the same tick as the setVisible(false) that should invalidate it can read stale, still-expanded
    // dimensions) - so Collapse restores this explicitly rather than trusting a recomputed preferred size.
    private Dimension collapsedSize;

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
            Window window = getContainingWindow();
            boolean expanding = !helpEnabled;
            if (expanding && window != null) {
                collapsedSize = window.getSize();
            }
            helpEnabled = expanding;
            helpButton.setText(helpEnabled ? StudioBundle.message("button.CollapseHelp") : StudioBundle.message("button.ExpandHelp"));
            // TEMPORARY - diagnosing why Collapse doesn't shrink the dialog back down; remove once fixed.
            LOG.warn("STUDIO: CRON RESIZE before setVisible(" + helpEnabled + ") - window.size=" + (window == null ? "null" : window.getSize())
                    + " window.minimumSize=" + (window == null ? "null" : window.getMinimumSize())
                    + " window.isMinimumSizeSet=" + (window == null ? "null" : window.isMinimumSizeSet())
                    + " window.isValid=" + (window == null ? "null" : window.isValid())
                    + " collapsedSize=" + collapsedSize);
            helpPanel.setVisible(helpEnabled);
            if (window != null) {
                if (helpEnabled) {
                    window.pack();
                } else if (collapsedSize != null) {
                    // Confirmed via debug logging: IntelliJ's DialogWrapper leaves an explicit, stuck
                    // Window#minimumSize behind from Expand's auto-grow - its own validate() pass does NOT
                    // relax it back down even though the rest of the layout (preferredSize, child components)
                    // correctly recomputes to the smaller, collapsed size. That stale minimum silently clamps
                    // any setSize() attempting to shrink below it. Clear it explicitly, then resize, then
                    // validate() to force children to reflow to the new (smaller) bounds - skipping that last
                    // step previously left the window resized but its contents un-relaid-out.
                    window.setMinimumSize(null);
                    window.setSize(collapsedSize);
                    window.validate();
                    LOG.warn("STUDIO: CRON RESIZE after setMinimumSize(null)+setSize(" + collapsedSize + ")+validate() - window.size=" + window.getSize()
                            + " window.minimumSize=" + window.getMinimumSize()
                            + " window.isMinimumSizeSet=" + window.isMinimumSizeSet());
                }
                window.repaint();
            }
        });

        add(okCancelPanel, BorderLayout.SOUTH);

        helpPanel = new JBPanel(new BorderLayout());
        // Match the titled-border styling used for the property groups (ComponentPropertiesPanel#setSubPanel)
        // rather than the plain default-look-and-feel titled border this used to have.
        Border helpBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeAwareColors.getBorderColor()),
                StudioBundle.message("label.QuartzCronConfigurationHelp"),
                TitledBorder.LEFT,
                TitledBorder.TOP);
        helpPanel.setBorder(helpBorder);
        helpTextPane = new JTextPane();
        helpTextPane.setContentType("text/html");
        helpTextPane.setText(StudioBundle.message("label.CronHelpText"));
        helpPanel.setBackground(ThemeAwareColors.getBackgroundColor());
        helpPanel.add(helpTextPane, BorderLayout.CENTER);
        add(helpPanel, BorderLayout.CENTER);
        helpPanel.setVisible(helpEnabled);
    }

    /**
     * @return the top-level window hosting this panel - the test harness's plain JFrame if set, otherwise the
     * real popup dialog's underlying Window, or null if neither has been wired up yet.
     */
    private Window getContainingWindow() {
        if (testFrame != null) {
            return testFrame;
        }
        return cronPopupDialogue != null ? cronPopupDialogue.getWindow() : null;
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
            description.setForeground(ThemeAwareColors.getTextColor());
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
