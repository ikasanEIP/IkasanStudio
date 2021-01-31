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

        if (componentProperty.getMeta().getDataType() == java.lang.Integer.class) {
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.textField = new JFormattedTextField(amountFormat);
            if (componentProperty.getValue() != null) {
                textField.setValue(componentProperty.getValue());
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
