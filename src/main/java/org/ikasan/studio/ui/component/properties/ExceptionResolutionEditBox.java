package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentPropertyInstance;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.instance.IkasanExceptionResolution;
import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolutionMeta;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolutionEditBox {
    private final ExceptionResolutionPanel resolutionPanel;
    private final JLabel exceptionTitleField;
    private final JComboBox<String> exceptionJComboBox;
    private final JLabel actionTitleField;
    private String currentAction = null;
    private final JComboBox<String> actionJComboBox;
    private final JLabel paramsTitleField;
    private List<ComponentPropertyEditBox> actionParamEditBoxList = new ArrayList<>();
    private final boolean componentInitialisation;
    private final IkasanExceptionResolution ikasanExceptionResolution;

    public ExceptionResolutionEditBox(ExceptionResolutionPanel resolutionPanel, IkasanExceptionResolution ikasanExceptionResolution, boolean componentInitialisation) {
        this.resolutionPanel = resolutionPanel;
        this.ikasanExceptionResolution = ikasanExceptionResolution ;
        this.componentInitialisation = componentInitialisation;

        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");
        List<String> currentExceptions = IkasanExceptionResolutionMeta.getStandardExceptionsList();
        this.exceptionJComboBox = new JComboBox(currentExceptions.toArray());
        this.exceptionJComboBox.setEditable(true);
        this.exceptionTitleField = new JLabel("Exception");
        this.actionJComboBox = new JComboBox(IkasanComponentLibrary.getExceptionResolver(IkasanComponentLibrary.STD_IKASAN_PACK).getActionList().toArray());
        if (ikasanExceptionResolution.getExceptionsCaught() != null) {
            // There might be a bespoke exception already set
            if (!currentExceptions.contains(ikasanExceptionResolution.getExceptionsCaught())) {
                currentExceptions.add(ikasanExceptionResolution.getExceptionsCaught());
            }
            exceptionJComboBox.setSelectedItem(ikasanExceptionResolution.getExceptionsCaught());
        }
        
        if (ikasanExceptionResolution.getTheAction() != null) {
            currentAction = ikasanExceptionResolution.getTheAction();
            actionJComboBox.setSelectedItem(currentAction);
            if (!ikasanExceptionResolution.getParams().isEmpty()) {
                actionParamEditBoxList = new ArrayList<>();
                for (IkasanComponentPropertyInstance property : ikasanExceptionResolution.getParams()) {
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(property, this.componentInitialisation);
                    actionParamEditBoxList.add(actionParam);
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
            actionParamEditBoxList = new ArrayList<>();
            for (IkasanComponentPropertyMeta propertyMeta : IkasanExceptionResolution.getMetaForActionParams(actionSelected)) {
                if (!propertyMeta.isVoid()) {
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(new IkasanComponentPropertyInstance(propertyMeta), this.componentInitialisation);
                    actionParamEditBoxList.add(actionParam);
                }
            }

            currentAction = (String)actionJComboBox.getSelectedItem();
            resolutionPanel.populatePropertiesEditorPanel();
            resolutionPanel.redrawPanel();
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public IkasanExceptionResolution updateValueObjectWithEnteredValues() {
        String theException = (String)exceptionJComboBox.getSelectedItem();
        if (theException != null && ! theException.endsWith(".class")) {
            theException += ".class";
        }
        ikasanExceptionResolution.setExceptionsCaught(theException);
        ikasanExceptionResolution.setTheAction((String)actionJComboBox.getSelectedItem());
        if (!actionParamEditBoxList.isEmpty()) {
            List<IkasanComponentPropertyInstance> newActionParams = new ArrayList<>();
            ikasanExceptionResolution.setParams(newActionParams);
            for (ComponentPropertyEditBox componentPropertyEditBox : actionParamEditBoxList) {
                newActionParams.add(componentPropertyEditBox.updateValueObjectWithEnteredValues());
            }
        }

        return ikasanExceptionResolution;
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
            if (actionParamEditBoxList.isEmpty()) {
                hasValue = true;
            } else {

                // Either all mandatory fields have value, or at least 1 non mandatory field has value
                boolean nonMandatoryHasValue = false;
                boolean hasNonMandatory = false;
                boolean mandatoryHasValue = true;
                for(ComponentPropertyEditBox editBox : actionParamEditBoxList) {
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
     * Determine if the data entered differs from the value object (ikasanExceptionResoluition)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
        boolean hasChanged = false;

        String selectedException = (String) exceptionJComboBox.getSelectedItem();
        String selectedAction = (String) actionJComboBox.getSelectedItem();

        if (editBoxHasValue()) {
            if (!selectedException.equals(ikasanExceptionResolution.getExceptionsCaught()) ||
                    !selectedAction.equals(ikasanExceptionResolution.getTheAction())) {
                hasChanged = true;
            } else if (selectedAction.equals(ikasanExceptionResolution.getTheAction())) {
                // Check to see if the values of any action params has changed
                for (ComponentPropertyEditBox componentPropertyEditBox : actionParamEditBoxList) {
                    if (componentPropertyEditBox.propertyValueHasChanged()) {
                        hasChanged = true;
                        break;
                    }
                }
            }
        } else if (ikasanExceptionResolution.getExceptionsCaught() != null || ikasanExceptionResolution.getTheAction() != null){
            hasChanged = true;
        }
        return hasChanged;
    }

    /**
     * get the key for the exception resolution
     * @return the key for this exception resolution
     */
    public String getPropertyKey() {
        return (String)exceptionJComboBox.getSelectedItem();
    }

    /**
     * actionParams will only have elements if an action has been chosen the requires params.
     * @return
     */
    public boolean actionHasParams() {
        return !actionParamEditBoxList.isEmpty();
    }
    public String getAction() {
        return (String)actionJComboBox.getSelectedItem();
    }

    public List<ComponentPropertyEditBox> getActionParamsEditBoxList() {
        return actionParamEditBoxList;
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
        } else if (!IkasanExceptionResolutionMeta.isValidAction((String) actionJComboBox.getSelectedItem())) {
            result.add(new ValidationInfo("The action " + actionJComboBox.getSelectedItem() + " is not recognised"));
        } else {
            // By now the action params should have been set (if there are any)
            if (!actionParamEditBoxList.isEmpty()) {
                for (ComponentPropertyEditBox param : actionParamEditBoxList) {
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
