package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import lombok.Data;

import javax.swing.*;

@Data
public class ComponentInput {
    private static final Logger LOG = Logger.getInstance("#ComponentInput");
    private boolean booleanInput;
    private boolean choiceInput;
    private JCheckBox trueBox;
    private JCheckBox falseBox;
    private JFormattedTextField propertyValueField;
    private ComboBox<Object> propertyChoiceValueField;

    public ComponentInput(JCheckBox trueBox, JCheckBox falseBox) {
        booleanInput = true;
        this.trueBox = trueBox;
        this.falseBox = falseBox;
    }

    public ComponentInput(JFormattedTextField propertyValueField) {
        booleanInput = false;
        choiceInput = false;
        this.propertyValueField = propertyValueField;
    }

    public ComponentInput(ComboBox<Object> propertyChoiceValueField) {
        booleanInput = false;
        choiceInput = true;
        this.propertyChoiceValueField = propertyChoiceValueField;
    }

    public JComponent getFirstFocusComponent() {
        JComponent firstComponent;
        if (booleanInput) {
            firstComponent = trueBox;
        } else if (choiceInput) {
            firstComponent = propertyChoiceValueField;
        } else {
            firstComponent = propertyValueField;
        }
        if (firstComponent == null) {
            LOG.warn("STUDIO: Component can't ever be null ");
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
