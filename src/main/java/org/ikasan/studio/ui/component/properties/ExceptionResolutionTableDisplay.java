package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
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
public class ExceptionResolutionTableDisplay {
    private final ExceptionResolverPanel resolverPanel;
    private final Project project;
    private final JLabel exceptionTitleField;
    private final JLabel actionTitleField;
    private final JLabel paramsTitleField;
    private final JButton addButton;
    private final List<ExceptionResolutionRowDisplay> exceptionResolutionRowDisplayList = new ArrayList<>();
    private final boolean componentInitialisation;
    private final ExceptionResolver exceptionResolver;
    private boolean hasChanged = false;

    public ExceptionResolutionTableDisplay(ExceptionResolverPanel resolverPanel, Project project, ExceptionResolver exceptionResolver, boolean componentInitialisation) {
        this.resolverPanel = resolverPanel;
        this.project = project;
        this.exceptionResolver = exceptionResolver;
        this.componentInitialisation = componentInitialisation;

        this.exceptionTitleField = new JLabel("Exception to be caught");
        this.actionTitleField = new JLabel("Action");
        this.paramsTitleField = new JLabel("Params");

        if (exceptionResolver.getIkasanExceptionResolutionMap() != null) {
            for (ExceptionResolution exceptionResolution : exceptionResolver.getIkasanExceptionResolutionMap().values()) {
                exceptionResolutionRowDisplayList.add(new ExceptionResolutionRowDisplay(this, exceptionResolution, componentInitialisation));
            }
        }
        addButton = new JButton("ADD");
        addButton.addActionListener(e ->
            doAdd()
        );
    }

    public void doDelete(ExceptionResolution exceptionResolution) {
        if (exceptionResolutionRowDisplayList.removeIf(item -> exceptionResolution.equals(item.getIkasanExceptionResolution()))) {
            hasChanged = true;
            resolverPanel.populatePropertiesEditorPanel();
            resolverPanel.redrawPanel();
        }
    }

    private void doAdd() {
        List<String> listOfExceptionsAlreadyCaught;
        if (exceptionResolutionRowDisplayList != null) {
            listOfExceptionsAlreadyCaught = exceptionResolutionRowDisplayList.stream().map(x->x.getExceptionResolution().getExceptionsCaught()).toList();
        } else {
            listOfExceptionsAlreadyCaught = new ArrayList<>();
        }
        ExceptionResolutionPanel exceptionResolutionPanel = new ExceptionResolutionPanel(listOfExceptionsAlreadyCaught, project, true);
        ExceptionResolution newResolution = null;
        UiContext uiContext = project.getService(UiContext.class);
        try {
            newResolution = new ExceptionResolution(uiContext.getIkasanModule().getMetaVersion());
        } catch (Exception e) {
            StudioUIUtils.displayIdeaWarnMessage(project, "There was a problem trying to get meta data (" + e.getMessage() + "), please review your logs");
        }
        if (newResolution != null) {
            exceptionResolutionPanel.updateTargetComponent(newResolution);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    project,
                    uiContext.getDesignerCanvas(),
                    exceptionResolutionPanel);
            if (propertiesPopupDialogue.showAndGet()) {
                StudioUIUtils.displayIdeaInfoMessage(project, "Code generation in progress, please wait.");
                exceptionResolutionRowDisplayList.add(new ExceptionResolutionRowDisplay(this, newResolution, componentInitialisation));
                hasChanged = true;
                resolverPanel.populatePropertiesEditorPanel();
                resolverPanel.redrawPanel();
            }
        }
    }

    /**
     * Usually the final step of edit, update the original value object with the entered data
     */
    public void  updateValueObjectWithEnteredValues() {
        exceptionResolver.resetIkasanExceptionResolutionList();
        for (ExceptionResolutionRowDisplay exceptionResolutionRowDisplay : exceptionResolutionRowDisplayList) {
            exceptionResolver.addExceptionResolution(exceptionResolutionRowDisplay.getIkasanExceptionResolution());
        }
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
        if (exceptionResolutionRowDisplayList.isEmpty()) {
            result.add(new ValidationInfo("At least one exception should be added. If the resolver is no longer required, delete it from the flow."));
        } else if (!hasChanged) {
            result.add(new ValidationInfo("No change has been made yet, change the configuration or cancel the action"));
        }
        return result;
    }
}
