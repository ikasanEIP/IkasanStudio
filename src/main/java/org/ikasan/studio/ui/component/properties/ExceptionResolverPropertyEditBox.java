package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolution;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolver;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolverPropertyEditBox {
    private PropertiesPanel parent;
    private String projectKey;
    private JLabel exceptionTitleField;
    private JLabel actionTitleField;
    private JLabel paramsTitleField;
    private JButton addButton;
    private List<ExceptionResolutionPropertyDisplayBox> exceptionResolutionPropertyDisplayBoxList = new ArrayList<>();
    private boolean popupMode;
    private IkasanExceptionResolver ikasanExceptionResolver;
    private boolean dirty = false;

    public ExceptionResolverPropertyEditBox(PropertiesPanel parent, String projectKey, IkasanExceptionResolver ikasanExceptionResolver, boolean popupMode) {
        this.parent = parent;
        this.projectKey = projectKey;
        this.ikasanExceptionResolver = ikasanExceptionResolver ;
        this.popupMode = popupMode;

        this.exceptionTitleField = new JLabel("Exception");
        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");

        if (ikasanExceptionResolver.getIkasanExceptionResolutionMap() != null) {
            for (IkasanExceptionResolution exceptionResolution : ikasanExceptionResolver.getIkasanExceptionResolutionMap().values()) {
                exceptionResolutionPropertyDisplayBoxList.add(new ExceptionResolutionPropertyDisplayBox(exceptionResolution, popupMode));
            }
        }
        addButton = new JButton("+");
        addButton.addActionListener(e -> {
                    doAdd(e);
                }
        );
    }

    private void doAdd(ActionEvent ae) {
        ExceptionResolutionPropertiesPanel exceptionResolutionPropertiesPanel = new ExceptionResolutionPropertiesPanel(projectKey, true);
        IkasanExceptionResolution newResolution = new IkasanExceptionResolution(ikasanExceptionResolver);
        exceptionResolutionPropertiesPanel.updateTargetComponent(newResolution);
        PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                Context.getProject(projectKey),
                Context.getDesignerCanvas(projectKey),
                exceptionResolutionPropertiesPanel);
        if (! propertiesDialogue.showAndGet()) {
            // i.e. cancel.
            newResolution = null;
        } else {
            ikasanExceptionResolver.addExceptionResolution(newResolution);
            parent.populatePropertiesEditorPanel();
            parent.redrawPanel();
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public IkasanExceptionResolver updateValueObjectWithEnteredValues() {
        ikasanExceptionResolver.resetIkasanExceptionResolutionList();
        for (ExceptionResolutionPropertyDisplayBox exceptionResolutionPropertyDisplayBox : exceptionResolutionPropertyDisplayBoxList) {
            ikasanExceptionResolver.addExceptionResolution(exceptionResolutionPropertyDisplayBox.getIkasanExceptionResolution());
        }
        return ikasanExceptionResolver;
    }


    /**
     * Determine if the edit box has values in all mandatory fields.
     * @return true if the editbox has a non-whitespace / real value.
     */
    public boolean editBoxHasValue() {
        return exceptionResolutionPropertyDisplayBoxList != null && !exceptionResolutionPropertyDisplayBoxList.isEmpty();
    }

    /**
     * Determine if the data entered differs from the value object (ikasanExceptionResoluition)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
        return dirty;
    }

    /**
     * Validate the selected values
     * @return a non-empty ValidationInfo list if there are validation errors
     */
    protected List<ValidationInfo> doValidateAll() {
        if (exceptionResolutionPropertyDisplayBoxList == null || exceptionResolutionPropertyDisplayBoxList.isEmpty()) {
            List<ValidationInfo> result = new ArrayList<>();
            result.add(new ValidationInfo("At least one exception should be added, or cancel so that exception resolver is not defined"));
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JLabel getExceptionTitleField() {
        return exceptionTitleField;
    }

    public JLabel getActionTitleField() {
        return actionTitleField;
    }

    public JLabel getParamsTitleField() {
        return paramsTitleField;
    }

    public List<ExceptionResolutionPropertyDisplayBox> getExceptionResolutionPropertyDisplayBoxList() {
        return exceptionResolutionPropertyDisplayBoxList;
    }
}
