package org.ikasan.studio.ui.component.properties;

import lombok.Data;

import javax.swing.*;

@Data
public class ComponentInput {
    private boolean booleanInput;
    private JCheckBox trueBox;
    private JCheckBox falseBox;
    private JFormattedTextField propertyValueField;

    public ComponentInput(JCheckBox trueBox, JCheckBox falseBox) {
        booleanInput = true;
        this.trueBox = trueBox;
        this.falseBox = falseBox;
    }

    public ComponentInput(JFormattedTextField propertyValueField) {
        booleanInput = false;
        this.propertyValueField = propertyValueField;
    }

    public JComponent getFirstFocusComponent() {
        JComponent firstComponent;
        if (booleanInput) {
            firstComponent = trueBox;
        } else {
            firstComponent = propertyValueField;
        }
        return firstComponent;
    }

    public void setEnabled(boolean enabled) {
        if (trueBox != null) {
            trueBox.setEnabled(enabled);
        }
        if (falseBox != null) {
            falseBox.setEnabled(enabled);
        }
        if (propertyValueField != null) {
            propertyValueField.setEnabled(enabled);
        }
    }
}
