package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import lombok.Data;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
@Data
public class ExceptionResolverEditBox {
    private final ExceptionResolverPanel resolverPanel;
    private final String projectKey;
    private final JLabel exceptionTitleField;
    private final JLabel actionTitleField;
    private final JLabel paramsTitleField;
    private final JButton addButton;
    private final List<org.ikasan.studio.ui.component.properties.ExceptionResolution> exceptionResolutionList = new ArrayList<>();
    private final boolean componentInitialisation;
    private final ExceptionResolver exceptionResolver;
    private boolean hasChanged = false;

    public ExceptionResolverEditBox(ExceptionResolverPanel resolverPanel, String projectKey, ExceptionResolver exceptionResolver, boolean componentInitialisation) {
        this.resolverPanel = resolverPanel;
        this.projectKey = projectKey;
        this.exceptionResolver = exceptionResolver;
        this.componentInitialisation = componentInitialisation;

        this.exceptionTitleField = new JLabel("Exception");
        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");

        if (exceptionResolver.getIkasanExceptionResolutionMap() != null) {
            for (ExceptionResolution exceptionResolution : exceptionResolver.getIkasanExceptionResolutionMap().values()) {
                exceptionResolutionList.add(new org.ikasan.studio.ui.component.properties.ExceptionResolution(this, exceptionResolution, componentInitialisation));
            }
        }
        addButton = new JButton("ADD");
        addButton.addActionListener(e ->
            doAdd()
        );
    }

    public void doDelete(ExceptionResolution exceptionResolution) {
        if (exceptionResolutionList.removeIf(item -> exceptionResolution.equals(item.getIkasanExceptionResolution()))) {
            hasChanged = true;
            resolverPanel.populatePropertiesEditorPanel();
            resolverPanel.redrawPanel();
        }
    }

    private void doAdd() {
        ExceptionResolutionPanel exceptionResolutionPanel = new ExceptionResolutionPanel(exceptionResolutionList, projectKey, true);
        ExceptionResolution newResolution = null;
        try {
            newResolution = new ExceptionResolution(UiContext.getIkasanModule(projectKey).getMetaVersion());
        } catch (Exception e) {
            StudioUIUtils.displayIdeaWarnMessage(projectKey, "There was a problem trying to get meta data (" + e.getMessage() + "), please review your logs");
        }
        if (newResolution != null) {
            exceptionResolutionPanel.updateTargetComponent(newResolution);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    projectKey,
                    UiContext.getDesignerCanvas(projectKey),
                    exceptionResolutionPanel);
            if (propertiesPopupDialogue.showAndGet()) {
                StudioUIUtils.displayIdeaInfoMessage(projectKey, "Code generation in progress, please wait.");
                exceptionResolutionList.add(new org.ikasan.studio.ui.component.properties.ExceptionResolution(this, newResolution, componentInitialisation));
                hasChanged = true;
                resolverPanel.populatePropertiesEditorPanel();
                resolverPanel.redrawPanel();
            }
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public ExceptionResolver updateValueObjectWithEnteredValues() {
        exceptionResolver.resetIkasanExceptionResolutionList();
        for (org.ikasan.studio.ui.component.properties.ExceptionResolution exceptionResolution : exceptionResolutionList) {
            exceptionResolver.addExceptionResolution(exceptionResolution.getIkasanExceptionResolution());
        }
        return exceptionResolver;
    }


    /**
     * Determine if the edit box has values in all mandatory fields.
     * @return true if the editbox has a non-whitespace / real value.
     */
    public boolean editBoxHasValue() {
        return !exceptionResolutionList.isEmpty();
    }

    /**
     * Determine if the data entered differs from the value object (ikasanExceptionResolution)
     * @return true if the property has been altered
     */
    public boolean propertyValueHasChanged() {
        return hasChanged;
    }


    /**
     * Validate the selected values
     * @return a non-empty ValidationInfo list if there are validation errors
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = new ArrayList<>();
        if (exceptionResolutionList.isEmpty()) {
            result.add(new ValidationInfo("At least one exception should be added. If the resolver is no longer required, delete it from the flow."));
        } else if (!hasChanged) {
            result.add(new ValidationInfo("No change has been made yet, change the configuration or cancel the action"));
        }
        return result;
    }
}
