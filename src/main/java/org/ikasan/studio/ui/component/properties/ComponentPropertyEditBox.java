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
    private Class type;

    public ComponentPropertyEditBox(IkasanComponentProperty componentProperty) {
        this.label = componentProperty.getMeta().getPropertyName();
        this.labelField = new JLabel(label);
        type = componentProperty.getMeta().getDataType();
        Object value = componentProperty.getValue();

        // @todo we can have all types of components with rich pattern matching validation
        if (type == java.lang.Integer.class || type == java.lang.Long.class) {
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.textField = new JFormattedTextField(amountFormat);
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = Integer.valueOf((String) value);
                }
                textField.setValue(value);
            }
        } else if (type == java.lang.Boolean.class) {
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
        if (type == java.lang.Boolean.class) {
            return booleanField.isSelected();
        } else if (type == java.lang.String.class) {
            // The formatter would be null if this was a standard text field.
            return textField.getText();
        } else {
            return textField.getValue();
        }
    }

    /**
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    public boolean isEmpty() {
        // For boolean we don't current support unset @todo support unset if we need to
        if (type == java.lang.String.class) {
            return textField.getText() == null || textField.getText().isEmpty();
        } else if (type == java.lang.Integer.class || type == java.lang.Long.class) {
            // NumberFormat.getNumberInstance() will always return long
            return textField.getValue() == null || ((Long)textField.getValue() == 0);
        }
        return false;
    }
}
