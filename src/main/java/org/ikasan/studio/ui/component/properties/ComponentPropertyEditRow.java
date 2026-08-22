package org.ikasan.studio.ui.component.properties;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.*;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ComponentPropertyEditRow {
    private static final Logger LOG = Logger.getInstance("#ComponentPropertyEditRow");
    private final JLabel propertyTitleField;
    private final JButton dataValidationHelper;
    private ComboBox<Object> propertyChoiceValueField;
    private JFormattedTextField propertyValueField;
    private JCheckBox propertyBooleanFieldTrue;
    private JCheckBox propertyBooleanFieldFalse;
    private final JLabel affectsUserImplementedClassIndicator;
    private boolean isList = false;
    // true when the field is currently showing the meta-declared default purely as a preview (e.g. after
    // "Set Defaults", or a boolean's always-shown default) rather than a value the user has genuinely chosen.
    // getValue() reports null while this is set, so previewing a default is never mistaken for a real edit and
    // silently persisted into the model / generated code.
    private boolean showingDefaultOnly = false;
    // Guards the field listeners while we programmatically populate a preview, so that write doesn't itself
    // clear showingDefaultOnly (Swing fires DocumentListener/ItemListener on programmatic changes too, unlike
    // JCheckBox's ActionListener which only fires on genuine user clicks).
    private boolean suppressChangeDetection = false;
    // True while this field's value was derived automatically from another field (see the "__fieldName:" Default
    // button below) rather than typed directly by the user - while true, the derived field keeps tracking every
    // change to its source field live. A genuine keystroke in this field clears it permanently, the same way
    // showingDefaultOnly is cleared, so a deliberate override is never silently clobbered by further auto-sync.
    private boolean autoDerivedValue = false;
    private JButton defaultValueButton;
    private JCheckBox rowOverwriteCheckBox;
    private final ComponentPropertyMeta meta;
    private final ComponentProperty componentProperty;
    private final Project project;
    private final Object initialValue;

    /**
     * Constructor
     * @param project the users' current Java project
     * @param componentProperty to be exposed for edit
     * @param componentInitialisation only default the value if this is true
     */
    public ComponentPropertyEditRow(Project project, ComponentProperty componentProperty, boolean componentInitialisation) {
        this(project, componentProperty, componentInitialisation, null, null);
    }

    /**
     * Constructor
     * @param project the users' current Java project
     * @param componentProperty to be exposed for edit
     * @param componentInitialisation only default the value if this is true
     * @param listenerFoAnyEditChanges used by the parent to detect changes to the values being edited. If this is not required, use the other constructor.
     * @param componentPropertyEditBoxMap a growing list of propertyName -> ComponentPropertyEditRow, this instance will be added to it in the constructor.
     *                                    This is needed if fields could default off each other. If this is not required, use the other constructor.
     */
    public ComponentPropertyEditRow(Project project, ComponentProperty componentProperty, boolean componentInitialisation, SimpleChangeListener listenerFoAnyEditChanges, Map<String, ComponentPropertyEditRow> componentPropertyEditBoxMap) {
        this.project = project;
        this.componentProperty = componentProperty;
        this.initialValue = componentProperty.getValue();
        String labelText = componentProperty.getMeta().getDisplayLabel() != null ? componentProperty.getMeta().getDisplayLabel() : componentProperty.getMeta().getPropertyName();
        this.propertyTitleField = new JLabel(labelText);
        this.meta = componentProperty.getMeta();
        if (componentPropertyEditBoxMap != null) {
            componentPropertyEditBoxMap.put(getPropertyKey(), this);
        }

        if (    componentInitialisation &&
                componentProperty.getValue() == null &&
                !meta.isOptional() &&
                meta.getDefaultValue() != null &&
                !ComponentPropertyMeta.isSubstitutionValue(meta.getDefaultValue())) {
            componentProperty.setValue(componentProperty.getDefaultValue());
        }

        // @todo we can have all types of components with rich pattern matching validation
        if (meta.getChoices() != null) {
            propertyChoiceValueField = new ComboBox<>();
            if (meta.isOptional()) {
                propertyChoiceValueField.addItem("");
            }
            meta.getChoices()
                .forEach( choice -> propertyChoiceValueField.addItem(choice));
            if (listenerFoAnyEditChanges != null) {
                propertyChoiceValueField.addItemListener(e -> {
                    if (!suppressChangeDetection) {
                        showingDefaultOnly = false;
                    }
                    listenerFoAnyEditChanges.actionEvent();
                });
            }
        } else if (meta.getPropertyDataType() == java.lang.Integer.class || meta.getPropertyDataType() == java.lang.Long.class) {
            // NUMERIC INPUT
            NumberFormat amountFormat = NumberFormat.getNumberInstance();
            // Grouping is on by default, which renders values like a port or timeout as "2,121" - not wanted for any property here.
            amountFormat.setGroupingUsed(false);
            this.propertyValueField = new JFormattedTextField(amountFormat);
            if (listenerFoAnyEditChanges != null) {
                this.propertyValueField.getDocument().addDocumentListener(new DocumentListener() {
                    // @See ComponentPropertiesPanel#editBoxChangeListener()
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                });
            }
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            boolean isMandatory = componentProperty.getMeta().isMandatory();
            // BOOLEAN INPUT
            propertyBooleanFieldTrue = new JCheckBox();
            propertyBooleanFieldFalse = new JCheckBox();
            propertyBooleanFieldTrue.setBackground(JBColor.WHITE);
            propertyBooleanFieldFalse.setBackground(JBColor.WHITE);
            propertyBooleanFieldTrue.addActionListener(e -> {
                // A genuine click - Swing never fires ActionListener for the programmatic .setSelected() calls
                // used to preview the default, so no suppressChangeDetection guard is needed here.
                showingDefaultOnly = false;
                if (propertyBooleanFieldTrue.isSelected() && propertyBooleanFieldFalse.isSelected()) {
                    propertyBooleanFieldFalse.setSelected(false);
                } else if (isMandatory && !propertyBooleanFieldTrue.isSelected()) {
                    propertyBooleanFieldFalse.setSelected(true);
                }
                if (listenerFoAnyEditChanges != null) {
                    listenerFoAnyEditChanges.actionEvent();
                }
            });
            propertyBooleanFieldFalse.addActionListener(e -> {
                showingDefaultOnly = false;
                if (propertyBooleanFieldFalse.isSelected() && propertyBooleanFieldTrue.isSelected()) {
                    propertyBooleanFieldTrue.setSelected(false);
                } else if (isMandatory && !propertyBooleanFieldFalse.isSelected()) {
                    propertyBooleanFieldTrue.setSelected(true);
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

            if (listenerFoAnyEditChanges != null) {
                this.propertyValueField.getDocument().addDocumentListener(new DocumentListener() {
                    // @See ComponentPropertiesPanel#editBoxChangeListener()
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                            autoDerivedValue = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                            autoDerivedValue = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        if (!suppressChangeDetection) {
                            showingDefaultOnly = false;
                            autoDerivedValue = false;
                        }
                        listenerFoAnyEditChanges.actionEvent();
                    }
                });
            }

            // For fields that derive their default value from another field, provide a "D" button to apply the default on demand.
            if (componentPropertyEditBoxMap != null && meta.getDefaultValue() != null && meta.getDefaultValue().toString().startsWith(SUBSTITUTION_FIELD_NAME)) {
                String[] parts = componentProperty.getDefaultValue().toString().split(SUBSTITUTION_NAME_VALUE_DELIM);
                String literal = parts.length > 2 ? parts[2] : "";
                if (parts.length > 1) {
                    String targetComponentName = parts[1];
                    ComponentPropertyEditRow targetComponentPropertyEditRow = componentPropertyEditBoxMap.get(targetComponentName);
                    if (targetComponentPropertyEditRow != null) {
                        String defaultButtonLabel = StudioBundle.message("button.Default");
                        String clearButtonLabel = StudioBundle.message("button.Clear");
                        String setDefaultTooltip = StudioBundle.message("message.SetDefaultValueDerivedFrom", targetComponentName);
                        defaultValueButton = new JButton(defaultButtonLabel);
                        defaultValueButton.setToolTipText(setDefaultTooltip);
                        defaultValueButton.addActionListener(e -> {
                            if (defaultButtonLabel.equals(defaultValueButton.getText())) {
                                if (propertyValueField.getValue() == null || propertyValueField.getText().isBlank()) {
                                    applyDerivedValue(targetComponentPropertyEditRow, literal);
                                    if (listenerFoAnyEditChanges != null) {
                                        listenerFoAnyEditChanges.actionEvent();
                                    }
                                }
                                // A deliberate click re-enables live tracking even if a prior direct edit had
                                // turned it off - the user is explicitly asking to follow the source field again.
                                autoDerivedValue = true;
                                defaultValueButton.setText(clearButtonLabel);
                                defaultValueButton.setToolTipText(StudioBundle.message("message.ClearTheDefaultValue"));
                            } else {
                                propertyValueField.setValue(null);
                                propertyValueField.setText("");
                                autoDerivedValue = false;
                                defaultValueButton.setText(defaultButtonLabel);
                                defaultValueButton.setToolTipText(setDefaultTooltip);
                            }
                        });

                        // Keep this field silently tracking its source field's live (not yet saved) edits, so the
                        // user never has to click Default at all - but only when this field is mandatory. A
                        // mandatory field always ends up needing some value, so auto-deriving one is pure
                        // convenience; a non-mandatory field left blank may be a deliberate "no user code needed"
                        // choice, and auto-populating it would silently turn that into a real, changed value the
                        // user never asked for - so those keep the manual Default button as their only path.
                        if (meta.isMandatory() && initialValue == null) {
                            autoDerivedValue = true;
                            targetComponentPropertyEditRow.addValueChangeListener(() -> {
                                if (autoDerivedValue) {
                                    applyDerivedValue(targetComponentPropertyEditRow, literal);
                                    defaultValueButton.setText(clearButtonLabel);
                                    defaultValueButton.setToolTipText(StudioBundle.message("message.ClearTheDefaultValue"));
                                    if (listenerFoAnyEditChanges != null) {
                                        listenerFoAnyEditChanges.actionEvent();
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        resetDataEntryComponentsWithNewValues();
        propertyTitleField.setToolTipText(componentProperty.getMeta().getHelpText());
        if (componentProperty.getMeta().getDataValidationType() != null) {
            dataValidationHelper = new JButton();
            dataValidationHelper.setIcon(IkasanComponentLibrary.getSmallHelpIcon(StudioBundle.message("tooltip.HelpWithCronConfiguration")));
            dataValidationHelper.setBorder(JBUI.Borders.empty(5, 15));
            dataValidationHelper.addActionListener(e -> doDataValidationHelperPopup());
        } else {
            dataValidationHelper = null;
        }

        if (componentProperty.affectsUserImplementedClass() && !componentInitialisation) {
            // Nothing to protect yet on first-time creation (componentInitialisation) - the indicator/confirmation
            // only matters once a user-implemented class stub may already exist to be regenerated.
            affectsUserImplementedClassIndicator = new JLabel(AllIcons.General.Warning);
            affectsUserImplementedClassIndicator.setToolTipText(StudioBundle.message("tooltip.AffectsUserImplementedClass"));
        } else {
            affectsUserImplementedClassIndicator = null;
        }

        // A bespoke, user-owned stub already exists for this property - offer a per-row checkbox to allow
        // regeneration, mirroring the component-level "Allow Update" checkbox but scoped to just this property.
        // Nothing to protect yet if the property has never been given a value.
        if (meta.isProtectFromOverwrite() && !componentInitialisation && componentProperty.getValue() != null) {
            rowOverwriteCheckBox = new JCheckBox();
            rowOverwriteCheckBox.setToolTipText(StudioBundle.message("tooltip.CheckTheBoxIfYouWishToRewriteOverwriteTheExistingCode"));
        }
    }

    /**
     * Resolve and write this field's value from a source field's current (possibly not yet saved) value plus any
     * literal suffix from a "__fieldName:X:literal" default, sanitised into a legal Java class name when this
     * property affects a generated user-implemented class - the same derivation the Default button always did,
     * now shared with the live auto-sync listener below. No-ops while the source field has nothing typed into it
     * yet. The write is guarded so it is never itself mistaken for a direct user edit (which would otherwise
     * clear showingDefaultOnly/autoDerivedValue); callers that need listeners re-notified after calling this
     * (the button click has no other row's listener to piggyback on) must do so themselves.
     */
    private void applyDerivedValue(ComponentPropertyEditRow sourceRow, String literal) {
        Object sourceValue = sourceRow.getValue();
        if (sourceValue == null || sourceValue.toString().isBlank()) {
            return;
        }
        String derivedValue = sourceValue + literal;
        if (componentProperty.affectsUserImplementedClass()) {
            derivedValue = StudioBuildUtils.toJavaClassName(derivedValue);
        }
        suppressChangeDetection = true;
        try {
            propertyValueField.setValue(derivedValue);
        } finally {
            suppressChangeDetection = false;
        }
    }

    /**
     * Notify the given listener whenever this row's live (not yet saved) value may have changed - lets a
     * dependent row (see the "__fieldName:" Default button above) track this field's edits as they happen. Only
     * supported for plain string fields, the only kind ever used as a "__fieldName:" target today.
     */
    public void addValueChangeListener(Runnable onChange) {
        if (propertyValueField != null) {
            propertyValueField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
            });
        }
    }

    private void doDataValidationHelperPopup() {
        CronPanel cronPanel = new CronPanel(project, (String)getValue());
            CronPopupDialogue cronPopupDialogue = new CronPopupDialogue(
                    project,
                    project.getService(UiContext.class).getDesignerCanvas(),
                    cronPanel);
            if (cronPopupDialogue.showAndGet()) {
                componentProperty.setValue(cronPanel.getValue());
                resetDataEntryComponentsWithNewValues();
            }
    }

    private String getListAsText(String bracketedCommList) {
        String returnValue = "";
        if (bracketedCommList != null) {
            returnValue = bracketedCommList.replace("[", "").replace("]", "");
        }
        return returnValue;
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

    /**
     * "Set Defaults" - every field now previews its declared default unconditionally (see
     * resetDataEntryComponentsWithNewValues()), so this just re-affirms that preview. Nothing is persisted
     * (and so nothing is written into generated code) unless the user goes on to genuinely interact with the
     * field - typing, ticking a checkbox, or picking a choice.
     */
    public void setDefaultValue() {
        resetDataEntryComponentsWithNewValues();
    }

    public void clearValue() {
        if (componentProperty != null) {
            componentProperty.setValue(null);
            showingDefaultOnly = false;
            if (meta.getChoices() != null) {
                propertyChoiceValueField.setSelectedItem("");
            } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
                if (propertyBooleanFieldTrue != null) propertyBooleanFieldTrue.setSelected(false);
                if (propertyBooleanFieldFalse != null) propertyBooleanFieldFalse.setSelected(false);
            } else {
                propertyValueField.setText("");
            }
        }
    }

    public void resetDataEntryComponentsWithNewValues() {
        Object value = componentProperty.getValue();
        if (meta.getChoices() != null) {
            if (componentProperty.getValue() != null) {
                propertyChoiceValueField.setSelectedItem(componentProperty.getValue());
                showingDefaultOnly = false;
            } else {
                Object defaultValue = previewableDefault();
                showingDefaultOnly = defaultValue != null;
                suppressChangeDetection = true;
                try {
                    propertyChoiceValueField.setSelectedItem(defaultValue != null ? defaultValue : (meta.isOptional() ? "" : null));
                } finally {
                    suppressChangeDetection = false;
                }
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
                showingDefaultOnly = false;
            } else {
                // Preview the declared default purely for display, same idea as the boolean fallback below -
                // useful for reviewing e.g. timeout defaults without committing them. showingDefaultOnly (and
                // suppressChangeDetection while writing the widget) keeps this from being mistaken for a
                // genuinely-entered value.
                Object defaultValue = previewableDefault();
                showingDefaultOnly = defaultValue != null;
                if (defaultValue != null) {
                    suppressChangeDetection = true;
                    try {
                        this.propertyValueField.setValue(defaultValue);
                    } finally {
                        suppressChangeDetection = false;
                    }
                } else {
                    this.propertyValueField.setValue(null);
                }
            }
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            // Fall back to the meta-declared default purely for display - the property itself stays
            // unset (componentProperty.getValue() still returns null) until the user actually interacts
            // with it. Without this, a never-set boolean (e.g. a brand new Flow's isRecording) left both
            // checkboxes in their fresh, unchecked Swing-default state instead of showing the declared
            // default (false) as selected. showingDefaultOnly tracks that so this preview is never mistaken
            // for a genuinely-chosen value.
            Object displayValue = value != null ? value : previewableDefault();
            showingDefaultOnly = (value == null && displayValue != null);
            if (displayValue != null) {
                // Defensive, just in case not set correctly
                if (displayValue instanceof String) {
                    if (((String) displayValue).isBlank()) {
                        displayValue = Boolean.FALSE;
                    } else {
                        displayValue = Boolean.valueOf((String) displayValue);
                    }
                }
                // Now we can be sure displayValue is Boolean
                if (displayValue instanceof Boolean) {
                    if ((Boolean) displayValue) {
                        propertyBooleanFieldTrue.setSelected(true);
                        propertyBooleanFieldFalse.setSelected(false);
                    } else {
                        propertyBooleanFieldFalse.setSelected(true);
                        propertyBooleanFieldTrue.setSelected(false);
                    }
                }
            } else {
                propertyBooleanFieldTrue.setSelected(false);
                propertyBooleanFieldFalse.setSelected(false);
            }
        } else {
            if (value != null) {
                if (isList) {
                    String strValue;
                    if (value instanceof List<?>) {
                        strValue = getListAsText(((List<?>)value).stream().<String>map(Object::toString).collect(Collectors.joining(",")));
                    } else {
                        strValue = getListAsText((String)value);
                    }
                    this.propertyValueField.setText(strValue);
                } else {
                    this.propertyValueField.setText(value.toString());
                }
                showingDefaultOnly = false;
            } else {
                // Preview the declared default purely for display - see the numeric branch above for why.
                Object defaultValue = previewableDefault();
                showingDefaultOnly = defaultValue != null;
                suppressChangeDetection = true;
                try {
                    this.propertyValueField.setText(defaultValue != null ? defaultValue.toString() : "");
                } finally {
                    suppressChangeDetection = false;
                }
            }
        }
    }

    /**
     * @return the meta-declared default for this property, or null if there isn't one usable for preview - either
     * because none is declared, or because it's a "__..." substitution placeholder (e.g. configurationId's
     * "__module-__flow-__component") meant to be resolved via the separate "Default"/derive button or the
     * generator's own substitution, not shown to the user as a literal string.
     */
    private Object previewableDefault() {
        Object defaultValue = componentProperty.getDefaultValue();
        return (defaultValue == null || ComponentPropertyMeta.isSubstitutionValue(defaultValue)) ? null : defaultValue;
    }

    /**
     * Given the class of the property, return a value of the appropriate type.
     * @return the value of the property updated by the user.
     */
    public Object getValue() {
        // The field may just be previewing the meta-declared default (a boolean's always-shown default, or
        // "Set Defaults") rather than holding a value the user has genuinely chosen - report unset until it is.
        if (showingDefaultOnly) {
            return null;
        }
        Object returnValue = null;
        if (isChoiceProperty()) {
            Object selected = propertyChoiceValueField.getSelectedItem();
            returnValue = (selected == null || "".equals(selected)) ? null : selected;
        } else if (meta.getPropertyDataType() == java.lang.Boolean.class) {
            // It is possible that neither are currently selected i.e. the property is unset
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
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DuplicatesInTheListWillBeRemoved"));
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
     * For the given field type, determine if a valid value has been set.
     * @return true if the field is empty or unset
     */
    public boolean inputfieldIsUnset() {
        boolean fieldNotSet = false;
        // For boolean we don't current support unset @todo support unset if we need to

        Object value = getValue();
        if (value == null) {
            fieldNotSet = true;
        } else if (isList) {
            List<?> listValue = (List<?>) value;
            fieldNotSet = listValue.isEmpty() ||
                    (listValue.size() == 1 && listValue.get(0) instanceof String && ((String)listValue.get(0)).isEmpty());
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
        Object enteredValue = getValue();
        return ((initialValue == null && editBoxHasValue()) ||
                (initialValue != null && !initialValue.equals(enteredValue)));
    }

    public Project getProject() { return project; }
    public JLabel getPropertyTitleField() {
        return propertyTitleField;
    }
    public JButton getDataValidationHelper() {
        return dataValidationHelper;
    }
    public JButton getDefaultValueButton() {
        return defaultValueButton;
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

    public JLabel getAffectsUserImplementedClassIndicator() {
        return affectsUserImplementedClassIndicator;
    }

    public JCheckBox getRowOverwriteCheckBox() {
        return rowOverwriteCheckBox;
    }

    public boolean isProtectedFromOverwrite() {
        return meta.isProtectFromOverwrite();
    }

    /**
     * @return true if this property's generated stub may be (re)written: either it never had a value before
     * (first-time generation, nothing to protect), or the user has explicitly ticked this row's checkbox.
     */
    public boolean isRowOverwriteAllowed() {
        return initialValue == null || (rowOverwriteCheckBox != null && rowOverwriteCheckBox.isSelected());
    }
}
