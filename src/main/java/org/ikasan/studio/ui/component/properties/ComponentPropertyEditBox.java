package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.model.ikasan.IkasanComponentProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;

public class ComponentPropertyEditBox {
    private String propertyTitle;
    private JLabel propertyTitleField;
    private JFormattedTextField propertyValueField;
    private JCheckBox propertyBooleanField;
    private boolean userMaintainedClass = false;
    private boolean popupMode;
    private JCheckBox regenerateSourceCheckBox;
    private JLabel regenerateLabel;
    private Class type;

    public ComponentPropertyEditBox(IkasanComponentProperty componentProperty, boolean popupMode) {
        this.propertyTitle = componentProperty.getMeta().getPropertyName();
        this.popupMode = popupMode;
        this.propertyTitleField = new JLabel(propertyTitle);
        this.type = componentProperty.getMeta().getPropertyDataType();
        Object value = componentProperty.getValue();

        // @todo we can have all types of components with rich pattern matching validation
        if (type == java.lang.Integer.class || type == java.lang.Long.class) {
            // NUMERIC INPUT
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.propertyValueField = new JFormattedTextField(amountFormat);
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = Integer.valueOf((String) value);
                }
                propertyValueField.setValue(value);
            }
        } else if (type == java.lang.Boolean.class) {
            // BOOLEAN INPUT
            propertyBooleanField = new JCheckBox();
            propertyBooleanField.setBackground(Color.WHITE);
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    value = Boolean.valueOf((String) value);
                }

                if (value instanceof Boolean) {
                    propertyBooleanField.setSelected((Boolean)value);
                }
            }
        }
        else {
            // STRING INPUT
            this.propertyValueField = new JFormattedTextField();

            if (componentProperty.getValue() != null) {
                propertyValueField.setText(componentProperty.getValue().toString());
            }
        }
        propertyTitleField.setToolTipText(componentProperty.getMeta().getHelpText());

        if (componentProperty.getMeta().isUserImplementedClass() && !popupMode) {
            userMaintainedClass = true;
            regenerateLabel = new JLabel("Regenerate");
            regenerateSourceCheckBox = new JCheckBox();
            regenerateSourceCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    propertyValueField.setEditable(ie.getStateChange() == 1);
                    propertyValueField.setEnabled(ie.getStateChange() == 1);
                }
            });
            regenerateSourceCheckBox.setBackground(Color.WHITE);

            // Cant edit unless the regenerateSource is selected
            disableRegeneratingFeilds();
        }
    }

    public void disableRegeneratingFeilds() {
        if (userMaintainedClass) {
            regenerateSourceCheckBox.setSelected(false);
            propertyValueField.setEditable(false);
            propertyValueField.setEnabled(false);
        }
    }

    public String getPropertyLabel() {
        return propertyTitle;
    }

    public JLabel getPropertyTitleField() {
        return propertyTitleField;
    }

    public JComponent getInputField() {
        return propertyBooleanField != null ? propertyBooleanField : propertyValueField;
    }

    public Object getValue() {
        if (type == java.lang.Boolean.class) {
            return propertyBooleanField.isSelected();
        } else if (type == java.lang.String.class) {
            // The formatter would be null if this was a standard text field.
            return propertyValueField.getText();
        } else {
            return propertyValueField.getValue();
        }
    }

    /**
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    public boolean isEmpty() {
        // For boolean we don't current support unset @todo support unset if we need to
        if (type == java.lang.String.class) {
            return propertyValueField.getText() == null || propertyValueField.getText().isEmpty();
        } else if (type == java.lang.Integer.class || type == java.lang.Long.class) {
            // NumberFormat.getNumberInstance() will always return long
            return propertyValueField.getValue() == null || ((Long) propertyValueField.getValue() == 0);
        }
        return false;
    }

    public boolean isUserMaintainedClass() {
        return userMaintainedClass;
    }

    public boolean isUserMaintainedClassWithPermissionToRegenerate() {
        return isUserMaintainedClass() && regenerateSourceCheckBox != null && regenerateSourceCheckBox.isSelected();
    }

    public JCheckBox getRegenerateSourceCheckBox() {
        return regenerateSourceCheckBox;
    }

    public JLabel getRegenerateLabel() {
        return regenerateLabel;
    }
}
