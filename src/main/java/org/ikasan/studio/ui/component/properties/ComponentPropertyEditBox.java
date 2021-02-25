package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.model.Ikasan.IkasanComponentProperty;

import javax.swing.*;
import java.text.NumberFormat;

public class ComponentPropertyEditBox {
    private String label;
    private JLabel labelField;
    private JFormattedTextField textField;

    public ComponentPropertyEditBox(IkasanComponentProperty componentProperty) {
        this.label = componentProperty.getMeta().getPropertyName();
        this.labelField = new JLabel(label);

        // @todo we can have all types of components with rich pattern matching validation
        if (componentProperty.getMeta().getDataType() == java.lang.Integer.class) {
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.textField = new JFormattedTextField(amountFormat);
            Object value = componentProperty.getValue();
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = new Integer((String)value);
                }
                textField.setValue(value);
            }
        } else {
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

    public JFormattedTextField getTextField() {
        return textField;
    }

    public Object getValue() {
        // The formatter would be null if this was a standard text field.
        if (textField.getFormatter() == null) {
            return textField.getText();
        } else {
            return textField.getValue();
        }
    }
}
