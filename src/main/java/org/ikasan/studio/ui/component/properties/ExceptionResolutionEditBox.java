package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.model.ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolution;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolutionEditBox {
    private ExceptionResolutionPanel resolutionPanel;
//    private static IkasanExceptionResolutionMeta IKASAN_EXCEPTION_RESOLUTION_META = new IkasanExceptionResolutionMeta();
    private JLabel exceptionTitleField;
    private JComboBox<String> exceptionJComboBox;
    private JLabel actionTitleField;
    private String currentAction = null;
    private JComboBox<String> actionJComboBox;
    private JLabel paramsTitleField;
    private List<ComponentPropertyEditBox> actionParamEditBoxList = new ArrayList<>();
    private boolean popupMode;
    private IkasanExceptionResolution ikasanExceptionResolution;
//    private IkasanExceptionResolver ikasanExceptionResolver;

    public ExceptionResolutionEditBox(ExceptionResolutionPanel resolutionPanel, IkasanExceptionResolution ikasanExceptionResolution, boolean popupMode) {
        this.resolutionPanel = resolutionPanel;
        this.ikasanExceptionResolution = ikasanExceptionResolution ;
        this.popupMode = popupMode;

        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");
        List<String> currentExceptions = IkasanExceptionResolution.getMeta().getStandardExceptionsList();
        this.exceptionJComboBox = new JComboBox(currentExceptions.toArray());
        this.exceptionJComboBox.setEditable(true);
        this.exceptionTitleField = new JLabel("Exception");
        this.actionJComboBox = new JComboBox(IkasanExceptionResolution.getMeta().getActionList().toArray());
        if (ikasanExceptionResolution.getTheException() != null) {
            // There might be a bespoke exception already set
            if (!currentExceptions.contains(ikasanExceptionResolution.getTheException())) {
                currentExceptions.add(ikasanExceptionResolution.getTheException());
            }
            exceptionJComboBox.setSelectedItem(ikasanExceptionResolution.getTheException());
        }
        
        if (ikasanExceptionResolution.getTheAction() != null) {
            currentAction = ikasanExceptionResolution.getTheAction();
            actionJComboBox.setSelectedItem(currentAction);
            if (!ikasanExceptionResolution.getParams().isEmpty()) {
                actionParamEditBoxList = new ArrayList<>();
                for (IkasanComponentProperty property : ikasanExceptionResolution.getParams()) {
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(property, this.popupMode);
                    actionParamEditBoxList.add(actionParam);
                }
            }
        }
        
        actionJComboBox.addActionListener (e -> {
            setNewActionParams();
        });
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
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(new IkasanComponentProperty(propertyMeta), this.popupMode);
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
        ikasanExceptionResolution.setTheException((String)exceptionJComboBox.getSelectedItem());
        ikasanExceptionResolution.setTheAction((String)actionJComboBox.getSelectedItem());
        if (!actionParamEditBoxList.isEmpty()) {
            List newActionParams = new ArrayList<>();
            ikasanExceptionResolution.setParams(newActionParams);
            for (ComponentPropertyEditBox componentPropertyEditBox : actionParamEditBoxList) {
                newActionParams.add(componentPropertyEditBox.updateValueObjectWithEnteredValues());
            }
        }

//        ikasanExceptionResolver.getIkasanExceptionResolutionList().put(getPropertyKey(), ikasanExceptionResolution);
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
            if (!selectedException.equals(ikasanExceptionResolution.getTheException()) ||
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
        } else if (ikasanExceptionResolution.getTheException() != null || ikasanExceptionResolution.getTheAction() != null){
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
        } else if (ikasanExceptionResolution.getParent().hasExceptionResolution((String)exceptionJComboBox.getSelectedItem()) ||
                    resolutionPanel.hasExceptionResolution((String)exceptionJComboBox.getSelectedItem())) {
            result.add(new ValidationInfo("The exception " + exceptionJComboBox.getSelectedItem() + " already has an assigned action, change the exception or cancel"));
        } else if (actionJComboBox.getSelectedItem() == null) {
            result.add(new ValidationInfo("An action must be chosen"));
        } else if (!IkasanExceptionResolution.getMeta().isValidAction((String) actionJComboBox.getSelectedItem())) {
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