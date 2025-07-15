package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.STRING_LIST;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ComponentPropertyEditBox {
    private static final Logger LOG = Logger.getInstance("#ComponentPropertyEditBox");
    private final JLabel propertyTitleField;
    private final JButton dataValidationHelper;
    private ComboBox<Object> propertyChoiceValueField;
    private JFormattedTextField propertyValueField;
    private JCheckBox propertyBooleanFieldTrue;
    private JCheckBox propertyBooleanFieldFalse;
    private boolean affectsUserImplementedClass = false;
    private boolean isList = false;
    private final ComponentPropertyMeta meta;
    private final ComponentProperty componentProperty;
    private final String projectKey;

    /**
     * Constructor
     * @param projectKey identity of the users current Java project
     * @param componentProperty to be exposed for edit
     * @param componentInitialisation only default the value if this is true
     */
    public ComponentPropertyEditBox(String projectKey, ComponentProperty componentProperty, boolean componentInitialisation) {
        this(projectKey, componentProperty, componentInitialisation, null, null);
    }

    /**
     * Constructor
     * @param projectKey identity of the users current Java project
     * @param componentProperty to be exposed for edit
     * @param componentInitialisation only default the value if this is true
     * @param listenerFoAnyEditChanges used by the parent to detect changes to the values being edited. If this is not required, use the other constructor.
     * @param componentPropertyEditBoxMap a growing list of propertyName -> ComponentPropertyEditBox, this instance will be added to it in the constructor. This is needed if fields could default off each other. If this is not required, use the other constructor.
     */
    public ComponentPropertyEditBox(String projectKey, ComponentProperty componentProperty, boolean componentInitialisation, SimpleChangeListener listenerFoAnyEditChanges, Map<String, ComponentPropertyEditBox> componentPropertyEditBoxMap) {
        this.projectKey = projectKey;
        this.componentProperty = componentProperty;
        this.propertyTitleField = new JLabel(componentProperty.getMeta().getPropertyName());
        this.meta = componentProperty.getMeta();
        if (componentPropertyEditBoxMap != null) {
            componentPropertyEditBoxMap.put(getPropertyKey(), this);
        }

        Object value = componentProperty.getValue();
        if (    componentInitialisation &&
                value == null &&
                !meta.isOptional() &&
                meta.getDefaultValue() != null &&
                !ComponentPropertyMeta.isSubstitutionValue(meta.getDefaultValue())) {
            componentProperty.setValue(componentProperty.getDefaultValue());
        }

        // @todo we can have all types of components with rich pattern matching validation
        if (meta.getChoices() != null) {
            propertyChoiceValueField = new ComboBox<>();
            meta.getChoices()
                .forEach( choice -> propertyChoiceValueField.addItem(choice));
            if (listenerFoAnyEditChanges != null) {
                propertyChoiceValueField.addItemListener(e -> listenerFoAnyEditChanges.actionEvent());
            }
        } else if (meta.getPropertyDataType() == java.lang.Integer.class || meta.getPropertyDataType() == java.lang.Long.class) {
            // NUMERIC INPUT
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            this.propertyValueField = new JFormattedTextField(amountFormat);
            if (!componentInitialisation && listenerFoAnyEditChanges != null) {
                this.propertyValueField.getDocument().addDocumentListener(new DocumentListener() {
                    // @See ComponentPropertiesPanel#editBoxChangeListener()
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        listenerFoAnyEditChanges.actionEvent();
                    }
                });
            }
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            // BOOLEAN INPUT
            propertyBooleanFieldTrue = new JCheckBox();
            propertyBooleanFieldFalse = new JCheckBox();
            propertyBooleanFieldTrue.setBackground(JBColor.WHITE);
            propertyBooleanFieldFalse.setBackground(JBColor.WHITE);
            propertyBooleanFieldTrue.addActionListener(e -> {
                if (propertyBooleanFieldTrue.isSelected() && propertyBooleanFieldFalse.isSelected()) {
                    propertyBooleanFieldFalse.setSelected(false);
                }
                if (listenerFoAnyEditChanges != null) {
                    listenerFoAnyEditChanges.actionEvent();
                }
            });
            propertyBooleanFieldFalse.addActionListener(e -> {
                if (propertyBooleanFieldFalse.isSelected() && propertyBooleanFieldTrue.isSelected()) {
                    propertyBooleanFieldTrue.setSelected(false);
                }
                if (listenerFoAnyEditChanges != null) {
                    listenerFoAnyEditChanges.actionEvent();
                }
            });
        } else {
            // STRING INPUT
            this.propertyValueField = new JFormattedTextField();

            // For list, allow comma seperated entry then convert to/from at start/end
            if (meta.getUsageDataType().equals(STRING_LIST)) {
                isList = true;
            }

            if (!componentInitialisation && listenerFoAnyEditChanges != null) {
                this.propertyValueField.getDocument().addDocumentListener(new DocumentListener() {
                    // @See ComponentPropertiesPanel#editBoxChangeListener()
                    @Override
                    public void insertUpdate(DocumentEvent e) { listenerFoAnyEditChanges.actionEvent(); }
                    @Override
                    public void removeUpdate(DocumentEvent e) { listenerFoAnyEditChanges.actionEvent(); }
                    @Override
                    public void changedUpdate(DocumentEvent e) { listenerFoAnyEditChanges.actionEvent(); }
                });
            }

            // These fields attempt to take their default value from another e.g. a bespoke method named after the propertyName
            if (componentPropertyEditBoxMap!= null && meta.getDefaultValue() != null && meta.getDefaultValue().toString().startsWith("__fieldName:")) {

                String[] components = componentProperty.getDefaultValue().toString().split(":");
                if (components.length > 1) {
                    String targetComponentName = components[1];
                    ComponentPropertyEditBox targetComponentPropertyEditBox = componentPropertyEditBoxMap.get(targetComponentName);
                    if (targetComponentPropertyEditBox != null) {
                        targetComponentPropertyEditBox.registerActionListener(event -> {
                            if (propertyValueField.getValue() == null || propertyValueField.getText().isBlank()) {
                                propertyValueField.setValue(targetComponentPropertyEditBox.getValue());
                            }
                        });
                        targetComponentPropertyEditBox.registerFocusListener(new FocusListener() {
                            @Override
                            public void focusGained(FocusEvent e) { }

                            @Override
                            public void focusLost(FocusEvent e) {
                                if (propertyValueField.getValue() == null || propertyValueField.getText().isBlank()) {
                                    propertyValueField.setValue(targetComponentPropertyEditBox.getValue());
                                }
                            }
                        });
                    }
                }
            }
        }
        resetDataEntryComponentsWithNewValues();
        propertyTitleField.setToolTipText(componentProperty.getMeta().getHelpText());
        if (componentProperty.getMeta().getDataValidationType() != null) {
            dataValidationHelper = new JButton();
            dataValidationHelper.setIcon(IkasanComponentLibrary.getSmallHelpIcon("Help with cron configuration"));
            dataValidationHelper.setBorder(new EmptyBorder(5, 15, 5, 15));
            dataValidationHelper.addActionListener(e -> doDataValidationHelperPopup());
        } else {
            dataValidationHelper = null;
        }

        if (componentProperty.affectsUserImplementedClass() && !componentInitialisation) {
            affectsUserImplementedClass = true;
            // Cant edit unless the regenerateSource is selected
            controlFieldsAffectingUserImplementedClass(false);
        }
    }

    /**
     * Register an action listener (press enter) for the input field, currently this would only make sense for the propertyValueField
     * Its typical use might be to default another field to the value entered in this field.
     * @param listener
     */
    public void registerActionListener(ActionListener listener) {
        if (propertyValueField != null) {
            propertyValueField.addActionListener(listener);
        }
    }
    /**
     * Register a focus listener (tab off) for the input field, currently this would only make sense for the propertyValueField
     * Its typical use might be to default another field to the value entered in this field.
     * @param listener
     */
    public void registerFocusListener(FocusListener listener) {
        if (propertyValueField != null) {
            propertyValueField.addFocusListener(listener);
        }
    }

    private void doDataValidationHelperPopup() {
        CronPanel cronPanel = new CronPanel(projectKey, (String)getValue());
            CronPopupDialogue cronPopupDialogue = new CronPopupDialogue(
                    projectKey,
                    UiContext.getDesignerCanvas(projectKey),
                    cronPanel);
            if (cronPopupDialogue.showAndGet()) {
                componentProperty.setValue(cronPanel.getValue());
                resetDataEntryComponentsWithNewValues();
            }
    }

    private String getListAsText(List<String> stringList) {
        String returnValue = "";
        if (stringList != null) {
            returnValue = getListAsText(stringList.toString());
        }
        return returnValue;
    }

    private String getListAsText(String bracketedCommList) {
        String returnValue = "";
        if (bracketedCommList != null) {
            returnValue = bracketedCommList.replace("[", "").replace("]", "");
        }
        return returnValue;
    }

    /**
     * UperImplementedClasses are classes that Studio will create the stub for, with the intention that the
     * user will complete the implementation.
     * When some properties are set, those values might affect a UserImplementedClass, so we only
     * enable those fields when the user explicit elects to do so
     * @param enable any fields that are tagged as affecting user implemented classes.
     */
    public void controlFieldsAffectingUserImplementedClass(boolean enable) {
        if (affectsUserImplementedClass) {
            if (propertyChoiceValueField != null) {
                propertyChoiceValueField.setEnabled(enable);
                // If the regenerate code is disabled, reset the input boxes
                if (!enable && propertyValueHasChanged()) {
                    propertyChoiceValueField.setSelectedItem(componentProperty.getValue());
                }
            } else if (propertyValueField != null) {
                propertyValueField.setEditable(enable);
                propertyValueField.setEnabled(enable);
                // If the regenerate code is disabled, reset the input boxes
                if (!enable && propertyValueHasChanged()) {
                    if (isList) {
                        propertyValueField.setValue(getListAsText((String)componentProperty.getValue()));
                    } else {
                        propertyValueField.setValue(componentProperty.getValue());
                    }
                }
            } else if (propertyBooleanFieldTrue != null) {
                propertyBooleanFieldTrue.setEnabled(enable);
                propertyBooleanFieldFalse.setEnabled(enable);
                if (!enable && propertyValueHasChanged()) {
                    Boolean oldValue = (Boolean)componentProperty.getValue();
                    propertyBooleanFieldTrue.setSelected(oldValue);
                    propertyBooleanFieldFalse.setSelected(!oldValue);
                }
            }
        }
    }

    /**
     * For a simple property, the key IS the property name.
     * @return the key for this property.
     */
    public String getPropertyKey() { return componentProperty.getMeta().getPropertyName(); }

    public boolean isChoiceProperty() {
        return propertyChoiceValueField != null;
    }
    public boolean isBooleanProperty() {
        return propertyBooleanFieldTrue != null;
    }

    public ComponentInput getInputField() {
        ComponentInput componentInput = null;
        if (meta.getPropertyDataType() == null && meta.getUsageDataType() == null) {
            // there is no value to enter, just a label to display
            LOG.info("STUDIO: NOTE: Not data type detected, no componentInput box generated");
        } else if (isChoiceProperty()) {
            componentInput = new ComponentInput(propertyChoiceValueField);
        } else if (isBooleanProperty()) {
            componentInput = new ComponentInput(propertyBooleanFieldTrue, propertyBooleanFieldFalse);
        } else {
            componentInput = new ComponentInput(propertyValueField);
        }
        if (meta.isReadOnlyProperty() && componentInput != null) {
            componentInput.setEnabled(false);
        }
        return componentInput;
    }

    public void setDefaultValue() {
        if (componentProperty != null && componentProperty.getDefaultValue() != null) {
            componentProperty.setValue(componentProperty.getDefaultValue());
            resetDataEntryComponentsWithNewValues();
        }
    }

    public void resetDataEntryComponentsWithNoValue() {
        if (meta.getChoices() != null) {
            propertyChoiceValueField.setSelectedItem(null);
        } else if (meta.getPropertyDataType() == java.lang.Integer.class || meta.getPropertyDataType() == java.lang.Long.class) {
            this.propertyValueField.setValue(null);
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            propertyBooleanFieldTrue.setSelected(false);
            propertyBooleanFieldFalse.setSelected(false);
        } else {
            this.propertyValueField.setText(null);
        }
    }

    public void resetDataEntryComponentsWithNewValues() {
        Object value = componentProperty.getValue();
        if (meta.getChoices() != null) {
            if (componentProperty.getValue() != null) {
                propertyChoiceValueField.setSelectedItem(componentProperty.getValue());
            }
        } else if (meta.getPropertyDataType() == java.lang.Integer.class || meta.getPropertyDataType() == java.lang.Long.class) {
            // NUMERIC INPUT
            if (value != null) {
                // Coming from a property this may not be the correct type yet
                if (value instanceof String) {
                    if (((String) value).isEmpty()) {
                        value = 0;
                    } else {
                        value = Integer.valueOf((String) value);
                    }
                }
                this.propertyValueField.setValue(value);
            }
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            if (value != null) {
                // Defensive, just in case not set correctly
                if (value instanceof String) {
                    if (((String) value).isBlank()) {
                        value = Boolean.FALSE;
                    } else {
                        value = Boolean.valueOf((String) value);
                    }
                }
                // Now we can be sure valuue is Boolean
                if (value instanceof Boolean) {
                    if ((Boolean)value) {
                        propertyBooleanFieldTrue.setSelected(true);
                    } else {
                        propertyBooleanFieldFalse.setSelected(true);
                    }
                }
            }
        } else {
            if (value != null) {
                if (isList) {
                    String strValue;
                    if (value instanceof List<?>) {
                        strValue = getListAsText((String) ((List)value).stream().map(Object::toString).collect(Collectors.joining(",")));
                    } else {
                        strValue = getListAsText((String)value);
                    }
                    this.propertyValueField.setText(strValue);
                } else {
                    this.propertyValueField.setText(value.toString());
                }
            }
        }
    }

    /**
     * Given the class of the property, return a value of the appropriate type.
     * @return the value of the property updated by the user.
     */
    public Object getValue() {
        Object returnValue = null;
        if (isChoiceProperty()) {
            returnValue = propertyChoiceValueField.getSelectedItem();
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            // It is possible neither are current selected i.e. the property is unset
            if (isBooleanProperty() && propertyBooleanFieldTrue.isSelected()) {
                returnValue = true;
            } else if (propertyBooleanFieldFalse != null && propertyBooleanFieldFalse.isSelected()) {
                returnValue = false;
            }
        } else if (meta.getUsageDataType().equals(STRING_LIST)) {
            String rawValue = (String)propertyValueField.getValue();
            // Bug workaround
            if (rawValue == null) {
                rawValue = propertyValueField.getText();
            }

            List<String> rawList = Arrays.asList(rawValue.split("\\s*,\\s*"));
            Set<String> deduplicate = new HashSet<>(rawList);
            if (rawList.size() > deduplicate.size()) {
                StudioUIUtils.displayIdeaWarnMessage(projectKey, "Duplicates in the list will be removed");
                returnValue = new ArrayList<>(deduplicate);
            } else {
                returnValue = rawList;
            }
        } else if (meta.getPropertyDataType() == java.lang.String.class) {
            // The formatter would be null if this was a standard text field.
            returnValue = propertyValueField.getText();
        } else {
            returnValue = propertyValueField.getValue();
        }
        return returnValue;
    }

    /**
     * Get the value of the propertyInputField
     * @return the value of the property updated by the user.
     */
    public Object getRawEntered() {
        if (meta.getUsageDataType().equals(STRING_LIST)) {
            String commmaList = propertyValueField.getText();
            if (commmaList != null) {
                commmaList = commmaList.replace("[", "").replace("]", "");
                return commmaList;
            } else {
                return null;
            }
        } else {
            return getValue();
        }
    }

    /**
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    public boolean inputfieldIsUnset() {
        boolean fieldNotSet = false;
        // For boolean we don't current support unset @todo support unset if we need to

        Object value = getValue();
        if (value == null) {
            fieldNotSet = true;
        } else if (meta.getPropertyDataType() == java.lang.String.class) {
            fieldNotSet = ((String)value).isEmpty();
        } else if (meta.getPropertyDataType() == java.lang.Long.class || meta.getPropertyDataType() == java.lang.Integer.class) {
            if (value instanceof Long) {
                fieldNotSet = ((Long)value == 0);
            } else if (value instanceof Integer) {
                fieldNotSet = ((Integer)value == 0);
            }
        }
        return fieldNotSet;
    }

    /**
     * Validates the values populated
     * @return a populated ValidationInfo array if there are any validation issues.
     */
    protected java.util.List<ValidationInfo> doValidateAll() {
        //@todo setup once in class and clear down
        List<ValidationInfo> result = new ArrayList<>();
        // 1. force population of mandatory properties
        if (meta.isMandatory() &&
                !isBooleanProperty() &&
                inputfieldIsUnset()) {
            result.add(new ValidationInfo(componentProperty.getMeta().getPropertyName() + " must be set to a valid value", getOverridingInputField()));
        }
        // 2. Apply a regex validation pattern as defined in the component's meta pack definition
        if (meta.getPropertyDataType() == java.lang.String.class && meta.getValidationPattern() != null && propertyValueHasChanged()) {
            String valueToBeChecked;
            // Currently, lists are being entered as comma separated values.
            if (meta.getUsageDataType() != null && STRING_LIST.equals(meta.getUsageDataType())) {
                valueToBeChecked = propertyValueField.getText();
            } else {
                valueToBeChecked = (String) getValue();
            }
            if (!meta.getValidationPattern().matcher(valueToBeChecked).matches()) {
                result.add(new ValidationInfo(meta.getValidationMessage(), getOverridingInputField()));
            }
        }
        return result;
    }

    /**
     * Determine if the edit box has a valid value
     * @return true if the editbox has a non-whitespace / real value.
     */
    boolean editBoxHasValue() {
        boolean hasValue = false;

        Object value = getValue();
        if (value instanceof String) {
            if (!((String) value).isEmpty()) {
                hasValue = true;
            }
        } else {
            hasValue = (value != null);
        }

        return hasValue;
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public ComponentProperty updateValueObjectWithEnteredValues() {
        componentProperty.setValue(getValue());
        return componentProperty;
    }

    /**
     * Determine if the data entered differs from the value object (componentProperty)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
        Object currentValue = componentProperty.getValue();
        Object enteredValue = getValue();
        return ((currentValue == null && editBoxHasValue()) ||
                (currentValue != null && !currentValue.equals(enteredValue)));
    }


    public JLabel getPropertyTitleField() {
        return propertyTitleField;
    }
    public JButton getDataValidationHelper() {
        return dataValidationHelper;
    }
    public JFormattedTextField getOverridingInputField() {
        return propertyValueField;
    }

    public ComponentPropertyMeta getMeta() {
        return meta;
    }
    public boolean isMandatory() {
        return meta.isMandatory();
    }
    public ComponentProperty getComponentProperty() {
        return componentProperty;
    }

    public boolean isAffectsUserImplementedClass() {
        return affectsUserImplementedClass;
    }
}
