package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.IkasanExceptionResolution;
import org.ikasan.studio.model.ikasan.instance.IkasanExceptionResolver;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
public class ExceptionResolverEditBox {
    private ExceptionResolverPanel resolverPanel;
    private String projectKey;
    private JLabel exceptionTitleField;
    private JLabel actionTitleField;
    private JLabel paramsTitleField;
    private JButton addButton;
    private List<ExceptionResolution> exceptionResolutionList = new ArrayList<>();
    private boolean componentInitialisation;
    private IkasanExceptionResolver ikasanExceptionResolver;
    private boolean hasChanged = false;

    public ExceptionResolverEditBox(ExceptionResolverPanel resolverPanel, String projectKey, IkasanExceptionResolver ikasanExceptionResolver, boolean componentInitialisation) {
        this.resolverPanel = resolverPanel;
        this.projectKey = projectKey;
        this.ikasanExceptionResolver = ikasanExceptionResolver ;
        this.componentInitialisation = componentInitialisation;

        this.exceptionTitleField = new JLabel("Exception");
        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");

        if (ikasanExceptionResolver.getIkasanExceptionResolutionMap() != null) {
            for (IkasanExceptionResolution exceptionResolution : ikasanExceptionResolver.getIkasanExceptionResolutionMap().values()) {
                exceptionResolutionList.add(new ExceptionResolution(this, exceptionResolution, componentInitialisation));
            }
        }
        addButton = new JButton("ADD");
        addButton.addActionListener(e ->
            doAdd()
        );
    }

    public void doDelete(IkasanExceptionResolution ikasanExceptionResolution) {
        if (exceptionResolutionList != null) {
            if (exceptionResolutionList.removeIf(item -> ikasanExceptionResolution.equals(item.getIkasanExceptionResolution()))) {
                hasChanged = true;
                resolverPanel.populatePropertiesEditorPanel();
                resolverPanel.redrawPanel();
            }
        }
    }

    private void doAdd() {
        ExceptionResolutionPanel exceptionResolutionPanel = new ExceptionResolutionPanel(exceptionResolutionList, projectKey, true);
        IkasanExceptionResolution newResolution = new IkasanExceptionResolution(ikasanExceptionResolver);
        exceptionResolutionPanel.updateTargetComponent(newResolution);
        PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                Context.getProject(projectKey),
                Context.getDesignerCanvas(projectKey),
                exceptionResolutionPanel);
        if (propertiesDialogue.showAndGet()) {
            exceptionResolutionList.add(new ExceptionResolution(this, newResolution, componentInitialisation));
            hasChanged = true;
            resolverPanel.populatePropertiesEditorPanel();
            resolverPanel.redrawPanel();
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public IkasanExceptionResolver updateValueObjectWithEnteredValues() {
        ikasanExceptionResolver.resetIkasanExceptionResolutionList();
        for (ExceptionResolution exceptionResolution : exceptionResolutionList) {
            ikasanExceptionResolver.addExceptionResolution(exceptionResolution.getIkasanExceptionResolution());
        }
        return ikasanExceptionResolver;
    }


    /**
     * Determine if the edit box has values in all mandatory fields.
     * @return true if the editbox has a non-whitespace / real value.
     */
    public boolean editBoxHasValue() {
        return exceptionResolutionList != null && !exceptionResolutionList.isEmpty();
    }

    /**
     * Determine if the data entered differs from the value object (ikasanExceptionResoluition)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
//        //@todo we could check ikasanExceptionResolver.getIkasanExceptionResolutionMap() and  exceptionResolutionList to see if effectively same.
        return hasChanged;
    }


    /**
     * Validate the selected values
     * @return a non-empty ValidationInfo list if there are validation errors
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = new ArrayList<>();
        if (exceptionResolutionList == null || exceptionResolutionList.isEmpty()) {
            result.add(new ValidationInfo("At least one exception should be added, or cancel so that exception resolver is not defined"));
        } else if (!hasChanged) {
            result.add(new ValidationInfo("No change has been made yet, change the configuration or cancel the action"));
        }
        return result;
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

    public List<ExceptionResolution> getExceptionResolutionList() {
        return exceptionResolutionList;
    }

    public void setHasChanged(boolean hasChanged) {
        this.hasChanged = hasChanged;
    }
}
