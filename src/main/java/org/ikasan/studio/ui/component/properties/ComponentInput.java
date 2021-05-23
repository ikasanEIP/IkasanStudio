package org.ikasan.studio.ui.component.properties;

import javax.swing.*;

public class ComponentInput {
    private boolean isBooleanInput;
    private JCheckBox trueBox;
    private JCheckBox falseBox;
    private JFormattedTextField propertyValueField;

    public ComponentInput(JCheckBox trueBox, JCheckBox falseBox) {
        isBooleanInput = true;
        this.trueBox = trueBox;
        this.falseBox = falseBox;
    }

    public ComponentInput(JFormattedTextField propertyValueField) {
        isBooleanInput = false;
        this.propertyValueField = propertyValueField;
    }

    public JComponent getFirstFocusComponent() {
        JComponent firstComponent = null ;
        if (isBooleanInput) {
            firstComponent = trueBox;
        } else {
            firstComponent = propertyValueField;
        }
        return firstComponent;
    }

    public boolean isBooleanInput() {
        return isBooleanInput;
    }

    public void setBooleanInput(boolean booleanInput) {
        isBooleanInput = booleanInput;
    }

    public JCheckBox getTrueBox() {
        return trueBox;
    }

    public void setTrueBox(JCheckBox trueBox) {
        this.trueBox = trueBox;
    }

    public JCheckBox getFalseBox() {
        return falseBox;
    }

    public void setFalseBox(JCheckBox falseBox) {
        this.falseBox = falseBox;
    }

    public JFormattedTextField getPropertyValueField() {
        return propertyValueField;
    }

    public void setPropertyValueField(JFormattedTextField propertyValueField) {
        this.propertyValueField = propertyValueField;
    }
}
