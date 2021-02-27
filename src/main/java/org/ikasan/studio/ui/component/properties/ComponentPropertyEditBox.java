package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.model.Ikasan.IkasanComponentProperty;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class ComponentPropertyEditBox {
    private String label;
    private JLabel labelField;
    private JFormattedTextField textField;
    private JCheckBox booleanField;

    public ComponentPropertyEditBox(IkasanComponentProperty componentProperty) {
        this.label = componentProperty.getMeta().getPropertyName();
        this.labelField = new JLabel(label);
        Object value = componentProperty.getValue();
        // @todo we can have all types of components with rich pattern matching validation
        if (componentProperty.getMeta().getDataType() == java.lang.Integer.class) {
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.textField = new JFormattedTextField(amountFormat);
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = Integer.valueOf((String) value);
                }
                textField.setValue(value);
            }
        } else if (componentProperty.getMeta().getDataType() == java.lang.Boolean.class) {
            this.booleanField = new JCheckBox();
            booleanField.setBackground(Color.WHITE);
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = Boolean.valueOf((String) value);
                }

                if (value instanceof Boolean) {
                    booleanField.setSelected((Boolean)value);
                }
            }
        }
        else {
            // Assume String
            this.textField = new JFormattedTextField();

            if (componentProperty.getValue() != null) {
                textField.setText(componentProperty.getValue().toString());
            }
        }
        labelField.setToolTipText(componentProperty.getMeta().getHelpText());
    }

    public String getLabel() {
        return label;
    }

    public JLabel getLabelField() {
        return labelField;
    }

    public JComponent getInputField() {
        return booleanField != null ? booleanField : textField;
    }

    public Object getValue() {
        // The formatter would be null if this was a standard text field.
        if (booleanField != null) {
            return booleanField.isSelected();
        } else if (textField.getFormatter() == null) {
            return textField.getText();
        } else {
            return textField.getValue();
        }
    }
}
