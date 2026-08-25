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
            // setEditable, not setEnabled: a fully disabled JTextComponent can't have its text selected or
            // copied at all, which is exactly the wrong behaviour for a read-only property value someone is
            // trying to copy out to report an issue. setEditable(false) blocks typing while leaving selection/
            // Ctrl+C/right-click-copy working normally.
            propertyValueField.setEditable(enabled);
        }
    }

    /**
     * Show/hide every widget behind this input - used by the properties-search filter to collapse a
     * non-matching row's space in its GridBagLayout parent.
     */
    public void setVisible(boolean visible) {
        if (trueBox != null) {
            trueBox.setVisible(visible);
        }
        if (falseBox != null) {
            falseBox.setVisible(visible);
        }
        if (propertyValueField != null) {
            propertyValueField.setVisible(visible);
        }
        if (propertyChoiceValueField != null) {
            propertyChoiceValueField.setVisible(visible);
        }
    }
}
