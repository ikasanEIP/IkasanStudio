package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.ExceptionActionMeta;
import org.ikasan.studio.core.model.ikasan.meta.ExceptionResolverMeta;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolutionEditBox {
    private final String projectKey;
    ExceptionResolverMeta exceptionResolverMeta;
    private final ExceptionResolutionPanel resolutionPanel;
    private final JLabel exceptionTitleField;
    private final JComboBox exceptionJComboBox;
    private final JLabel actionTitleField;
    private String currentAction = null;
    private final JComboBox actionJComboBox;
    private final JLabel paramsTitleField;
    private List<ComponentPropertyEditBox> componentPropertyEditBoxList = new ArrayList<>();
    private final boolean componentInitialisation;
    private final ExceptionResolution exceptionResolution;

    public ExceptionResolutionEditBox(String projectKey, ExceptionResolverMeta exceptionResolverMeta, ExceptionResolutionPanel resolutionPanel, ExceptionResolution exceptionResolution, boolean componentInitialisation) {
        this.projectKey = projectKey;
        this.exceptionResolverMeta = exceptionResolverMeta;
        this.resolutionPanel = resolutionPanel;
        this.exceptionResolution = exceptionResolution;
        this.componentInitialisation = componentInitialisation;

        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");

        List<String> currentExceptions = exceptionResolverMeta.getExceptionsCaught();
        Object[] exceptions = currentExceptions.toArray();
        Object[] actions = exceptionResolverMeta.getActionList().stream()
                .map(ExceptionActionMeta::getActionName)
                .toArray();

        this.actionJComboBox = new JComboBox(actions);
        this.exceptionJComboBox = new JComboBox(exceptions);

        this.exceptionJComboBox.setEditable(true);
        this.exceptionTitleField = new JLabel("Exception");
        if (exceptionResolution.getExceptionsCaught() != null) {
            // There might be a bespoke exception already set
            if (!currentExceptions.contains(exceptionResolution.getExceptionsCaught())) {
                currentExceptions.add(exceptionResolution.getExceptionsCaught());
            }
            exceptionJComboBox.setSelectedItem(exceptionResolution.getExceptionsCaught());
        }

        // Setup the box for update if they already exist
        if (exceptionResolution.getTheAction() != null) {
            currentAction = exceptionResolution.getTheAction();
            actionJComboBox.setSelectedItem(currentAction);
            if (!exceptionResolution.getComponentProperties().isEmpty()) {
                componentPropertyEditBoxList = new ArrayList<>();
                for (ComponentProperty property : exceptionResolution.getComponentProperties().values()) {
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(projectKey, property, this.componentInitialisation, null);
                    componentPropertyEditBoxList.add(actionParam);
                }
            }
        }
        
        actionJComboBox.addActionListener (e ->
            setNewActionParams()
        );
    }

    /**
     * Once the action has been chosen, need to determine if there are new params for the action, if so add them and redraw.
     */
    private void setNewActionParams() {
        String actionSelected = (String)actionJComboBox.getSelectedItem();
        if (actionSelected != null && ! actionSelected.equals(currentAction)) {
            currentAction = (String)actionJComboBox.getSelectedItem();
            componentPropertyEditBoxList = new ArrayList<>();
            ExceptionActionMeta exceptionActionMeta = exceptionResolverMeta.getExceptionActionWithName(currentAction);

            if (exceptionActionMeta != null) {
                for (ComponentPropertyMeta propertyMeta : exceptionActionMeta.getActionProperties().values()) {
                    if (!propertyMeta.isVoid()) {
                        ComponentProperty newActionProperty = new ComponentProperty(propertyMeta);
//                        ComponentPropertyMeta componentPropertyMeta = exceptionActionMeta.getMetaProperty(fieldName);
                        // The property has to be added to this exception resolution so that it can be updated later.
                        exceptionResolution.addComponentProperty(newActionProperty.getMeta().getPropertyName(), newActionProperty);
                        ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(projectKey, newActionProperty, this.componentInitialisation, null);
                        componentPropertyEditBoxList.add(actionParam);
                    }
                }
            }
            resolutionPanel.populatePropertiesEditorPanel();
            resolutionPanel.redrawPanel();
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public void updateValueObjectWithEnteredValues() {
        String theException = (String)exceptionJComboBox.getSelectedItem();
        if (theException != null && ! theException.endsWith(".class")) {
            theException += ".class";
        }
        exceptionResolution.setExceptionsCaught(theException);
        exceptionResolution.setTheAction((String)actionJComboBox.getSelectedItem());
        if (!componentPropertyEditBoxList.isEmpty()) {
            for (ComponentPropertyEditBox componentPropertyEditBox : componentPropertyEditBoxList) {
                componentPropertyEditBox.updateValueObjectWithEnteredValues();
            }
        }
    }


    /**
     * Determine if the edit box has values in all mandatory fields.
     * @return true if the editbox has a non-whitespace / real value.
     */
    public boolean editBoxHasValue() {
        boolean hasValue = false;

        String selectedException = (String)exceptionJComboBox.getSelectedItem();
        String selectedAction = (String)actionJComboBox.getSelectedItem();
        if (selectedException != null && ! selectedException.isEmpty() &&
            selectedAction != null && ! selectedAction.isEmpty()) {
            if (componentPropertyEditBoxList.isEmpty()) {
                hasValue = true;
            } else {

                // Either all mandatory fields have value, or at least 1 non-mandatory field has value
                boolean nonMandatoryHasValue = false;
                boolean hasNonMandatory = false;
                boolean mandatoryHasValue = true;
                for(ComponentPropertyEditBox editBox : componentPropertyEditBoxList) {
                    if (!editBox.isMandatory()) {
                        hasNonMandatory = true ;
                    }
                    if (editBox.isMandatory() && !editBox.editBoxHasValue()) {
                        mandatoryHasValue = false;
                        break;
                    } else if (editBox.editBoxHasValue() && !editBox.isMandatory()) {
                        nonMandatoryHasValue = true;
                    }
                }
                if (mandatoryHasValue || (hasNonMandatory && nonMandatoryHasValue)) {
                    hasValue = true;
                }
            }
        }
        return hasValue;
    }

    /**
     * Determine if the data entered differs from the value object (ikasanExceptionResolution)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
        boolean hasChanged = false;

        String selectedException = (String) exceptionJComboBox.getSelectedItem();
        String selectedAction = (String) actionJComboBox.getSelectedItem();

        if (editBoxHasValue() && selectedAction != null && selectedException != null) {
            if (!selectedException.equals(exceptionResolution.getExceptionsCaught()) ||
                    !selectedAction.equals(exceptionResolution.getTheAction())) {
                hasChanged = true;
            } else if (selectedAction.equals(exceptionResolution.getTheAction())) {
                // Check to see if the values of any action params has changed
                for (ComponentPropertyEditBox componentPropertyEditBox : componentPropertyEditBoxList) {
                    if (componentPropertyEditBox.propertyValueHasChanged()) {
                        hasChanged = true;
                        break;
                    }
                }
            }
        } else if (exceptionResolution.getExceptionsCaught() != null || exceptionResolution.getTheAction() != null){
            hasChanged = true;
        }
        return hasChanged;
    }

    public String getAction() {
        return (String)actionJComboBox.getSelectedItem();
    }

    public List<ComponentPropertyEditBox> getActionParamsEditBoxList() {
        return componentPropertyEditBoxList;
    }

    /**
     * Validate the selected values
     * @return a non-empty ValidationInfo list if there are validation errors
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = new ArrayList<>();
        if (exceptionJComboBox.getSelectedItem() == null) {
            result.add(new ValidationInfo("A valid exception must be set"));
        } else if (resolutionPanel.hasExceptionResolution((String)exceptionJComboBox.getSelectedItem())) {
            result.add(new ValidationInfo("The exception " + exceptionJComboBox.getSelectedItem() + " already has an assigned action, change the exception or cancel"));
        } else if (actionJComboBox.getSelectedItem() == null) {
            result.add(new ValidationInfo("An action must be chosen"));
        } else if (!ExceptionResolverMeta.isValidAction((String) actionJComboBox.getSelectedItem())) {
            result.add(new ValidationInfo("The action " + actionJComboBox.getSelectedItem() + " is not recognised"));
        } else {
            // By now the action params should have been set (if there are any)
            if (!componentPropertyEditBoxList.isEmpty()) {
                for (ComponentPropertyEditBox param : componentPropertyEditBoxList) {
                    result.addAll(param.doValidateAll());
                }
            }
        }
        return  result;
    }

    public JLabel getExceptionTitleField() {
        return exceptionTitleField;
    }

    public JComboBox<String> getExceptionJComboBox() {
        return exceptionJComboBox;
    }

    public JLabel getActionTitleField() {
        return actionTitleField;
    }

    public JComboBox<String> getActionJComboBox() {
        return actionJComboBox;
    }

    public JLabel getParamsTitleField() {
        return paramsTitleField;
    }
}
