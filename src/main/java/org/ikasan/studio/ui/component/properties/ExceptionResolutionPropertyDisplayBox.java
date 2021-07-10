package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.model.ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolution;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolutionPropertyDisplayBox {
    private JLabel exceptionField;
    private JLabel actionField;
    private List<ComponentPropertyEditBox> actionParamEditBoxList = new ArrayList<>();
    private boolean popupMode;
    private IkasanExceptionResolution ikasanExceptionResolution;
    private JButton deleteButton;

    public ExceptionResolutionPropertyDisplayBox(IkasanExceptionResolution ikasanExceptionResolution, boolean popupMode) {
        this.ikasanExceptionResolution = ikasanExceptionResolution ;
        this.popupMode = popupMode;

        String theException = ikasanExceptionResolution.getTheException();
        String theAction = ikasanExceptionResolution.getTheAction();

        if (theException != null && !theException.isEmpty()) {
            exceptionField.setText(ikasanExceptionResolution.getTheException());
            deleteButton = new JButton("X");
            deleteButton.addActionListener(e -> {
                doDelete(e);
                }
            );
        }
        if (theAction != null && !theAction.isEmpty()) {
            actionField.setText(ikasanExceptionResolution.getTheAction());
        }
        if (ikasanExceptionResolution.getTheAction() != null) {
            if (!ikasanExceptionResolution.getParams().isEmpty()) {
                actionParamEditBoxList = new ArrayList<>();
                for (IkasanComponentProperty property : ikasanExceptionResolution.getParams()) {
                    ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(property, this.popupMode);
                    actionParamEditBoxList.add(actionParam);
                }
            }
        }
    }

    private void doDelete(ActionEvent ae) {

    }

//    /**
//     * Once the action has been chosen, need to determine if there are new params for the action, if so add them and redraw.
//     */
//    private void setNewActionParams() {
//        String actionSelected = (String)actionJComboBox.getSelectedItem();
//        if (actionSelected != null && ! actionSelected.equals(currentAction)) {
////            Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> actionProperties = getPropertiesForAction(actionSelected);
//            actionParamEditBoxList = new ArrayList<>();
//            for (IkasanComponentPropertyMeta propertyMeta : IkasanExceptionResolution.getMetaForActionParams(actionSelected)) {
//                ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(new IkasanComponentProperty(propertyMeta), this.popupMode);
//                actionParamEditBoxList.add(actionParam);
//            }
//            dirty = true;
//            currentAction = (String)actionJComboBox.getSelectedItem();
//        }
//    }

//    /**
//     * Usually the final step of edit, update the original value object with the entered data
//     */
//    public IkasanExceptionResolution updateValueObjectWithEnteredValues() {
//        ikasanExceptionResolution.setTheException((String)exceptionJComboBox.getSelectedItem());
//        ikasanExceptionResolution.setTheAction((String)actionJComboBox.getSelectedItem());
//        if (!actionParamEditBoxList.isEmpty()) {
//            List newActionParams = new ArrayList<>();
//            ikasanExceptionResolution.setParams(newActionParams);
//            for (ComponentPropertyEditBox componentPropertyEditBox : actionParamEditBoxList) {
//                newActionParams.add(componentPropertyEditBox.updateValueObjectWithEnteredValues());
//            }
//        }
//
////        ikasanExceptionResolver.getIkasanExceptionResolutionList().put(getPropertyKey(), ikasanExceptionResolution);
//        return ikasanExceptionResolution;
//    }


//    /**
//     * Determine if the edit box has values in all mandatory fields.
//     * @return true if the editbox has a non-whitespace / real value.
//     */
//    public boolean editBoxHasValue() {
//        boolean hasValue = false;
//
//        String selectedException = (String)exceptionJComboBox.getSelectedItem();
//        String selectedAction = (String)actionJComboBox.getSelectedItem();
//        if (selectedException != null && ! selectedException.isEmpty() &&
//            selectedAction != null && ! selectedAction.isEmpty()) {
//            if (actionParamEditBoxList.isEmpty()) {
//                hasValue = true;
//            } else {
//
//                // Either all mandatory fields have value, or at least 1 non mandatory field has value
//                boolean nonMandatoryHasValue = false;
//                boolean hasNonMandatory = false;
//                boolean mandatoryHasValue = true;
//                for(ComponentPropertyEditBox editBox : actionParamEditBoxList) {
//                    if (!editBox.isMandatory()) {
//                        hasNonMandatory = true ;
//                    }
//                    if (editBox.isMandatory() && !editBox.editBoxHasValue()) {
//                        mandatoryHasValue = false;
//                        break;
//                    } else if (editBox.editBoxHasValue() && !editBox.isMandatory()) {
//                        nonMandatoryHasValue = true;
//                    }
//                }
//                if (mandatoryHasValue || (hasNonMandatory && nonMandatoryHasValue)) {
//                    hasValue = true;
//                }
//            }
//        }
//        return hasValue;
//    }

//    /**
//     * Determine if the data entered differs from the value object (ikasanExceptionResoluition)
//     * @return true if the property has been altered
//     */
//    public boolean propertyValueHasChanged() {
//        boolean hasChanged = false;
//
//        String selectedException = (String) exceptionJComboBox.getSelectedItem();
//        String selectedAction = (String) actionJComboBox.getSelectedItem();
//
//        if (editBoxHasValue()) {
//            if (!selectedException.equals(ikasanExceptionResolution.getTheException()) ||
//                    !selectedAction.equals(ikasanExceptionResolution.getTheAction())) {
//                hasChanged = true;
//            } else if (selectedAction.equals(ikasanExceptionResolution.getTheAction())) {
//                // Check to see if the values of any action params has changed
//                for (ComponentPropertyEditBox componentPropertyEditBox : actionParamEditBoxList) {
//                    if (componentPropertyEditBox.propertyValueHasChanged()) {
//                        hasChanged = true;
//                        break;
//                    }
//                }
//            }
//        } else if (ikasanExceptionResolution.getTheException() != null || ikasanExceptionResolution.getTheAction() != null){
//            hasChanged = true;
//        }
//        return hasChanged;
//    }

    /**
     * get the key for the exception resolution
     * @return the key for this exception resolution
     */
    public String getPropertyKey() {
        return (String)ikasanExceptionResolution.getTheException();
    }

    /**
     * actionParams will only have elements if an action has been chosen the requires params.
     * @return
     */
    public boolean actionHasParams() {
        return !actionParamEditBoxList.isEmpty();
    }
//    public String getAction() {
//        return (String)actionJComboBox.getSelectedItem();
//    }

    public List<ComponentPropertyEditBox> getActionParamsEditBoxList() {
        return actionParamEditBoxList;
    }

    /**
     * Validate the selected values
     * @return a non-empty ValidationInfo list if there are validation errors
     */
    protected List<ValidationInfo> doValidateAll() {
        return  Collections.emptyList();
    }

    public IkasanExceptionResolution getIkasanExceptionResolution() {
        return ikasanExceptionResolution;
    }

    public JLabel getExceptionField() {
        return exceptionField;
    }

    public JLabel getActionField() {
        return actionField;
    }

    public List<ComponentPropertyEditBox> getActionParamEditBoxList() {
        return actionParamEditBoxList;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }
}
